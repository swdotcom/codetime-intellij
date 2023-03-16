package com.software.codetime.managers;

import com.intellij.openapi.application.ApplicationManager;
import swdc.java.ops.manager.StatusBarUpdateHandler;
import swdc.java.ops.model.SessionSummary;

public class SessionStatusUpdateManager implements StatusBarUpdateHandler {
    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        ApplicationManager.getApplication().invokeLater(() -> {
            StatusBarManager.updateStatusBar(sessionSummary);
        });
    }
}
