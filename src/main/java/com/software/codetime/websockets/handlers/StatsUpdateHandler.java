package com.software.codetime.websockets.handlers;

import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.SessionSummary;

import javax.swing.*;

public class StatsUpdateHandler {
    public static void handleStatsUpdate(SessionSummary summaryData) {
        if (summaryData != null && ConfigManager.ws_msg_handler != null) {
            SwingUtilities.invokeLater(() -> {
                ConfigManager.ws_msg_handler.updateEditorStatus(summaryData);
            });
        }
    }
}
