package com.software.codetime.managers;

import com.google.gson.JsonObject;
import swdc.java.ops.manager.SessionSummaryManager;
import swdc.java.ops.model.SessionSummary;
import swdc.java.ops.model.User;
import swdc.java.ops.websockets.WebsocketMessageHandler;

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
