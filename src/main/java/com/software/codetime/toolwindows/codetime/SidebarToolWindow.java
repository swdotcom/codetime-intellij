package com.software.codetime.toolwindows.codetime;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.intellij.ui.jcef.JBCefApp;
import com.software.codetime.listeners.ProjectActivateListener;
import com.software.codetime.managers.IntellijProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SidebarToolWindow implements ToolWindowFactory {
    private static CodeTimeToolWindow ctWindow;
    private static TreeView tv;
    public static Project windowProject;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull com.intellij.openapi.wm.ToolWindow toolWindow) {
        if (windowProject != null) {
            return;
        }
        if (SidebarToolWindow.isJcefSupported()) {
            initWebView(project, toolWindow);
            ctWindow.refresh();
        } else {
            initTreeView(project, toolWindow);
            tv.refresh();
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
        ctWindow = new CodeTimeToolWindow(project);
        ContentFactoryImpl factoryImpl = new ContentFactoryImpl();
        Content content = factoryImpl.createContent(ctWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        windowProject = project;
    }

    private static void initTreeView(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        tv = new TreeView();
        ContentFactoryImpl factoryImpl = new ContentFactoryImpl();
        Content content = factoryImpl.createContent(tv.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
        windowProject = project;
    }

    private static void initialize() {
        if (SidebarToolWindow.isJcefSupported()) {
            if (ctWindow == null) {
                initializeWebView();
            }
        } else {
            if (tv == null) {
                Project p = ProjectActivateListener.getCurrentProject();
                if (p == null) {
                    p = IntellijProjectManager.getFirstActiveProject();
                }
                com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(p).getToolWindow("Code Time");
                if (toolWindow != null) {
                    initTreeView(p, toolWindow);
                }
            }
        }
    }

    private static void initializeWebView() {
        Project p = ProjectActivateListener.getCurrentProject();
        if (p == null) {
            p = IntellijProjectManager.getFirstActiveProject();
        }
        com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(p).getToolWindow("Code Time");
        if (toolWindow != null) {
            initWebView(p, toolWindow);
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
        initializeWebView();
        if (windowProject != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(windowProject).getToolWindow("Code Time");
                if (toolWindow != null) {
                    toolWindow.show();
                }
            });
        }
    }
}
