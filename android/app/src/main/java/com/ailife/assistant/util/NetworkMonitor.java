package com.ailife.assistant.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

/**
 * 网络状态监听 — 实时检测断网/恢复
 */
public class NetworkMonitor {

    public interface OnNetworkChangeListener {
        void onLost();
        void onAvailable();
    }

    private final ConnectivityManager cm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OnNetworkChangeListener listener;
    private boolean wasAvailable = true;

    public NetworkMonitor(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void start(OnNetworkChangeListener listener) {
        this.listener = listener;
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(@androidx.annotation.NonNull Network network) {
                handler.post(() -> {
                    wasAvailable = false;
                    if (NetworkMonitor.this.listener != null) {
                        NetworkMonitor.this.listener.onLost();
                    }
                });
            }

            @Override
            public void onAvailable(@androidx.annotation.NonNull Network network) {
                handler.post(() -> {
                    if (!wasAvailable && NetworkMonitor.this.listener != null) {
                        NetworkMonitor.this.listener.onAvailable();
                    }
                    wasAvailable = true;
                });
            }
        };
        cm.registerNetworkCallback(request, callback);
    }

    public boolean isOnline() {
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
