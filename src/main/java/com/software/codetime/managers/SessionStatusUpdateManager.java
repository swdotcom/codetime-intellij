package com.software.codetime.managers;

import com.intellij.openapi.application.ApplicationManager;
import com.software.codetime.main.StatusBarManager;
import com.software.codetime.models.SessionSummary;

public class SessionStatusUpdateManager implements StatusBarUpdateHandler {
    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        ApplicationManager.getApplication().invokeLater(() -> {
            StatusBarManager.updateStatusBar(sessionSummary);
        });
    }
}
