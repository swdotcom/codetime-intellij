package com.software.codetime.managers;

import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.*;
import swdc.java.ops.model.UserState;
import swdc.java.ops.snowplow.events.UIInteractionType;
import swdc.java.ops.websockets.handlers.AuthenticatedPluginUser;

import javax.swing.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.logging.Logger;

public class UserSessionManager {

    private static UserSessionManager instance = null;
    public static final Logger log = Logger.getLogger("SoftwareCoSessionManager");

    public static UserSessionManager getInstance() {
        if (instance == null) {
            instance = new UserSessionManager();
        }
        return instance;
    }


    public static String getReadmeFile() {
        String file = FileUtilManager.getSoftwareDir(true);
        if (UtilManager.isWindows()) {
            file += "\\jetbrainsCt_README.md";
        } else {
            file += "/jetbrainsCt_README.md";
        }
        return file;
    }

    public static void launchLogin(String loginType, UIInteractionType interactionType, boolean isSignUp) {
        String auth_callback_state = FileUtilManager.getAuthCallbackState(true);

        FileUtilManager.setBooleanItem("switching_account", !isSignUp);

        String plugin_uuid = FileUtilManager.getPluginUuid();

        JsonObject obj = new JsonObject();
        obj.addProperty("plugin_uuid", plugin_uuid);
        obj.addProperty("plugin_id", ConfigManager.plugin_id);
        obj.addProperty("auth_callback_state", auth_callback_state);

        String url = "";
        if (loginType == null || loginType.equals("software") || loginType.equals("email")) {
            obj.addProperty("token", FileUtilManager.getItem("jwt"));
            obj.addProperty("auth", "software");
            if (isSignUp) {
                url = ConfigManager.app_url + "/email-signup";
            } else {
                url = ConfigManager.app_url + "/onboarding";
            }
        } else if (loginType.equals("google")) {
            url = ConfigManager.app_url + "/auth/google";
        } else if (loginType.equals("github")) {
            url = ConfigManager.app_url + "/auth/github";
        }

        StringBuffer sb = new StringBuffer();
        Iterator<String> keys = obj.keySet().iterator();
        while(keys.hasNext()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            String key = keys.next();
            String val = obj.get(key).getAsString();
            try {
                val = URLEncoder.encode(val, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.info("Unable to url encode value, error: " + e.getMessage());
            }
            sb.append(key).append("=").append(val);
        }
        url += "?" + sb;

        FileUtilManager.setItem("authType", loginType);

        BrowserUtil.browse(url);

        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            checkAuthCompletion(10);}, 20);
    }

    private static void checkAuthCompletion(int tries) {
        if (tries < 0) {
            return;
        }
        String authCallbackState = FileUtilManager.getAuthCallbackState(false);
        UserState userState = AccountManager.getUserLoginState(false /*isIntegrationRequest*/);
        if (StringUtils.isNotBlank(authCallbackState) && (userState.user == null || userState.user.registered == 0)) {
            tries--;
            final int newTryCount = tries;
            AsyncManager.getInstance().executeOnceInSeconds(() -> {
                checkAuthCompletion(newTryCount);
            }, 15);
        } else if (StringUtils.isNotBlank(authCallbackState)) {
            AuthenticatedPluginUser.handleAuthenticatedPluginUser(userState.user);
        }
    }

    public static void launchWebDashboard(UIInteractionType interactionType) {
        if (StringUtils.isBlank(FileUtilManager.getItem("name"))) {
            ApplicationManager.getApplication().invokeLater(() -> {
                String msg = "\nSign up or log in to see more data visualizations.\n";

                Object[] options = {"Sign up"};
                Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", UserSessionManager.class.getClassLoader());
                int choice = JOptionPane.showOptionDialog(
                        null, msg, "\n Sign up \n", JOptionPane.OK_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon, options, options[0]);

                if (choice == 0) {
                    AuthPromptManager.initiateSignupFlow();
                }
            });
            return;
        }

        String url = ConfigManager.app_url;
        BrowserUtil.browse(url);
    }
}

