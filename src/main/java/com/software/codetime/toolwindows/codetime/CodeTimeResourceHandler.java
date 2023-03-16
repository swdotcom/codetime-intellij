package com.software.codetime.toolwindows.codetime;

import com.google.gson.JsonObject;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.software.codetime.managers.StatusBarManager;
import com.software.codetime.toolwindows.WebviewClosedConnection;
import com.software.codetime.toolwindows.WebviewOpenedConnection;
import com.software.codetime.toolwindows.WebviewResourceState;
import com.software.codetime.toolwindows.codetime.html.*;
import org.apache.commons.lang.StringUtils;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefCookie;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import swdc.java.ops.http.ClientResponse;
import swdc.java.ops.http.OpsHttpClient;
import swdc.java.ops.manager.FileUtilManager;
import swdc.java.ops.manager.UtilManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.MalformedURLException;
import java.net.URL;

public class CodeTimeResourceHandler implements CefResourceHandler {

    private WebviewResourceState state = new WebviewClosedConnection();

    private long ref = 0;

    public long getNativeRef(String identifer) {
        return ref;
    }

    public void setNativeRef(String identifer, long nativeRef) {
        ref = nativeRef;
    }

    public boolean canSetCookie(CefCookie cookie)
    {
        return false;
    }

    public boolean canGetCookie(CefCookie cookie)
    {
        return false;
    }

    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback cefCallback) {
        String url = cefRequest.getURL();
        if (StringUtils.isNotBlank(url)) {

            String pathToResource = url.replace("http://codetime", "codetime");
            URL resourceUrl = getClass().getClassLoader().getResource(pathToResource);

            File f = new File(FileUtilManager.getCodeTimeViewHtmlFile());
            Writer writer = null;
            try {
                String html = buildHtml();

                writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(f), StandardCharsets.UTF_8));
                writer.write(html);
            } catch (Exception e) {
                System.out.println("Code time window write error: " + e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Exception e) {
                        System.out.println("Writer close error: " + e.getMessage());
                    }
                }
            }

            try {
                resourceUrl = f.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            try {
                state = new WebviewOpenedConnection(resourceUrl.openConnection());
            } catch (Exception e) {
                //
            }
            cefCallback.Continue();
            return true;
        }
        return false;
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl) {
        state.getResponseHeaders(cefResponse, responseLength, redirectUrl);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback) {
        return state.readResponse(dataOut, designedBytesToRead, bytesRead, callback);
    }

    @Override
    public void cancel() {
        state.close();
        state = new WebviewClosedConnection();
    }

    private String buildHtml() {
        JsonObject obj = new JsonObject();
        obj.addProperty("showing_statusbar", StatusBarManager.showingStatusText());
        obj.addProperty("skip_slack_connect", FileUtilManager.getBooleanItem("intellij_CtskipSlackConnect"));
        String qStr = UtilManager.buildQueryString(obj, true);
        String api = "/plugin/sidebar" + qStr;
        ClientResponse resp = OpsHttpClient.appGet(api);
        if (resp.isOk()) {
            return CssUtil.updateBodyCss(resp.getJsonStr());
        }
        return LoadError.get404Html();
    }

}
