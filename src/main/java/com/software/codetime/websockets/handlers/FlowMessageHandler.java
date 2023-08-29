package com.software.codetime.websockets.handlers;

import com.google.gson.JsonObject;
import com.software.codetime.managers.ConfigManager;

import javax.swing.*;

public class FlowMessageHandler {
    public static void handleFlowScoreMessage(JsonObject obj) {
        if (ConfigManager.ws_msg_handler != null) {
            SwingUtilities.invokeLater(() -> {
                ConfigManager.ws_msg_handler.handleFlowScore(obj);
            });
        }

    }

    public static void handleFlowStateMessage(boolean enable_flow) {
        if (ConfigManager.ws_msg_handler != null) {
            SwingUtilities.invokeLater(() -> {
                ConfigManager.ws_msg_handler.handleFlowState(enable_flow);
            });
        }

    }
}
