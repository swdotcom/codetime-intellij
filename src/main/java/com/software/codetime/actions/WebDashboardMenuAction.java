package com.software.codetime.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.software.codetime.managers.UserSessionManager;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class WebDashboardMenuAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            UserSessionManager.launchWebDashboard(UIInteractionType.click);
        });
    }

    @Override
    public void update(AnActionEvent event) {
        String email = FileUtilManager.getItem("name");
        boolean isLoggedIn = StringUtils.isNotBlank(email);
        event.getPresentation().setVisible(isLoggedIn);
        event.getPresentation().setEnabled(true);
    }
}
