package com.software.codetime.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

public class DashboardMenuAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/code_time");
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
