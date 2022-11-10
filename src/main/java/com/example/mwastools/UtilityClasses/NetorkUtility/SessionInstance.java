package com.example.mwastools.UtilityClasses.NetorkUtility;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.example.mwastools.UtilityClasses.GeneralUtils;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import javafx.application.Platform;

public class SessionInstance {

    private PropertiesConfiguration properties = new PropertiesConfiguration("application.properties");

    Session session;

    private String REMOTE_HOST = properties.getString("remoteip");

    private String USERNAME = properties.getString("remoteuser");

    private int PORT = properties.getInt("remoteport");

    private String PASSWORD = properties.getString("remotepass");

    private static SessionInstance sessionInstance;

    private SessionInstance() throws ConfigurationException {
        try {
            this.properties = new PropertiesConfiguration("application.properties");
            this.session = setupJschSession();
        }
        catch (ConfigurationException e) {
            GeneralUtils.showErrorPopup(e.getMessage());
            Platform.exit();
        }
    }

    public synchronized static SessionInstance getInstance() {
        try {
            if (sessionInstance == null) {
                sessionInstance = new SessionInstance();
            }
            return sessionInstance;
        }
        catch (Exception e) {
            GeneralUtils.showErrorPopup(
                "Cannot connect to remote server: " + e.getMessage() + "\n" + "Please make sure internet" +
                    "connection is working or you are connected to the correct remote server");
            return null;
        }

    }

    public Session setupJschSession() {
        try {
            JSch jsch = new JSch();

            Session jschSession = jsch.getSession(USERNAME, REMOTE_HOST, PORT);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            jschSession.setConfig(config);
            jschSession.setPassword(PASSWORD);
            jschSession.connect();
            return jschSession;
        }
        catch (JSchException e) {
            GeneralUtils.showErrorPopup(
                "Cannot connect to remote server: " + e.getMessage() + "\n" + "Please make sure internet " +
                    "connection is working and/or you are connected to the correct remote server. You can connect to " +
                    "the correct remote server via the SSH tab.");
            return null;
        }
    }

    public Session setupJschSession(String username, String remote_host_ip, String host_password, int port)
        throws JSchException {
        JSch jsch = new JSch();

        Session jschSession = jsch.getSession(username, remote_host_ip, port);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        jschSession.setConfig(config);
        jschSession.setPassword(host_password);
        jschSession.connect();
        return jschSession;
    }

    public Session getSession() {
        return session;
    }

    public void disconnectSessionIfAlive() {
        if (session != null) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public static boolean isSessionAlive(Session session) {
        return session != null && session.isConnected();
    }

    public void shutdown() {
        disconnectSessionIfAlive();
        Platform.exit();
    }

}
