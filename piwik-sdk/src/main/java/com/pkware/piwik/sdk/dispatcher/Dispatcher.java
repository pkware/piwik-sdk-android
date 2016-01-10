package com.pkware.piwik.sdk.dispatcher;

import com.pkware.piwik.sdk.storage.StorageEngine;

public interface Dispatcher {

    /**
     * This will send all logged events to the destination receiver. It can be called from any thread.
     */
    void dispatch(StorageEngine storageEngine);
}
