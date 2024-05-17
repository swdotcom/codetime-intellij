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
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ProjectManager pm = ProjectManager.getInstance();
                if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
                    try {
                        String email = FileUtilManager.getItem("name");
                        Project p = pm.getOpenProjects()[0];
                        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(p);
                        updateTextWidget(statusBar, flowmsgId, "", "");
                        updateTextWidget(statusBar, kpmmsgId, "", "");
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    public static void updateStatusBar(SessionSummary sessionSummary) {
        if (sessionSummary == null) {
            sessionSummary = SessionSummaryManager.fetchSessionSummary();
        }

        FileUtilManager.writeData(FileUtilManager.getSessionDataSummaryFile(), sessionSummary);

        String currentDayTimeStr = UtilManager.humanizeMinutes(sessionSummary.currentDayMinutes);

        // build the status bar text information
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ProjectManager pm = ProjectManager.getInstance();
                if (pm != null && pm.getOpenProjects() != null && pm.getOpenProjects().length > 0) {
                    try {
                        String email = FileUtilManager.getItem("name");
                        Project p = pm.getOpenProjects()[0];
                        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(p);

                        // show Code Time if the current day time is null or the user is not registered
                        String kpmMsgVal = StringUtils.isNotBlank(email) && currentDayTimeStr != null ? currentDayTimeStr : ConfigManager.plugin_name;

                        // icon first
                        String metricIconTooltip = "";
                        String kpmIcon = "time-clock.png";
                        updateIconWidget(statusBar, kpmiconId, kpmIcon, metricIconTooltip);

                        // text next
                        String kpmTextTooltip = "Active code time today. Click to see more from Code Time.";
                        updateTextWidget(statusBar, kpmmsgId, kpmMsgVal, kpmTextTooltip);

                        // don't show the flow mode icon if the user is not logged in
                        if (StringUtils.isNotBlank(email)) {
                            // flow icon
                            String flowTooltip = "Enter Flow Mode";
                            String flowIcon = "open-circle.png";
                            try {
                                if (FileUtilManager.getFlowChangeState()) {
                                    flowIcon = "closed-circle.png";
                                    flowTooltip = "Exit Flow Mode";
                                }
                                updateIconWidget(statusBar, flowiconId, flowIcon, flowTooltip);

                                // flow text next
                                updateTextWidget(statusBar, flowmsgId, "Flow", flowTooltip);
                            } catch (Exception e) {
                                System.out.println("status bar update error: " + e.getMessage());
                            }
                        }
                    } catch(Exception e){
                        //
                    }
                }
            }
        });
    }

    private static void updateIconWidget(StatusBar statusBar, String widgetId, String icon, String tooltip) {
        StatusBarKpmIconWidget kpmIconWidget = (StatusBarKpmIconWidget) statusBar.getWidget(widgetId);
        if (kpmIconWidget != null) {
            kpmIconWidget.updateIcon(icon);
            kpmIconWidget.setTooltip(tooltip);
            statusBar.updateWidget(widgetId);
        }
    }

    private static void updateTextWidget(StatusBar statusBar, String widgetId, String msg, String tooltip) {
        StatusBarKpmTextWidget kpmMsgWidget = (StatusBarKpmTextWidget) statusBar.getWidget(widgetId);
        if (kpmMsgWidget != null) {
            kpmMsgWidget.setText(msg);
            kpmMsgWidget.setTooltip(tooltip);
            statusBar.updateWidget(widgetId);
        }
    }
}
