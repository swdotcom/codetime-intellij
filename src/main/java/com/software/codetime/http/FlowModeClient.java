package com.software.codetime.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FlowModeClient {
    public static boolean isFlowModeOn() {
        ClientResponse resp = OpsHttpClient.appGet("/plugin/flow_sessions");
        if (resp.isOk()) {
            JsonArray arr = resp.getJsonObj().get("flow_sessions").getAsJsonArray();
            return arr.size() > 0;
        } else {
            return false;
        }
    }

    public static boolean enterFlowMode(boolean automated) {
        JsonObject obj = new JsonObject();
        obj.addProperty("automated", automated);
        ClientResponse resp = OpsHttpClient.appPost("/plugin/flow_sessions", obj);
        return resp.isOk();
    }

    public static boolean exitFlowMode() {
        ClientResponse resp = OpsHttpClient.appDelete("/plugin/flow_sessions", null);
        return resp.isOk();
    }
}
