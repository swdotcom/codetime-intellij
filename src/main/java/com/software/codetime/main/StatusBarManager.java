package com.software.codetime.main;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.SessionSummary;
import com.software.codetime.models.SessionSummaryManager;
import com.software.codetime.models.StatusBarKpmIconWidget;
import com.software.codetime.models.StatusBarKpmTextWidget;
import com.software.codetime.snowplow.events.UIInteractionType;
import com.software.codetime.toolwindows.codetime.SidebarToolWindow;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

public class StatusBarManager {
    private static boolean showStatusText = true;

    private final static String kpmmsgId = StatusBarKpmTextWidget.KPM_TEXT_ID;
    private final static String kpmiconId = StatusBarKpmIconWidget.KPM_ICON_ID;
    private final static String flowmsgId = StatusBarKpmTextWidget.FLOW_TEXT_ID;
    private final static String flowiconId = StatusBarKpmIconWidget.FLOW_ICON_ID;

    public static boolean showingStatusText() {
        return showStatusText;
    }

    public static void toggleStatusBar(UIInteractionType interactionType) {
        showStatusText = !showStatusText;

        if (showStatusText) {
            updateStatusBar(null);
        } else {
            hideStatus();
        }

        // refresh the tree
        SidebarToolWindow.refresh(false);
    }

    private static void hideStatus() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ProjectManager pm = ProjectManager.getInstance();
            if (pm == null || pm.getOpenProjects() == null) {
                return;
            }
            for (Project p : pm.getOpenProjects()) {
                try {
                    final StatusBar statusBar = WindowManager.getInstance().getStatusBar(p);
                    if (statusBar == null) {
                        continue;
                    }
                    updateTextWidget(statusBar, flowmsgId, "", "");
                    updateIconWidget(statusBar, flowiconId, null, "");
                    updateTextWidget(statusBar, kpmmsgId, "", "");
                } catch (Exception e) {
                    // ignore
                }
            }
        });
    }

    public static void updateStatusBar(SessionSummary sessionSummary) {
        // Do any I/O / computation off the UI thread, then update UI once.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            SessionSummary summary = sessionSummary != null ? sessionSummary : SessionSummaryManager.fetchSessionSummary();

            // Persist only when content changed (avoids disk churn + watch feedback loops).
            FileUtilManager.writeDataIfChanged(FileUtilManager.getSessionDataSummaryFile(), summary);

            final String name = FileUtilManager.getItem("name");
            final boolean loggedIn = StringUtils.isNotBlank(name);
            final String currentDayTimeStr = UtilManager.humanizeMinutes(summary.currentDayMinutes);

            // show Code Time if the current day time is null or the user is not registered
            final String kpmMsgVal = loggedIn && currentDayTimeStr != null ? currentDayTimeStr : ConfigManager.plugin_name;

            final String kpmIcon = "time-clock.png";
            final String metricIconTooltip = "";
            final String kpmTextTooltip = buildTooltip("Active code time today. Click to see more from Code Time.", name);

            String flowTooltip = null;
            String flowIcon = null;
            if (loggedIn) {
                flowTooltip = "Enter Flow Mode";
                flowIcon = "open-circle.png";
                try {
                    if (FileUtilManager.getFlowChangeState()) {
                        flowIcon = "closed-circle.png";
                        flowTooltip = "Exit Flow Mode";
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
            final String flowTooltipFinal = flowTooltip;
            final String flowIconFinal = flowIcon;
            final String flowTextTooltip = flowTooltipFinal != null ? buildTooltip(flowTooltipFinal, name) : "";

            ApplicationManager.getApplication().invokeLater(() -> {
                if (!showStatusText) {
                    return;
                }

                ProjectManager pm = ProjectManager.getInstance();
                if (pm == null || pm.getOpenProjects() == null) {
                    return;
                }

                for (Project p : pm.getOpenProjects()) {
                    try {
                        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(p);
                        if (statusBar == null) {
                            continue;
                        }

                        // Code time widget
                        updateIconWidget(statusBar, kpmiconId, kpmIcon, metricIconTooltip);
                        updateTextWidget(statusBar, kpmmsgId, kpmMsgVal, kpmTextTooltip);

                        // Flow widgets (clear them when logged out)
                        if (loggedIn) {
                            updateIconWidget(statusBar, flowiconId, flowIconFinal, flowTooltipFinal);
                            updateTextWidget(statusBar, flowmsgId, "Flow", flowTextTooltip);
                        } else {
                            updateIconWidget(statusBar, flowiconId, null, "");
                            updateTextWidget(statusBar, flowmsgId, "", "");
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            });
        });
    }

    private static void updateIconWidget(StatusBar statusBar, String widgetId, String icon, String tooltip) {
        if (statusBar == null) {
            return;
        }
        StatusBarKpmIconWidget kpmIconWidget = (StatusBarKpmIconWidget) statusBar.getWidget(widgetId);
        if (kpmIconWidget != null) {
            boolean changed = false;

            String currentIconName = kpmIconWidget.getIconName();
            if ((icon == null && currentIconName != null) || (icon != null && !icon.equals(currentIconName))) {
                kpmIconWidget.updateIcon(icon);
                changed = true;
            }

            String normalizedTooltip = tooltip != null ? tooltip : "";
            if (!normalizedTooltip.equals(kpmIconWidget.getTooltip())) {
                kpmIconWidget.setTooltip(normalizedTooltip);
                changed = true;
            }

            if (changed) {
                statusBar.updateWidget(widgetId);
            }
        }
    }

    private static void updateTextWidget(StatusBar statusBar, String widgetId, String msg, String tooltip) {
        if (statusBar == null) {
            return;
        }
        StatusBarKpmTextWidget kpmMsgWidget = (StatusBarKpmTextWidget) statusBar.getWidget(widgetId);
        if (kpmMsgWidget != null) {
            String normalizedMsg = msg != null ? msg : "";
            String normalizedTooltip = tooltip != null ? tooltip : "";

            boolean changed = false;
            if (!normalizedMsg.equals(kpmMsgWidget.getText())) {
                kpmMsgWidget.setText(normalizedMsg);
                changed = true;
            }
            if (!normalizedTooltip.equals(kpmMsgWidget.getTooltip())) {
                kpmMsgWidget.setTooltip(normalizedTooltip);
                changed = true;
            }

            if (changed) {
                statusBar.updateWidget(widgetId);
            }
        }
    }

    private static String buildTooltip(String baseTooltip, String name) {
        String tooltip = baseTooltip;
        if (tooltip == null) {
            tooltip = "Code time today. Click to see more from Code Time.";
        }
        if (tooltip.lastIndexOf(".") != tooltip.length() - 1) {
            tooltip += ".";
        }
        if (StringUtils.isNotBlank(name)) {
            tooltip += " Logged in as " + name;
        }
        return tooltip;
    }
}
