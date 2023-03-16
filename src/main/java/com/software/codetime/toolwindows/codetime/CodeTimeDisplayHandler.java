package com.software.codetime.toolwindows.codetime;

import com.software.codetime.toolwindows.WebviewCommandHandler;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;

public class CodeTimeDisplayHandler implements CefDisplayHandler {
    @Override
    public void onAddressChange(CefBrowser cefBrowser, CefFrame cefFrame, String s) {
    }

    @Override
    public void onTitleChange(CefBrowser cefBrowser, String s) {
    }

    @Override
    public boolean onTooltip(CefBrowser cefBrowser, String s) {
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser cefBrowser, String s) {
    }

    public boolean onCursorChange(CefBrowser cefBrowser, int i) {
        return false;
    }

    @Override
    public boolean onConsoleMessage(CefBrowser cefBrowser, CefSettings.LogSeverity logSeverity, String commandData, String s1, int i) {
        return WebviewCommandHandler.onConsoleCommand(commandData);
    }

}
