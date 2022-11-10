package com.example.mwastools;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import com.example.mwastools.Controllers.MWASToolsMainWindowController;
import com.example.mwastools.UtilityClasses.NetorkUtility.SessionInstance;

public class MWASToolsApplication extends Application {

    /**
     * Creates the main window of the application and sets the main window controller to load the window
     * components and handle its events.
     */
    @Override
    public void start(Stage stage) throws IOException {
        SessionInstance sessionInstance = SessionInstance.getInstance();
        FXMLLoader fxmlLoader = new FXMLLoader(MWASToolsApplication.class.getResource("mwas-tools-main-window.fxml"));

        stage.setTitle("MWASTools");
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);

        MWASToolsMainWindowController mwasToolsMainWindowController = fxmlLoader.getController();
        stage.setOnCloseRequest(e -> mwasToolsMainWindowController.shutdownApplicationAndCloseSession());

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}