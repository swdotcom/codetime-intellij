package com.software.codetime.websockets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.*;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.CurrentDayStatsEvent;
import com.software.codetime.models.IntegrationConnection;
import com.software.codetime.models.IntegrationConnectionEvent;
import com.software.codetime.models.User;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import com.software.codetime.websockets.handlers.AuthenticatedPluginUser;
import com.software.codetime.websockets.handlers.BillingUpdateMessageHandler;
import com.software.codetime.websockets.handlers.FlowMessageHandler;
import com.software.codetime.websockets.handlers.StatsUpdateHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsocketClient {
    public static final Logger log = Logger.getLogger("WebsocketClient");

    private static WebSocket ws = null;
    private static boolean receivedPing = false;
    private static boolean initializedPingSchedule = false;
    private static final int ONE_MIN_SECONDS = 60;
    private static final int DEFAULT_PING_INTERVAL_SECONDS = ONE_MIN_SECONDS * 30;
    private static int SERVER_PING_INTERVAL_SECONDS = DEFAULT_PING_INTERVAL_SECONDS + ONE_MIN_SECONDS;

    private static final Object connecting = new Object();

    public static void reConnect() {
        if (ws != null) {
            try {
                ws.disconnect();
            } catch (Exception e) {
                log.log(Level.WARNING, "websocket disconnect error: " + e.getMessage());
            }
        }

        receivedPing = false;
        connect();
    }

    private static void heartbeat(byte[] bytes) {
        receivedPing = true;
        if (bytes != null) {
            try {
                JsonObject obj = UtilManager.gson.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
                // timeout is in millis. convert it to seconds
                int new_timeout = (obj.get("timeout").getAsInt() / 1000);
                if (new_timeout > DEFAULT_PING_INTERVAL_SECONDS) {
                    // update the server ping interval
                    SERVER_PING_INTERVAL_SECONDS = new_timeout + ONE_MIN_SECONDS;
                } else {
                    SERVER_PING_INTERVAL_SECONDS = DEFAULT_PING_INTERVAL_SECONDS + ONE_MIN_SECONDS;
                }
            } catch (Exception e) {
                // use the default
                SERVER_PING_INTERVAL_SECONDS = DEFAULT_PING_INTERVAL_SECONDS + ONE_MIN_SECONDS;
            }
        }

    }

    private static void terminateAndReconnect() {
        if (receivedPing) {
            // reset the received ping flag
            receivedPing = false;
        }

        // initiate the terminate and reconnect call
        AsyncManager.getInstance().executeOnceInSeconds(
                () -> terminateAndReconnect(), SERVER_PING_INTERVAL_SECONDS);
    }

    public static void connect() {
        try {
            UtilManager.TimesData timesData = UtilManager.getTimesData();

            ws = new WebSocketFactory().createSocket(ConfigManager.WS_URL, 10000);
            ws.setMissingCloseFrameAllowed(true);
            ws.addHeader("Authorization", FileUtilManager.getItem("jwt"));
            ws.addHeader("X-SWDC-Plugin-Id", String.valueOf(ConfigManager.plugin_id));
            ws.addHeader("X-SWDC-Plugin-Name", ConfigManager.plugin_name);
            ws.addHeader("X-SWDC-Plugin-Version", ConfigManager.plugin_version);
            ws.addHeader("X-SWDC-Plugin-OS", UtilManager.getOs());
            ws.addHeader("X-SWDC-Plugin-TZ", timesData.timezone);
            ws.addHeader("X-SWDC-Plugin-Offset", String.valueOf(timesData.offset));
            ws.addHeader("X-SWDC-Plugin-Ide-Name", ConfigManager.ide_name);
            ws.addHeader("X-SWDC-Plugin-Ide-Version", ConfigManager.ide_version);
            ws.addHeader("X-SWDC-Plugin-UUID", FileUtilManager.getPluginUuid());

            // Register a listener to receive WebSocket events
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    try {
                        JsonObject obj = UtilManager.gson.fromJson(message, JsonObject.class);
                        if (obj != null && obj.has("type")) {
                            log.info("[WS-EVENT] " + obj.get("type").getAsString() + " event received");
                            JsonElement body = obj.get("body");
                            switch (obj.get("type").getAsString()) {
                                case "info":
                                    break;
                                case "flow_score":
                                    FlowMessageHandler.handleFlowScoreMessage(obj);
                                    break;
                                case "flow_state":
                                    boolean enable_flow = obj.get("body").getAsJsonObject().get("enable_flow").getAsBoolean();
                                    FlowMessageHandler.handleFlowStateMessage(enable_flow);
                                    break;
                                case "authenticated_plugin_user":
                                    User user = UtilManager.gson.fromJson(body, User.class);
                                    AuthenticatedPluginUser.handleAuthenticatedPluginUser(user);
                                    break;
                                case "current_day_stats_update":
                                    CurrentDayStatsEvent statsEvent = UtilManager.gson.fromJson(body, CurrentDayStatsEvent.class);
                                    StatsUpdateHandler.handleStatsUpdate(statsEvent.data);
                                    break;
                                case "user_integration_connection":
                                    IntegrationConnectionEvent integrationEvent = UtilManager.gson.fromJson(body, IntegrationConnectionEvent.class);
                                    IntegrationConnection.handleIntegrationConnectionEvent(integrationEvent);
                                    break;
                                case "billing_plan_update":
                                    BillingUpdateMessageHandler.handleBillingPlanUpdateMessage(obj);
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        log.warning("[WS-EVENT] Unable to handle incoming message: " + e.getMessage());
                    }
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                    // retry
                    AsyncManager.getInstance().executeOnceInSeconds(() -> terminateAndReconnect(), 25);
                }
                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                    log.info("Websocket has connected: " + websocket.toString());
                }
                @Override
                public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
                    // retry
                    AsyncManager.getInstance().executeOnceInSeconds(() -> WebsocketClient.connect(), 60 * 5);
                }

                @Override
                public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                    heartbeat(frame.getPayload());
                }
            });

            // Connect to the server and perform an opening handshake.
            // This method blocks until the opening handshake is finished.
            ws.connect();

            if (!initializedPingSchedule) {
                // init ping as received
                receivedPing = true;
                AsyncManager.getInstance().executeOnceInSeconds(
                        () -> terminateAndReconnect(), SERVER_PING_INTERVAL_SECONDS);
                initializedPingSchedule = true;
            }
        } catch (Exception e) {
            log.warning("[WS-EVENT] Socket creation error: " + e.getMessage());
        }
    }
}
