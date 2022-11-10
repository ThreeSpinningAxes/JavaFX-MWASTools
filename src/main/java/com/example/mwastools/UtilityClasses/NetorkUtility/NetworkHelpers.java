package com.example.mwastools.UtilityClasses.NetorkUtility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import javafx.scene.control.ListView;

/**
 * This class provides utility methods for network related commands like sftp or remote execution.
 */
public class NetworkHelpers {

    /**
     * Replaces the remote server files equivalent to the user's selected files. To find the matching files, search through
     * the list of directories on the remote server defined in the "directories" method parameter.
     */
    public static int SFTPTransferMultiDirMultiFile(Session session, ArrayList<File> files, List<String> directories,
        ListView<String> logs, int overwriteFlag)
        throws FileNotFoundException {

        if (files.size() == 0) {
            logs.getItems().add("No files were chosen for file transfer, please choose a file");
            return -1;
        }
        if (directories.size() == 0) {
            logs.getItems().add("directories for file transfer not configured");
            return -1;
        }

        ChannelSftp channelSftp = null;

        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        }
        catch (JSchException ex) {
            logs.getItems().add("Could not establish sftp connection. Abandoning file transfer");
            return -1;
        }

        try {
            channelSftp.cd(directories.get(0));
        }
        catch (SftpException ex1) {
            logs.getItems().add("Directory " + directories.get(0) + " does not exist on remote server.");
            channelSftp.disconnect();
            return -1;
        }
        ArrayList<Collection<String>> filesInEachDirectory = new ArrayList<>(Collections.emptyList());

        for (String directory : directories) {
            Collection<ChannelSftp.LsEntry> directoryFiles = null;
            try {
                directoryFiles = channelSftp.ls(directory);
            }
            catch (SftpException e) {
                logs.getItems().add("cannot execute commands on remote server. Abandoning file transfer");
                channelSftp.disconnect();
                return -1;
            }
            filesInEachDirectory.add(directoryFiles.stream()
                .map(ChannelSftp.LsEntry::getFilename)
                .collect(Collectors.toList()));
        }

        for (File file : files) {
            int directoryIndex = 0;
            boolean fileFound = false;
            for (Collection<String> directoryFiles : filesInEachDirectory) {
                if (directoryFiles.contains(file.getName())) {
                    try {
                        channelSftp.put(new FileInputStream(file), directories.get(directoryIndex) + file.getName(),
                            overwriteFlag);
                    }
                    catch (SftpException ex3) {
                        logs.getItems().add("cannot replace file " + file.getName() + ": " + ex3.getMessage());
                        logs.getItems().add("abandoning file transfer");
                        channelSftp.disconnect();
                        return -1;
                    }
                    logs.getItems().add("Updated " + file.getName());
                    fileFound = true;
                    break;
                }
                directoryIndex++;
            }
            if (!fileFound) {
                logs.getItems().add("Could not find " + file.getName() + "on the remote server");
                channelSftp.disconnect();
                return -1;
            }
        }
        channelSftp.disconnect();
        return 0;
    }

    /** 1 FILE -> 1 DIRECTORY*/
    public static int SFTPTransferSingleDirSingleFile(Session session, File file, String directory, ListView<String> logs)
        throws FileNotFoundException {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        }
        catch (JSchException ex) {
            logs.getItems().add("Could not establish sftp connection. Abandoning file transfer");
            channelSftp.disconnect();
            return -1;
        }
        try {
            channelSftp.put(new FileInputStream(file), directory + file.getName());
        }
        catch (SftpException ex2) {
            logs.getItems().add("cannot replace file " + file.getName() + ": " + ex2.getMessage());
            logs.getItems().add("abandoning file transfer");
            channelSftp.disconnect();
            return -1;
        }
        channelSftp.disconnect();
        return 0;
    }

    /** N FILES -> 1 DIRECTORY*/
    public static int SFTPTransferSingleDirMultiFile(Session session, ArrayList<File> files, String directory, ListView<String> logs)
        throws FileNotFoundException {
        ChannelSftp channelSftp = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
        }
        catch (JSchException ex) {
            logs.getItems().add("Could not establish sftp connection. Abandoning file transfer");
            channelSftp.disconnect();
            return -1;
        }

        try {
            for (File file : files) {
                channelSftp.put(new FileInputStream(file), directory + file.getName());
            }
        }
        catch (SftpException ex2) {
            logs.getItems().add("cannot place file: " + ex2.getMessage());
            logs.getItems().add("abandoning file transfer");
            channelSftp.disconnect();
            return -1;
        }
        channelSftp.disconnect();
        return 0;
    }

    public static ReturnPayload executeCommand(Session session, String command, ListView<String> logs,
        boolean requiresOutput) {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            ByteArrayOutputStream baos = null;
            ByteArrayOutputStream baosERR = null;

            if (requiresOutput) {
                baos = new ByteArrayOutputStream();
                baosERR = new ByteArrayOutputStream();

                channel.setOutputStream(baos);
                channel.setErrStream(baosERR);
            }
            channel.setCommand(command);
            channel.connect();
            while (channel.getExitStatus() == -1) {
                Thread.sleep(100);
            }
            ReturnPayload payload = new ReturnPayload();

            if (requiresOutput) {
                baos.close();
                baosERR.close();
                if (baosERR.size() != 0)
                    payload.stderr = baosERR.toString(StandardCharsets.UTF_8);
                if (baos.size() != 0)
                    payload.stdout = baos.toString(StandardCharsets.UTF_8);
            }

            payload.exitCode = channel.getExitStatus();
            channel.disconnect();
            payload.status_code = 0;

            return payload;
        }
        catch (JSchException | IOException | InterruptedException e) {
            logs.getItems().add(e.getMessage());
            ReturnPayload p = new ReturnPayload();
            p.status_code = -1;
            return p;
        }

    }

    public static ReturnPayload executeCommandNoLogs(Session session, String command) {
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteArrayOutputStream baosERR = new ByteArrayOutputStream();

            channel.setOutputStream(baos);
            channel.setErrStream(baosERR);

            channel.setCommand(command);
            channel.connect();

            while (channel.getExitStatus() == -1) {
                Thread.sleep(100);
            }
            baos.close();
            baosERR.close();

            ReturnPayload payload = new ReturnPayload();
            payload.exitCode = channel.getExitStatus();
            channel.disconnect();

            return payload;
        }
        catch (JSchException | IOException e) {
            ReturnPayload p = new ReturnPayload();
            p.status_code = -1;
            return p;
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
