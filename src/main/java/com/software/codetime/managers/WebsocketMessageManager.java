package com.software.codetime.managers;

import com.google.gson.JsonObject;
import com.software.codetime.models.FlowManager;
import com.software.codetime.models.SessionSummary;
import com.software.codetime.models.SessionSummaryManager;
import com.software.codetime.models.User;
import com.software.codetime.websockets.WebsocketMessageHandler;

public class WebsocketMessageManager implements WebsocketMessageHandler {
    @Override
    public void handleFlowScore(JsonObject jsonObject) {
        FlowManager.enterFlowMode(true);
    }

    @Override
    public void handleFlowState(boolean enable_flow) {
        if (!enable_flow) {
            FlowManager.exitFlowMode();
        }
    }

    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        SessionSummaryManager.updateFileSummaryAndStatsBar(sessionSummary);
    }

    @Override
    public void handleBillingPlanUpdateMessage(JsonObject jsonObject) {}

    @Override
    public void handlePostAuthenticatedPluginUser(User user) {}
}
