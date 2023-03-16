package com.software.codetime.toolwindows;

import org.cef.callback.CefCallback;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefResponse;

public interface WebviewResourceState {
    void getResponseHeaders(CefResponse cefResponse, IntRef responseLength, StringRef redirectUrl);

    boolean readResponse(byte[] dataOut, int designedBytesToRead, IntRef bytesRead, CefCallback callback);

    void close();
}
