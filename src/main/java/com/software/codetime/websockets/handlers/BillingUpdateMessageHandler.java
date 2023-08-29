package com.software.codetime.websockets.handlers;

import com.google.gson.JsonObject;
import com.software.codetime.managers.ConfigManager;

import javax.swing.*;

public class BillingUpdateMessageHandler {
    public static void handleBillingPlanUpdateMessage(JsonObject obj) {
        if (ConfigManager.ws_msg_handler != null) {
            SwingUtilities.invokeLater(() -> {
                ConfigManager.ws_msg_handler.handleBillingPlanUpdateMessage(obj);
            });
        }
    }
}
