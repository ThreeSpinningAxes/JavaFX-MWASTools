package com.example.mwastools.Controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.example.mwastools.UtilityClasses.GeneralUtils;
import com.example.mwastools.UtilityClasses.NetorkUtility.NetworkHelpers;
import com.example.mwastools.UtilityClasses.NetorkUtility.SessionInstance;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class LiveDropInTestNinjoDecoderLogsController {

    Session session = SessionInstance.getInstance().getSession();

    @FXML
    private TextArea componentLogs = new TextArea();

    @FXML
    private Button clearLiveDropInLogsButton;

    GetLiveLogs thread;

    public LiveDropInTestNinjoDecoderLogsController() {
    }

    public void initialize() {
        thread = new GetLiveLogs(componentLogs);
        thread.start();
    }

    public void clearLiveDropInLogs(ActionEvent e) {
        componentLogs.clear();
    }

    public void shutdownNinjoDecoderLiveLogWindowAndKillRemoteScript() {
        String command = "kill " + this.thread.ProcessIDForRemoteGetLogsScript;
        NetworkHelpers.executeCommandNoLogs(SessionInstance.getInstance().getSession(), command);
    }

    static class GetLiveLogs extends Thread {

        TextArea textArea;

        String ProcessIDForRemoteGetLogsScript;

        GetLiveLogs(TextArea textArea) {
            this.textArea = textArea;
        }

        public void run() {
            try {
                ChannelExec channelToSendCommandsToRemoteDocker = (ChannelExec) SessionInstance.getInstance()
                    .getSession()
                    .openChannel("exec");
                ByteArrayOutputStream scriptOutputStream = new ByteArrayOutputStream();

                setChannelToExecuteGetLiveLogsScriptOnDocker(channelToSendCommandsToRemoteDocker);
                setChannelOutputStreamToCaptureStdout(channelToSendCommandsToRemoteDocker, scriptOutputStream);
                executeGetLiveLogsScript(channelToSendCommandsToRemoteDocker, scriptOutputStream);
                printDropInLogsToLogWindow(channelToSendCommandsToRemoteDocker, scriptOutputStream);
                closeChannelAndItsByteStream(channelToSendCommandsToRemoteDocker);
            }
            catch (JSchException | IOException | InterruptedException e) {
                GeneralUtils.showErrorPopup(e.getMessage() + "\n will exit application");
                Platform.exit();
                System.exit(-1);
            }
        }

        private void setChannelToExecuteGetLiveLogsScriptOnDocker(ChannelExec channelToSendCommandsToRemoteDocker) {
            String executeGetLiveLogsScript =
                "cd /apps/dms/configTableScripts/scripts; sh ninjoDecoderwarp3Logs.sh";
            channelToSendCommandsToRemoteDocker.setCommand(executeGetLiveLogsScript);
        }

        private void setChannelOutputStreamToCaptureStdout(ChannelExec channelToSendCommandsToRemoteDocker,
            ByteArrayOutputStream scriptOutputStream) {
            channelToSendCommandsToRemoteDocker.setOutputStream(scriptOutputStream);
        }

        private void executeGetLiveLogsScript(ChannelExec channelToSendCommandsToRemoteDocker,
            ByteArrayOutputStream scriptOutputStream) throws JSchException, InterruptedException {
            channelToSendCommandsToRemoteDocker.connect();
            //wait 100 milliseconds to capture the processID of the remote script as the remote script itself
            //prints its own PID in stdout. Needed to kill script afterwards.
            Thread.sleep(100);
            this.ProcessIDForRemoteGetLogsScript = scriptOutputStream.toString(StandardCharsets.UTF_8);
        }

        private void printDropInLogsToLogWindow(ChannelExec channelToSendCommandsToRemoteDocker,
            ByteArrayOutputStream scriptOutputStream) throws IOException, InterruptedException {

            int previousBufferSize = scriptOutputStream.size();
            int currentBufferSize = previousBufferSize;

            //if exit status of remote script is -1, it is still executing
            while (channelToSendCommandsToRemoteDocker.getExitStatus() == -1) {
                currentBufferSize = scriptOutputStream.size();
                if (currentBufferSize > previousBufferSize) {
                    String newLogs = scriptOutputStream.toString(StandardCharsets.UTF_8)
                        .substring(previousBufferSize, currentBufferSize);
                    previousBufferSize = currentBufferSize;

                    textArea.appendText(newLogs);
                }
                Thread.sleep(1000);
            }
        }

        private void closeChannelAndItsByteStream(ChannelExec channelExec)
            throws IOException {
            channelExec.getOutputStream().close();
            channelExec.disconnect();
        }

    }

}





