package com.example.mwastools.Controllers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.example.mwastools.APIHandlers.MantisAutoMessage;
import com.example.mwastools.MWASToolsApplication;
import com.example.mwastools.UtilityClasses.GeneralUtils;
import com.example.mwastools.UtilityClasses.NetorkUtility.ReturnPayload;
import com.example.mwastools.UtilityClasses.NetorkUtility.SessionInstance;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import static com.example.mwastools.UtilityClasses.NetorkUtility.NetworkHelpers.SFTPTransferMultiDirMultiFile;
import static com.example.mwastools.UtilityClasses.NetorkUtility.NetworkHelpers.SFTPTransferSingleDirMultiFile;
import static com.example.mwastools.UtilityClasses.NetorkUtility.NetworkHelpers.SFTPTransferSingleDirSingleFile;
import static com.example.mwastools.UtilityClasses.NetorkUtility.NetworkHelpers.executeCommand;
import static com.example.mwastools.UtilityClasses.NetorkUtility.SessionInstance.isSessionAlive;

/**
 * This class handles all the events that occur on the main window (including events on all different tabs)
 */

public class MWASToolsMainWindowController {

    /** ----------------------------------- CONFIGURATION ----------------------------------------------------------*/
    private final PropertiesConfiguration properties = new PropertiesConfiguration("application.properties");

    /** ------------------------------------ DIRECTORIES ------------------------------------------------------- */
    private final String USER_PROFILE = System.getenv("USERPROFILE");

    /** List of directories where the config table files are on the docker */
    private final List<String> CONFIG_FILES_ON_DOCKER_DIRECTORIES = Arrays.stream(
            properties.getStringArray("dockerdirectories"))
        .toList();

    /** Directory where you drop in a DAX on the docker for drop testing */
    private final String TEST_DROP_IN_DIR = properties.getString("testdropindir");

    /** Directory of where to drop the updated config files on the docker to update the git repo */
    private final String DROP_DIR_FOR_GIT = properties.getString("dropdirforgit");

    private final String DOWNLOAD_DIR = USER_PROFILE + "\\Downloads\\";

    /** ----------------------------------- BACKEND VARIABLES -------------------------------------------------- */
    private String mantisIssueNumber;

    private String mantisUsername;

    private ArrayList<File> listOfConfigFiles = new ArrayList<>();

    private File dropInTestFile;

    /** Controller object that handles the events on the live Ninjo-decoder logs window for the drop in testing */
    public LiveDropInTestNinjoDecoderLogsController ninjoLogsTabController;

    /** ----------------------JAVAFX CONFIG TABLE UPDATE TAB INTERFACE COMPONENTS -------------------------------*/
    @FXML
    private TextField mantisIssueNumberField = new TextField();

    @FXML
    private TextField mantisUserNameField = new TextField();

    @FXML
    private ListView<String> selectedConfigTableFiles = new ListView<>();

    @FXML
    private ListView<String> logsOfConfigTableUpdateTab = new ListView<>();

    @FXML
    private Button openConfigTableButton;

    @FXML
    private Button clearConfigTableFilesButton;

    @FXML
    private Button updateConfigTablesButton;

    /** JAVAFX DROP IN TEST TAB COMPONENTS */

    @FXML
    private Button openDropIn;

    @FXML
    private Button clearDropIn;

    @FXML
    private Button dropDropIn;

    @FXML
    private Text selectedFileDropIn;

    @FXML
    private ListView<String> logsDropIn = new ListView<>();

    @FXML
    private Button seeNinjoDecoderWarp3Logs;

    @FXML
    private CheckBox pushToStabilityCheck;

    @FXML
    private Button clearTableUpdaterLogsButton;

    @FXML
    private Button clearDropInLogsButton;

    private Stage logWindow;

    /** ------------------------------- JAVAFX SSH SETTINGS TAB COMPONENTS ---------------------------------------*/

    @FXML
    private TextField remoteHostTextField;

    @FXML
    private TextField portNumberTextField;

    @FXML
    private TextField DMSUsernameTextField;

    @FXML
    private PasswordField DMSPasswordTextField;

    @FXML
    private Button establishConnectionButton;

    @FXML
    private ListView<String> SSHLogs = new ListView<>();

    public MWASToolsMainWindowController() throws ConfigurationException {}

    /**
     * Called when application starts up. Sets the mantis username and mantis issue number from cache.
     */
    public void initialize() {

        mantisUserNameField.setText(properties.getProperty("username").toString());
        mantisUsername = mantisUserNameField.getText();
        mantisIssueNumberField.setText(properties.getProperty("issuenumber").toString());
        mantisIssueNumber = mantisIssueNumberField.getText();
    }

    /** ------------------------------CONFIG FILE UPDATE TAB HANDLERS ----------------------------------------------------*/

    /**
     * After the user presses "update", the config tables on the docker are replaced with the user's selected files.
     * If the "push to stability" button is checked, an automated merge request is sent to implement the updates to
     * stability AND an automated message is sent to the mantis page. THe git push occurs via running a script on the
     * docker
     */
    public void sendConfigFiles(ActionEvent e) throws FileNotFoundException {

        Session session = SessionInstance.getInstance().getSession();

        if (!isSessionAlive(session)) {
            logsOfConfigTableUpdateTab.getItems().add("You are not connected to remote session. Abandoning");
            return;
        }

        if (areComponentsRestarting(session)) {
            logsOfConfigTableUpdateTab.getItems()
                .add("Docker components are currently restarting, please wait 5 minutes for them to finish.");
            return;
        }

        // perform sftp of config table files to docker. Fail is status code returns -1
        if (performSFTPOfConfigFilesToDocker(session) == -1) {
            return;
        }

        logsOfConfigTableUpdateTab.getItems().add("Updated docker tables.");
        restartComponents();

        if (pushToStabilityCheck.isSelected()) {

            autoSaveMantisUsername();
            autoSaveMantisIssueNumber();

            if (mantisIssueNumber.length() == 0 | mantisUsername.length() == 0) {
                logsOfConfigTableUpdateTab.getItems()
                    .add("Please make sure both the issue number and username text fields are not empty");
                return;
            }

            logsOfConfigTableUpdateTab.getItems().add("Requesting to push changes to stability");
            if (performSFTPofConfigFilesToGitRepoOnDocker(session) == -1) {
                return;
            }

            executeGITPushOnDockerOfConfigTableChangesAndAutomateMantisNote(session);
            logsOfConfigTableUpdateTab.getItems().add("Done");
        }
    }

    /**
     * Restarts the MWAS components on the docker by running a remote script
     */
    private void restartComponents() {
        logsOfConfigTableUpdateTab.getItems().add("Attempting to restart components on docker...");
        String command = "(cd /apps/dms/configTableScripts/scripts/; sh restart_components.sh > /dev/null 2>&1 &)";
        ReturnPayload p = executeCommand(SessionInstance.getInstance().getSession(), command,
            logsOfConfigTableUpdateTab, false);
        logsOfConfigTableUpdateTab.getItems().add("Restarting components on docker (takes 5-8 minutes to complete)");
    }

    private boolean areComponentsRestarting(Session session) {
        ReturnPayload checkIfComponentAreRestarting = executeCommand(session, "pgrep -f -x 'sh restart_components.sh'",
            logsOfConfigTableUpdateTab, true);
        if (checkIfComponentAreRestarting.stdout.length() != 0) {
            return true;
        }
        return false;
    }

    private int performSFTPOfConfigFilesToDocker(Session session) throws FileNotFoundException {
        return SFTPTransferMultiDirMultiFile(session, listOfConfigFiles, CONFIG_FILES_ON_DOCKER_DIRECTORIES,
            logsOfConfigTableUpdateTab,
            ChannelSftp.OVERWRITE);
    }

    private int performSFTPofConfigFilesToGitRepoOnDocker(Session session) throws FileNotFoundException {
        return SFTPTransferSingleDirMultiFile(session, listOfConfigFiles, DROP_DIR_FOR_GIT, logsOfConfigTableUpdateTab);
    }

    private void executeGITPushOnDockerOfConfigTableChangesAndAutomateMantisNote(Session session) {
        String command = "(cd /apps/dms/configTableScripts/scripts/; sh automateConfigTableUpdatePush.sh "
            + mantisIssueNumber + " 2>&1 &)";
        logsOfConfigTableUpdateTab.getItems().add("---------PUSH TO STABILITY LOGS----------");
        ReturnPayload automateGitPushScriptOutput = executeCommand(session, command, logsOfConfigTableUpdateTab, true);
        logsOfConfigTableUpdateTab.getItems().add(automateGitPushScriptOutput.stdout);
        if (automateGitPushScriptOutput.exitCode != 0) {
            logsOfConfigTableUpdateTab.getItems().add("ERROR: Pushing to stability failed. Check logs for info.");
            return;
        }
        sendAutomatedMessageOfConfigUpdateToMantisPage(true, logsOfConfigTableUpdateTab);
    }


    /**
     * Sends a mantis note that describes the tables updates and who initiated the update. Uses the number in the
     * Mantis issue number text box to get the correct mantis page.
     *
     * Uses the Mantis SOAP API protocol to send message.
     */
    private void sendAutomatedMessageOfConfigUpdateToMantisPage(boolean pushToStability, ListView<String> logs) {
        MantisAutoMessage mantisBot = null;
        try {
            mantisBot = new MantisAutoMessage(mantisIssueNumber, mantisUsername, pushToStability,
                selectedConfigTableFiles, logs,
                properties);
            logs.getItems().add("Send table update message to mantis");
            mantisBot.sendPOSTAPIRequestToWriteAutomatedMantisNote();
        }
        catch (ConfigurationException e) {
            GeneralUtils.showErrorPopup(e.getMessage());
            SessionInstance.getInstance().shutdown();
        }
    }

    /**
     * Called when user clicks "Open" button
     * Opens file explorer for user to select the config files they want to update
     */
    @FXML
    public void selectConfigFiles(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(DOWNLOAD_DIR));
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Excel Sheet", "*.xlsx"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        List<File> selectedConfigF = fc.showOpenMultipleDialog(null);

        if (selectedConfigF != null) {
            for (File file : selectedConfigF) {
                listOfConfigFiles.add(file);
                selectedConfigTableFiles.getItems().add(file.getName());
            }
        }
    }

    /**
     * Allows users to drag and drop files directly into the listview representing the list of config files
     */
    @FXML
    public void handleFileDroppedEvent(DragEvent event) {
        Dragboard db = event.getDragboard();
        List<File> files = db.getFiles();
        listOfConfigFiles.addAll(files);
        selectedConfigTableFiles.getItems().addAll(files.stream()
            .map(File::getName)
            .toList());
    }

    /**
     * Clears config files selected
     */
    @FXML
    public void clearConfigFiles(ActionEvent e) {
        listOfConfigFiles = new ArrayList<>();
        selectedConfigTableFiles.getItems().clear();
    }

    /**
     * Clears the config table updater log table
     */
    @FXML
    public void clearTableUpdaterLogs(ActionEvent e) {
        logsOfConfigTableUpdateTab.getItems().clear();
    }

    /**
     * Saves whatever the user types in the mantis issue text box into the properties file after pressing enter
     */
    @FXML
    public void saveMantisIssueNumberFromUserInput(ActionEvent e) throws ConfigurationException {
        mantisIssueNumber = mantisIssueNumberField.getText();
        properties.setProperty("issuenumber", mantisIssueNumber);
        properties.save();
    }

    /**
     * Saves whatever the user types in the mantis username text box into the properties file after pressing enter
     */
    @FXML
    public void saveMantisUsernameFromUserInput(ActionEvent e) throws ConfigurationException {
        mantisUsername = mantisUserNameField.getText();
        properties.setProperty("username", mantisUsername);
        properties.save();
    }

    /**
     * Saves whatever the user types in the mantis issue text box into the properties file after pressing update
     * Needed if user does not press enter in text box (entering saves the text)
     */
    public void autoSaveMantisIssueNumber() {
        mantisIssueNumber = mantisIssueNumberField.getText();
        try {
            properties.setProperty("issuenumber", mantisIssueNumber);
            properties.save();
        }
        catch (ConfigurationException e) {
            logsOfConfigTableUpdateTab.getItems().add("Exception occurred: " + e.getMessage());
        }
    }

    /**
     * Saves whatever the user types in the mantis username text box into the properties file after pressing update
     * button
     * Needed if user does not press enter in text box
     */
    public void autoSaveMantisUsername() {
        mantisUsername = mantisUserNameField.getText();
        try {
            properties.setProperty("username", mantisUsername);
            properties.save();
        }
        catch (ConfigurationException e) {
            logsOfConfigTableUpdateTab.getItems().add("Exception occurred: " + e.getMessage());
        }
    }

    /* ------------------------------------DROP IN TESTING TAB HANDLERS ---------------------------------------*/

    /**
     * Executes the drop test by performing sftp over to the docker and running a remote script. Also automatically
     * opens a window that shows the live logs of the drop in test.
     */
    @FXML
    public void execDropTest(ActionEvent e) throws FileNotFoundException {
        Session session = SessionInstance.getInstance().getSession();

        if (!isSessionAlive(session)) {
            logsDropIn.getItems().add("You are not connected to remote session. Abandoning");
            return;
        }

        if (dropInTestFile == null) {
            logsDropIn.getItems().add("No file selected. Please select file");
            return;
        }

        if (performSTFPOfDropTestFileToMWASAutoIssuer(session) == -1) {
            return;
        }

        logsDropIn.getItems().add("Executing drop test...");

        ReturnPayload outputOfSingleRunBashScript = executeSingleRunBashScriptToPerformDropTest(session);
        seeNinjoDecoderWarp3Logs.fire();

        if (outputOfSingleRunBashScript.stderr.contains("ERROR something went wrong")) {
            addSingleRunScriptErrorLogs(outputOfSingleRunBashScript);
            return;
        }

        if (outputOfSingleRunBashScript.status_code == 0) {
            if (outputOfSingleRunBashScript.exitCode == 0) {
                logsDropIn.getItems().add("Successfully executed drop test.");
            }
            else {
                logsDropIn.getItems()
                    .add("Remote script exited with unusual exit code: " + outputOfSingleRunBashScript.exitCode);
            }
        }
        else {
            logsDropIn.getItems().add("Exception occurred during drop test");
        }
    }

    private int performSTFPOfDropTestFileToMWASAutoIssuer(Session session) throws FileNotFoundException {
        return SFTPTransferSingleDirSingleFile(session, dropInTestFile, TEST_DROP_IN_DIR, logsDropIn);
    }

    private ReturnPayload executeSingleRunBashScriptToPerformDropTest(Session session) {
        String performDropTestCommand = "(cd /apps/dms/mwas-autoissuer/; sh single-run.sh)";
        return executeCommand(session, performDropTestCommand, logsDropIn, true);
    }

    private void addSingleRunScriptErrorLogs(ReturnPayload outputOfSingleRunBashScript) {
        logsDropIn.getItems().add("An error has occurred executing java code on your file");
        logsDropIn.getItems().add("Please make sure file is of correct type and/or format");
        logsDropIn.getItems().add("-----------EXECUTION LOGS------------");
        logsDropIn.getItems().add(outputOfSingleRunBashScript.stderr);
    }

    /**
     * Opens a window that shows the live logs of the drop test.
     */
    @FXML
    public void getNinjoDecoderWarp3Logs(ActionEvent e) throws IOException {
        if (SessionInstance.getInstance().getSession() == null) {
            logsDropIn.getItems().add("You are not connected to remote session.");
            return;
        }
        buildNinjoDecoderDropInTestLogWindow();
    }

    private void buildNinjoDecoderDropInTestLogWindow() throws IOException {
        if (logWindow == null || !logWindow.isShowing()) {

            FXMLLoader loader = new FXMLLoader(MWASToolsApplication.class.getResource("ninjo-live-logs-controller.fxml"));
            Parent root = loader.load();
            LiveDropInTestNinjoDecoderLogsController controller = loader.getController();
            this.ninjoLogsTabController = controller;
            Scene scene = new Scene(root);
            logWindow = new Stage();
            logWindow.setTitle("dms-decoder-ninjo-warp3 logs LIVE");
            logWindow.setScene(scene);

            double windowGap = 5;
            Window mainWindow = openConfigTableButton.getScene().getWindow();

            logWindow.setX(mainWindow.getX() + mainWindow.getWidth() + windowGap);
            logWindow.setY(mainWindow.getY());

            logWindow.show();
            logWindow.setOnCloseRequest(e -> controller.shutdownNinjoDecoderLiveLogWindowAndKillRemoteScript());
        }
    }

    /**
     * Opens file explorer for user to select drop in file for drop in testing after pressing "Open" button in the
     * drop in test tab
     */
    @FXML
    public void selectFileDropIn(ActionEvent e) {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(DOWNLOAD_DIR));

        File file = fc.showOpenDialog(null);

        if (file != null) {
            dropInTestFile = file;
            selectedFileDropIn.setText(file.getName());
        }
    }

    /**
     * Clears drop in file for drop in testing
     */
    @FXML
    public void clearDropInFile(ActionEvent e) {
        dropInTestFile = null;
        selectedFileDropIn.setText("");
    }

    /**
     * Clears logs in drop in tab
     */
    @FXML
    public void clearDropInLogs(ActionEvent e) {
        logsDropIn.getItems().clear();
    }


    /* --------------------------------------- SSH CONNECTION TAB HANDLER --------------------------------------- */
    @FXML
    public void establishSSHConnection(ActionEvent e) {

        String host = remoteHostTextField.getText();
        String port = portNumberTextField.getText();
        String user = DMSUsernameTextField.getText();
        String pass = DMSPasswordTextField.getText();

        if (host.length() == 0 || port.length() == 0 || user.length() == 0 || pass.length() == 0) {
            SSHLogs.getItems().add("Please make sure all text fields are not empty");
            return;
        }

        int portnum;

        try {
            portnum = Integer.parseInt(port);
        }
        catch (NumberFormatException ex) {
            SSHLogs.getItems().add("Port value must be a integer");
            return;
        }

        try {
            SessionInstance.getInstance().disconnectSessionIfAlive();
            SessionInstance.getInstance().setupJschSession(user, host, pass, portnum);
        }

        catch (JSchException ex2) {
            SSHLogs.getItems().add("Could not establish connection. Please make sure fields are inputted correctly");
            return;
        }
        saveProperty(properties, "remoteip", host);
        saveProperty(properties, "remoteport", port);
        saveProperty(properties, "remoteuser", user);
        saveProperty(properties, "remotepass", pass);
        SSHLogs.getItems().add("Connection is successful!");

    }

    private void saveProperty(PropertiesConfiguration properties, String propertyName, String propertyValue) {
        try {
            properties.setProperty(propertyName, propertyValue);
            properties.save();
        }
        catch (ConfigurationException e) {
            GeneralUtils.showErrorPopup(e.getMessage());
            Platform.exit();
            System.exit(-1);
        }
    }

    public void shutdownApplicationAndCloseSession() {
        shutdownLogsTab();
        SessionInstance.getInstance().shutdown();
    }

    private void shutdownLogsTab() {
        if (ninjoLogsTabController != null) {
            this.ninjoLogsTabController.shutdownNinjoDecoderLiveLogWindowAndKillRemoteScript();
        }
    }

}