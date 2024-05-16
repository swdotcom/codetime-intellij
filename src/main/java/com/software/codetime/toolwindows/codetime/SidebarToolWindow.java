package com.software.codetime.toolwindows.codetime;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactoryImpl;
import com.intellij.ui.jcef.JBCefApp;
import com.software.codetime.listeners.ProjectActivateListener;
import com.software.codetime.managers.IntellijProjectManager;
import org.jetbrains.annotations.NotNull;

public class SidebarToolWindow implements ToolWindowFactory {
    private static CodeTimeToolWindow ctWindow;
    private static SidebarTreeView tv;
    private static ToolWindow tw;
    public static Project windowProject;


    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        tw = toolWindow;
        if (SidebarToolWindow.isJcefSupported()) {
            initWebView(project, tw);
        } else {
            initTreeView(project, tw);
        }
    }

    @Override
    public void init(@NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        ToolWindowFactory.super.init(toolWindow);
    }

    private static boolean isJcefSupported() {
        try {
            // Throws: IllegalStateException – when JCEF initialization is not possible in current env
            JBCefApp.getInstance();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void initWebView(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (ctWindow == null) {
            ctWindow = new CodeTimeToolWindow(project);
        }
        ContentFactoryImpl factoryImpl = new ContentFactoryImpl();
        Content content = factoryImpl.createContent(ctWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        windowProject = project;
        ctWindow.refresh();
    }

    private static void initTreeView(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (tv == null) {
            tv = new SidebarTreeView();
        }
        ContentFactoryImpl factoryImpl = new ContentFactoryImpl();
        Content content = factoryImpl.createContent(tv.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        windowProject = project;
        tv.refresh();
    }

    private static void initSidebar() {
        if (windowProject == null) {
            windowProject = ProjectActivateListener.getCurrentProject();
            if (windowProject == null) {
                windowProject = IntellijProjectManager.getFirstActiveProject();
            }
        }
        if (windowProject != null) {
            com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(windowProject).getToolWindow("CodeTime");
            if (toolWindow != null) {
                if (SidebarToolWindow.isJcefSupported()) {
                    initWebView(windowProject, tw);
                } else {
                    initTreeView(windowProject, tw);
                }
            }
        }
    }

    public static void refresh(boolean open) {
        if (SidebarToolWindow.isJcefSupported()) {
            if (ctWindow != null) {
                ctWindow.refresh();
            }
        } else {
            if (tv != null) {
                tv.refresh();
            }
        }
        if (open) {
            // open it
            openToolWindow();
        }
    }

    public static void openToolWindow() {
        initSidebar();
        if (windowProject != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(windowProject).getToolWindow("CodeTime");
                if (toolWindow != null) {
                    toolWindow.show();
                }
            });
        }
    }
}
