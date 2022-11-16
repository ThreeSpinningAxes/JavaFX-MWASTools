module com.example.mwastools {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires com.almasb.fxgl.all;
    requires jsch;
    requires com.google.gson;
    requires java.net.http;
    requires org.apache.commons.io;
    requires commons.configuration;

    opens com.example.mwastools to javafx.fxml;
    exports com.example.mwastools;
    exports com.example.mwastools.UtilityClasses;
    opens com.example.mwastools.UtilityClasses to javafx.fxml;
    exports com.example.mwastools.Controllers;
    opens com.example.mwastools.Controllers to javafx.fxml;
    exports com.example.mwastools.UtilityClasses.NetorkUtility;
    opens com.example.mwastools.UtilityClasses.NetorkUtility to javafx.fxml;
    exports com.example.mwastools.APIs;
    opens com.example.mwastools.APIs to javafx.fxml;
}