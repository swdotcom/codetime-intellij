package com.software.codetime.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.software.codetime.events.UserStateChangeModel;
import com.software.codetime.http.ClientResponse;
import com.software.codetime.http.OpsHttpClient;
import com.software.codetime.models.*;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountManager {
    public static final Logger LOG = Logger.getLogger("AccountManager");

    private static User cachedUser = null;

    public static User getCachedUser() {
        if (cachedUser == null) {
            cachedUser = getUser();
        }
        return cachedUser;
    }

    public static void showAuthSelectPrompt(boolean isSignup, Runnable callback) {
        showAuthSelectPrompt(isSignup, ConfigManager.plugin_name, callback);
    }

    public static void showAuthSelectPrompt(boolean isSignup, String pluginName, Runnable callback) {
        boolean hasExistingAccount = StringUtils.isNotBlank(FileUtilManager.getItem("name"));
        String promptText = isSignup ? "Sign up" : "Log in";
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        String input = (String) JOptionPane.showInputDialog(
                null,
                promptText + " using",
                promptText,
                JOptionPane.QUESTION_MESSAGE,
                UtilManager.getResourceIcon("app-icon-blue.png", null),
                options, // Array of choices
                options[0]); // Initial choice
        boolean switchingAccount = hasExistingAccount || !isSignup;
        launchLogin(input.toLowerCase(), switchingAccount, pluginName, callback);
    }

    public static void launchLogin(String loginType, boolean switching_account, String pluginName, Runnable callback) {
        try {
            String auth_callback_state = FileUtilManager.getAuthCallbackState(true);

            FileUtilManager.setBooleanItem("switching_account", switching_account);

            String jwt = FileUtilManager.getItem("jwt");
            String name = FileUtilManager.getItem("name");

            String plugin_uuid = FileUtilManager.getPluginUuid();

            JsonObject obj = new JsonObject();
            obj.addProperty("plugin", "codetime");
            obj.addProperty("pluginVersion", ConfigManager.plugin_version);
            obj.addProperty("plugin_id", ConfigManager.plugin_id);
            obj.addProperty("auth_callback_state", auth_callback_state);
            obj.addProperty("redirect", ConfigManager.app_url);
            obj.addProperty("plugin_uuid", plugin_uuid);

            // send the plugin uuid and token if the user is not yet registered
            if (StringUtils.isBlank(name)) {
                obj.addProperty("plugin_token", jwt);
            }

            String url = "";
            if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
                if (!switching_account) {
                    url = ConfigManager.app_url + "/email-signup";
                } else {
                    url = ConfigManager.app_url + "/onboarding";
                }
            } else if (loginType.equals("google")) {
                url = ConfigManager.metrics_endpoint + "/auth/google";
            } else if (loginType.equals("github")) {
                url = ConfigManager.metrics_endpoint + "/auth/github";
            }

            url += UtilManager.buildQueryString(obj, true);

            FileUtilManager.setItem("authType", loginType);

            UtilManager.launchUrl(url);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Unable to launch the url: {0}", e.getMessage());
        }
    }

    public static User getUser() {
        User softwareUser = null;
        ClientResponse resp = OpsHttpClient.appGet("/api/v1/user");
        if (resp.isOk()) {
            JsonObject obj = resp.getJsonObj();

            if (obj != null) {
                softwareUser = buildUser(obj);

                long sessionThresholdInSec = ConfigManager.DEFAULT_SESSION_THRESHOLD_SECONDS;
                FileUtilManager.setNumericItem("sessionThresholdInSec", sessionThresholdInSec);
                boolean disableGitData = softwareUser.preferences.disableGitData;
                FileUtilManager.setBooleanItem("disableGitData", disableGitData);

                // i.e. "flowMode":{"durationMinutes":"90","editor":{"autoEnterFlowMode":true,"vscode":{"screenMode":"None"}
                FileUtilManager.setItem("flowMode", UtilManager.gson.toJson(softwareUser.preferences.flowMode));
            }
        }
        cachedUser = softwareUser;
        return softwareUser;
    }

    private static User buildUser(JsonObject userJson) {
        User softwareUser = new User();
        if (userJson.has("plugin_jwt")) {
            softwareUser.plugin_jwt = userJson.get("plugin_jwt").getAsString();
        } else {
            softwareUser.plugin_jwt = FileUtilManager.getItem("jwt");
        }
        softwareUser.registered = userJson.get("registered").getAsInt();
        softwareUser.email = userJson.get("email").getAsString();
        JsonObject userPrefs = userJson.get("preferences_parsed").getAsJsonObject();
        softwareUser.preferences = UtilManager.gson.fromJson(userPrefs, UserPreferences.class);
        if (userJson.has("integration_connections")) {
            JsonArray integrationConnectionsJson = userJson.get("integration_connections").getAsJsonArray();
            List<IntegrationConnection> connections = new ArrayList<>();
            for (JsonElement el : integrationConnectionsJson) {
                Type integrationType = new TypeToken<IntegrationConnection>() {}.getType();
                connections.add(UtilManager.gson.fromJson(el, integrationType));
            }
            softwareUser.integration_connections = connections;
        }
        if (userJson.has("latest_plugin_connections")) {
            JsonArray pluginConnectionsJson = userJson.get("latest_plugin_connections").getAsJsonArray();
            List<PluginConnection> connections = new ArrayList<>();
            for (JsonElement el : pluginConnectionsJson) {
                Type integrationType = new TypeToken<PluginConnection>() {}.getType();
                connections.add(UtilManager.gson.fromJson(el, integrationType));
            }
            softwareUser.latest_plugin_connections = connections;
        }
        JsonObject profile = userJson.get("profile").getAsJsonObject();
        softwareUser.profile = UtilManager.gson.fromJson(profile, Profile.class);
        return softwareUser;
    }

    public static String createAnonymousUser(boolean ignoreJwt) {
        // make sure we've fetched the app jwt
        String jwt = FileUtilManager.getItem("jwt");

        if (StringUtils.isBlank(jwt) || ignoreJwt) {
            String timezone = TimeZone.getDefault().getID();

            String plugin_uuid = FileUtilManager.getPluginUuid();
            String auth_callback_state = FileUtilManager.getAuthCallbackState(true);

            JsonObject payload = new JsonObject();
            payload.addProperty("username", UtilManager.getOsUsername());
            payload.addProperty("timezone", timezone);
            payload.addProperty("hostname", UtilManager.getHostname());
            payload.addProperty("auth_callback_state", auth_callback_state);
            payload.addProperty("plugin_uuid", plugin_uuid);

            String api = "/api/v1/anonymous_user";

            ClientResponse resp = OpsHttpClient.appPost(api, payload);
            if (resp.isOk()) {
                // check if we have the data and jwt
                // resp.data.jwt and resp.data.user
                // then update the session.json for the jwt
                JsonObject data = resp.getJsonObj();
                // check if we have any data
                if (data != null) {
                    User softwareUser = buildUser(data);
                    FileUtilManager.setItem("jwt", softwareUser.plugin_jwt);
                    FileUtilManager.setItem("name", null);
                    FileUtilManager.setBooleanItem("switching_account", false);
                    FileUtilManager.setAuthCallbackState(null);
                    return softwareUser.plugin_jwt;
                }
            }
        }
        return null;
    }

    public static String getDecodedUserIdFromJwt(String jwt) {
        String stippedDownJwt = jwt.indexOf("JWT ") != -1 ? jwt.substring("JWT ".length()) : jwt;
        try {
            String[] split_string = stippedDownJwt.split("\\.");
            String base64EncodedBody = split_string[1];

            org.apache.commons.codec.binary.Base64 base64Url = new Base64(true);
            String body = new String(base64Url.decode(base64EncodedBody));
            Map<String, String> jsonMap;

            ObjectMapper mapper = new ObjectMapper();
            // convert JSON string to Map
            jsonMap = mapper.readValue(body,
                    new TypeReference<Map<String, String>>() {
                    });
            Object idVal = jsonMap.getOrDefault("id", "");
            return idVal.toString();
        } catch (Exception ex) {}
        return "";
    }

    public static boolean checkRegistration(boolean showSignup, Runnable callback) {
        String name = FileUtilManager.getItem("name");
        if (StringUtils.isBlank(name)) {
            // the user is not registerd
            if (showSignup) {
                String msg = "Connecting Slack requires a registered account. Sign up or log in to continue.";
                showModalSignupPrompt(msg, callback);
            }
            return false;
        }
        return true;
    }

    public static void showModalSignupPrompt(String msg, Runnable callback) {
        Object[] options = {"Sign up"};

        JTextPane jtp = new JTextPane();
        Document doc = jtp.getDocument();
        try {
            doc.insertString(doc.getLength(), msg, new SimpleAttributeSet());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        jtp.setSize(new Dimension(500, 225));
        int choice = JOptionPane.showOptionDialog(
                null, jtp, "Registration", JOptionPane.OK_OPTION,
                JOptionPane.QUESTION_MESSAGE, UtilManager.getResourceIcon("app-icon-blue.png", null), options, options[0]);

        if (choice == 0) {
            UserStateChangeModel changeModel = new UserStateChangeModel();
            AccountManager.showAuthSelectPrompt(true, () -> {
                if (callback != null) {
                    callback.run();
                }
                changeModel.dispatchAuthenticationCompletion();
            });
        }
    }
}
