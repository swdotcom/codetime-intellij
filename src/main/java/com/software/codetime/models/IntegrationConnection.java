package com.software.codetime.models;

import com.software.codetime.managers.AccountManager;
import com.software.codetime.managers.ConfigManager;

import javax.swing.*;

public class IntegrationConnection {
    public long id;
    public String status;
    public long integration_type_id;

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
