package com.software.codetime.toolwindows;


import org.cef.callback.CefCallback;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefResponse;

public class WebviewClosedConnection implements WebviewResourceState {

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl) {
        cefResponse.setStatus(404);
    }

    @Override
    public boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback) {
        return false;
    }

    @Override
    public void close() {

    }
}
