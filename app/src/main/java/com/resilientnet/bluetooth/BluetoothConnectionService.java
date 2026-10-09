package com.resilientnet.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.List;

public class BluetoothConnectionService
        implements BluetoothConnectionListener,
        BluetoothMessageListener {

    private static BluetoothConnectionService instance;

    private final BluetoothConnectionManager connectionManager;

    private final Set<BluetoothConnectionObserver> observers =
            new CopyOnWriteArraySet<>();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private BluetoothConnectionService(Context context) {

        connectionManager = new BluetoothConnectionManager(
                context.getApplicationContext(),
                this,
                this
        );
    }

    public static synchronized BluetoothConnectionService
    getInstance(Context context) {

        if (instance == null) {

            instance =
                    new BluetoothConnectionService(
                            context
                    );
        }

        return instance;
    }

    // ---------------------------------------------------------
    // OBSERVERS
    // ---------------------------------------------------------

    public void registerObserver(
            BluetoothConnectionObserver observer) {

        if (observer != null) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(
            BluetoothConnectionObserver observer) {

        if (observer != null) {
            observers.remove(observer);
        }
    }

    // ---------------------------------------------------------
    // CONNECTION
    // ---------------------------------------------------------

    public void startServer() {

        connectionManager.startServer();
    }

    public void connectToDevice(
            BluetoothDevice device) {

        connectionManager.connectToDevice(device);
    }

    public boolean isConnected() {

        return connectionManager.isConnected();
    }

    public List<BluetoothConnectionThread> getActiveConnections() {

        return connectionManager.getActiveConnections();
    }
    // ---------------------------------------------------------
    // MESSAGE
    // ---------------------------------------------------------

    public void sendMessage(String message) {

        connectionManager.sendMessage(message);
    }

    // ---------------------------------------------------------
    // BLUETOOTH STATE
    // ---------------------------------------------------------

    public boolean isBluetoothAvailable() {

        return connectionManager.isBluetoothAvailable();
    }

    // ---------------------------------------------------------
    // CALLBACKS
    // ---------------------------------------------------------

    @Override
    public void onConnecting(
            BluetoothDevice device) {

        notifyObservers(
                observer ->
                        observer.onConnecting(device)
        );
    }

    @Override
    public void onConnected(
            BluetoothDevice device) {

        notifyObservers(
                observer ->
                        observer.onConnected(device)
        );
    }

    @Override
    public void onConnectionFailed(
            BluetoothDevice device,
            Exception exception) {

        notifyObservers(
                observer ->
                        observer.onConnectionFailed(
                                device,
                                exception
                        )
        );
    }

    @Override
    public void onDisconnected(
            BluetoothDevice device) {

        notifyObservers(
                observer ->
                        observer.onDisconnected(device)
        );
    }

    @Override
    public void onMessageReceived(
            BluetoothDevice device,
            String message) {

        notifyObservers(
                observer ->
                        observer.onMessageReceived(
                                device,
                                message
                        )
        );
    }

    // ---------------------------------------------------------
    // NOTIFICATION
    // ---------------------------------------------------------

    private void notifyObservers(
            ObserverAction action) {

        mainHandler.post(() -> {

            for (BluetoothConnectionObserver observer :
                    observers) {

                action.apply(observer);
            }
        });
    }

    private interface ObserverAction {

        void apply(
                BluetoothConnectionObserver observer
        );
    }

    // ---------------------------------------------------------
    // SHUTDOWN
    // ---------------------------------------------------------

    public void shutdown() {

        connectionManager.shutdown();
        observers.clear();
        instance = null;
    }
}