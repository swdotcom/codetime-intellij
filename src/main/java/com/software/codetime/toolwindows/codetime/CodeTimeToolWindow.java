package com.software.codetime.toolwindows.codetime;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.CefApp;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CodeTimeToolWindow {

    private final JBCefBrowser browser;

    public CodeTimeToolWindow(@NotNull Project project) {
        browser = new JBCefBrowser();
        browser.getJBCefClient().addDisplayHandler(new CodeTimeDisplayHandler(), browser.getCefBrowser());
        registerAppSchemeHandler();
        Disposer.register(project, browser);
        browser.loadURL("http://codetime/index.html");
    }

    private void registerAppSchemeHandler() {
        CefApp.getInstance().registerSchemeHandlerFactory("http", "codetime", new CodeTimeSchemeHandlerFactory());
    }

    public JComponent getContent() {
        return browser.getComponent();
    }

    public void refresh() {
        browser.loadURL("http://codetime/index.html");
    }

}
