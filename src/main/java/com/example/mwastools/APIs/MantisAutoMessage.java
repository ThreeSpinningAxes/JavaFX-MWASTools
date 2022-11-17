package com.example.mwastools.APIs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;

import com.example.mwastools.Controllers.MWASToolsMainWindowController;
import com.example.mwastools.UtilityClasses.GeneralUtils;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

/**
 * This class describes how the mantis note describing config table updates is constructed and sent
 */
public class MantisAutoMessage {

    private final PropertiesConfiguration properties = null;

    private final String BASE_SOAP_API_URL_FOR_MANTIS = "";

    private final String MANTIS_ADD_NOTE_REQUEST_URL = "";

    private String MANTIS_BOT_PASS;

    private String MANTIS_BOT_USER;

    private ListView<String> logs;

    private String mantisUsernameOfWhoInitiatedConfigTableUpdate;

    private String mantisIssueNumberOfPageThatTracksConfigTableUpdates;

    private ListView<String> selectedConfigTables;

    private boolean pushToStability;

    public MantisAutoMessage(String mantisIssueNumber, String mantisUsername, boolean pushToStability,
        ListView<String> listView, ListView<String> logs,
        PropertiesConfiguration properties) throws ConfigurationException {
        this.mantisIssueNumberOfPageThatTracksConfigTableUpdates = mantisIssueNumber;
        this.mantisUsernameOfWhoInitiatedConfigTableUpdate = mantisUsername;
        this.pushToStability = pushToStability;
        this.selectedConfigTables = listView;
        this.logs = logs;
        MANTIS_BOT_PASS = properties.getProperty("mantisbotpassword").toString();
        MANTIS_BOT_USER = properties.getProperty("mantisbotusername").toString();
    }

    /**
     * Sends a POST request to the Mantis SOAP API to create a mantis note regarding the config table updates
     */
    public boolean sendPOSTAPIRequestToWriteAutomatedMantisNote() {
        try {
            String POSTRequestBody = buildPOSTRequestBody();
            if (POSTRequestBody == null) {
                return false;
            }
            HttpRequest POSTRequest = buildPostRequest(POSTRequestBody);
            sendPOSTRequest(POSTRequest);
            return true;
        }
        catch (URISyntaxException | IOException | InterruptedException e) {
            logs.getItems().add("Error automating mantis note submission: " + e.getMessage());
            logs.getItems().add("Mantis note may have not been sent");
            return false;
        }
    }

    /**
     *
     * Builds the Http POST request that will be sent to the Mantis API
     */
    private HttpRequest buildPostRequest(String POSTRequestBody) throws URISyntaxException {
        HttpRequest POSTRequest = HttpRequest.newBuilder()
            .uri(new URI(BASE_SOAP_API_URL_FOR_MANTIS))
            .header("Content-Type", "text/xml; charset=UTF-8")
            .header("SOAPAction", MANTIS_ADD_NOTE_REQUEST_URL)
            .POST(HttpRequest.BodyPublishers.ofString(POSTRequestBody))
            .build();
        return POSTRequest;
    }

    private void sendPOSTRequest(HttpRequest POSTRequest) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        client.send(POSTRequest, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Fetches the outline of the POST request body from a XML file
     */
    private String getMantisAPIRequestSendNoteFormat() {
        try {
            String mantisPostRequestBodyFormat = IOUtils.toString(MWASToolsMainWindowController.class.getClassLoader()
                .getResourceAsStream("mantis-post-request-add-note-format.xml"), StandardCharsets.UTF_8);
            if (mantisPostRequestBodyFormat == null) {
                throw new IOException();
            }
            return mantisPostRequestBodyFormat;
        }
        catch (IOException e) {
            GeneralUtils.showErrorPopup(e.getMessage());
            return "";
        }
    }


    /**
     * Creates the text content for the note being sent
     */
    private String buildMantisNote() {
        StringBuilder note = new StringBuilder();
        note.append("Config Table Update" + '\n');
        note.append("--------------------------" + '\n');
        note.append("User who initiated table update: " + mantisUsernameOfWhoInitiatedConfigTableUpdate + '\n');
        note.append("Requested push to stability: " + pushToStability + '\n');
        note.append("Updated tables: \n");
        ObservableList<String> tables = selectedConfigTables.getItems();

        for (int i = 0; i < tables.size(); i++) {
            note.append("-" + tables.get(i) + "\n");
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        note.append(
            "Time updated: " + cal.getTimeZone().getDisplayName() + ": " + dateFormat.format(cal.getTime()) + '\n');
        return note.toString();
    }

    /**
     * Fills in the POST request XML body that is going to be sent to the Mantis API with the correct text fields.
     */
    @FXML
    public String buildPOSTRequestBody() {
        StringBuilder POSTRequestBodyBuilder = new StringBuilder(getMantisAPIRequestSendNoteFormat());
        if (POSTRequestBodyBuilder.toString().isEmpty()) {
            return null;
        }
        ArrayList<String> inputInfo = getInputFieldsForPOSTRequestBody();

        // The post request format string has "?" that marks areas where you need to replace "?"
        // with its corresponding input value.
        for (String input : inputInfo) {
            int tempIndex = POSTRequestBodyBuilder.indexOf("?");
            POSTRequestBodyBuilder.replace(tempIndex, tempIndex + 1, input);
        }
        return POSTRequestBodyBuilder.toString();
    }

    /**
     * Gets a list of all the info needed to be added to the POST request XML
     */
    private ArrayList<String> getInputFieldsForPOSTRequestBody() {
        ArrayList<String> input = new ArrayList<>();
        input.add(MANTIS_BOT_USER);
        input.add(MANTIS_BOT_PASS);
        input.add(mantisIssueNumberOfPageThatTracksConfigTableUpdates);
        input.add(MANTIS_BOT_USER);
        input.add(buildMantisNote());
        return input;
    }
}
