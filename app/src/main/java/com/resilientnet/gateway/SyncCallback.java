package com.resilientnet.gateway;

/**
 * Callback for monitoring backend sync execution.
 */
public interface SyncCallback {

    void onSyncStarted();

    void onSyncSuccess(int syncedSosCount, int syncedMessageCount);

    void onSyncFailed(String errorMessage);
}
