package com.example.mwastools.UtilityClasses;

import javafx.scene.control.Alert;

public class GeneralUtils {

    /**
     * error window showing exception message
     */
    public static void showErrorPopup(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error has occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }


}
