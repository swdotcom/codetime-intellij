package com.software.codetime.models;

import com.google.gson.JsonObject;
import com.software.codetime.managers.AccountManager;
import com.software.codetime.managers.ConfigManager;

import javax.swing.*;

public class IntegrationConnection {
    public long id;
    public String name;
    public String email;
    public String status;
    public String auth_id;
    public String access_token;
    public IntegrationType integration_type;
    public long integration_type_id;
    public JsonObject meta = new JsonObject();

    public static void handleIntegrationConnectionEvent(IntegrationConnectionEvent integrationEvent) {
        AccountManager.getUser();

        SlackStateChangeModel changeModel = new SlackStateChangeModel();
        changeModel.dispatchSlackStateChangeCompletion();
        if (ConfigManager.tree_refresh_runnable != null) {
            SwingUtilities.invokeLater(() -> {
                ConfigManager.tree_refresh_runnable.run();
            });
        }
    }
}
