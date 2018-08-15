package com.tangem.presentation.fragment;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketClass extends WebSocketListener {
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        webSocket.send("Hello");
        Log.i("WS_scsc", "scsc");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.i("WS_scsc", text);
        System.out.print(text);
    }

}