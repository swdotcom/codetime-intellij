package com.software.codetime.models;

import com.intellij.openapi.application.ApplicationManager;
import com.software.codetime.http.FlowModeClient;
import com.software.codetime.main.StatusBarManager;
import com.software.codetime.managers.AccountManager;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.toolwindows.codetime.SidebarToolWindow;
import com.software.codetime.utils.FileUtilManager;

public class FlowManager {
    public static void initFlowStatus() {
        boolean enabledFlow = FlowModeClient.isFlowModeOn();
        FileUtilManager.updateFlowChangeState(enabledFlow);
        updateFlowStateDisplay();
    }

    public static void toggleFlowMode(boolean automated) {
        if (!FileUtilManager.getFlowChangeState()) {
            enterFlowMode(automated);
        } else {
            exitFlowMode();
        }
    }

    public static void enterFlowMode(boolean automated) {
        boolean isRegistered = AccountManager.checkRegistration(false, null);
        if (!automated && !isRegistered) {
            // show the flow mode prompt
            AccountManager.showModalSignupPrompt("To use Flow Mode, please first sign up or login.", () -> { SidebarToolWindow.refresh(true);});
            return;
        }

        if (!FileUtilManager.getFlowChangeState()) {
            // go ahead and make the api call to enter flow mode
            FlowModeClient.enterFlowMode(automated);
            FileUtilManager.updateFlowChangeState(true);
        }

        updateFlowStateDisplay();
    }

    public static void exitFlowMode() {
        boolean isInFlow = FileUtilManager.getFlowChangeState();
        if (isInFlow) {
            FlowModeClient.exitFlowMode();
            FileUtilManager.updateFlowChangeState(false);
        }

        updateFlowStateDisplay();
    }

    private static void updateFlowStateDisplay() {
        ApplicationManager.getApplication().invokeLater(() -> {
            // at least update the status bar
            AsyncManager.getInstance().executeOnceInSeconds(() -> {
                SidebarToolWindow.refresh(false);
            }, 2);
            StatusBarManager.updateStatusBar(null);
        });
    }
}
