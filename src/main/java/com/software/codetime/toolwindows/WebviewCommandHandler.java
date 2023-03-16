package com.software.codetime.toolwindows;

import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.software.codetime.managers.*;
import com.software.codetime.toolwindows.codetime.SidebarToolWindow;
import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.*;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class WebviewCommandHandler {

    public static boolean onConsoleCommand(String commandData) {
        try {
            JsonObject jsonObject = UtilManager.gson.fromJson(commandData, JsonObject.class);
            if (!jsonObject.isJsonNull() && jsonObject.has("cmd")) {
                String cmd = jsonObject.get("cmd").getAsString();
                jsonObject.remove("cmd");
                executeJavascriptCommands(cmd, jsonObject);
            }
        } catch (Exception e) {
            System.out.println("Console message error: " + e.getMessage());
        }
        return false;
    }

    private static void executeJavascriptCommands(String cmd, JsonObject data) {
        switch (cmd) {
            case "showOrgDashboard":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/devops_performance?organization_slug=" + data.get("payload").getAsString());
                });
                break;
            case "switchAccount":
                ApplicationManager.getApplication().invokeLater(() -> {
                    AuthPromptManager.initiateSwitchAccountFlow();
                });
                break;
            case "displayReadme":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl("https://github.com/swdotcom/intellij-codetime");
                });
                break;
            case "viewProjectReports":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/reports");
                });
                break;
            case "configureSettings":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/preferences");
                });
                break;
            case "submitAnIssue":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.submitIntellijIssue();
                });
                break;
            case "toggleStatusBar":
                ApplicationManager.getApplication().invokeLater(() -> {
                    StatusBarManager.toggleStatusBar(UIInteractionType.click);
                });
                break;
            case "viewDashboard":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/code_time?view=summary");
                });
                break;
            case "softwareKpmDashboard":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UserSessionManager.launchWebDashboard(UIInteractionType.click);
                });
                break;
            case "enableFlowMode":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FlowManager.enterFlowMode(false);
                });
                break;
            case "exitFlowMode":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FlowManager.exitFlowMode();
                });
                break;
            case "toggle_flow":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FlowManager.toggleFlowMode(false);
                });
                break;
            case "manageSlackConnection":
            case "connectSlack":
            case "disconnectSlackWorkspace":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.app_url + "/data_sources/integration_types/slack");
                });
                break;
            case "registerAccount":
                ApplicationManager.getApplication().invokeLater(() -> {
                    AuthPromptManager.initiateSignupFlow();
                });
                break;
            case "login":
                ApplicationManager.getApplication().invokeLater(() -> {
                    AuthPromptManager.initiateLoginFlow();
                });
                break;
            case "createOrg":
                ApplicationManager.getApplication().invokeLater(() -> {
                    UtilManager.launchUrl(ConfigManager.create_org_url);
                });
                break;
            case "skipSlackConnect":
                ApplicationManager.getApplication().invokeLater(() -> {
                    FileUtilManager.setBooleanItem("intellij_CtskipSlackConnect", true);
                    SidebarToolWindow.refresh(false);
                });
                break;
            case "refreshCodeTimeView":
                ApplicationManager.getApplication().invokeLater(() -> {
                    SidebarToolWindow.refresh(false);
                });
                break;
            case "updateSettings":
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        JsonObject payload = data.get("payload").getAsJsonObject();
                        String path = payload.get("path").getAsString();
                        JsonObject json = payload.get("json").getAsJsonObject();
                        ClientResponse resp = OpsHttpClient.appPut(path, json);
                        if (resp.isOk()) {
                            AsyncManager.getInstance().executeOnceInSeconds(() -> {
                                AccountManager.getUser();
                            }, 0);
                        }
                    } catch (Exception e) {}
                });
                break;
            default:
                break;
        }
    }
}
