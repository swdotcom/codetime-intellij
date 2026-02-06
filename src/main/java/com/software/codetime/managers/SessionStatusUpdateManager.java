package com.software.codetime.managers;

import com.software.codetime.main.StatusBarManager;
import com.software.codetime.models.SessionSummary;

public class SessionStatusUpdateManager implements StatusBarUpdateHandler {
    @Override
    public void updateEditorStatus(SessionSummary sessionSummary) {
        // StatusBarManager handles threading; avoid redundant invokeLater nesting.
        StatusBarManager.updateStatusBar(sessionSummary);
    }
}
