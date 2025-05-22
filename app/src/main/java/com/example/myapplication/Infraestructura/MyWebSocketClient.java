package com.example.myapplication.Infraestructura;

import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MyWebSocketClient {

private OkHttpClient client;
private WebSocket webSocket;
private String ip;
private String port;
private String clientId;
private WebSocketMessageListener listener; // El listener para pasar los datos

// Constructor con listener
public MyWebSocketClient(String ip, String port, String clientId, WebSocketMessageListener listener) {
        this.ip = ip;
        this.port = port;
        this.clientId = clientId;
        this.listener = listener;
        }

// Interfaz para escuchar los mensajes de WebSocket
public interface WebSocketMessageListener {
    void onMessageReceived(String message);
    void onConnected();
    void onDisconnected();
    void onError(String error);
}

    public void start() {
        client = new OkHttpClient();
        String modelo = Build.MODEL;
        String fabricante = Build.MANUFACTURER;
        String url = "ws://" + ip + ":" + port + "/ws/?clientId=" + clientId+"&name="+modelo;
        Request request = new Request.Builder().url(url).build();

        Log.d("url",url);
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocket", "Conectado a " + url);
              //  webSocket.send("¡Hola desde Android!");
                if (listener != null) {
                    listener.onConnected();
                   // listener.onWaitingForConfirmation();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("WebSocket", "Mensaje recibido: " + text);

                // Pasar el mensaje recibido al listener de la UI
                if (listener != null) {
                    listener.onMessageReceived(text);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d("WebSocket", "Cerrando: " + reason);
                if (listener != null) {
                    listener.onDisconnected();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocket", "Error: " + t.getMessage(),t);
                if (response != null) {
                    Log.e("WebSocket", "Response: " + response.toString());
                }
                if (listener != null) {
                    listener.onError(t.getMessage());
                }
            }
        });
    }

    public void stop() {
        if (webSocket != null) {
            webSocket.close(1000, "Cierre manual");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
    public void sendMessage(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        } else {
            Log.e("WebSocket", "WebSocket no está conectado");
        }
    }
}