package com.software.codetime.websockets;

import com.google.gson.JsonObject;
import com.software.codetime.models.SessionSummary;
import com.software.codetime.models.User;

public interface WebsocketMessageHandler {
    void handleFlowScore(JsonObject obj);
    void handleFlowState(boolean enable_flow);
    void updateEditorStatus(SessionSummary data);
    void handleBillingPlanUpdateMessage(JsonObject obj);
    void handlePostAuthenticatedPluginUser(User user);
}
