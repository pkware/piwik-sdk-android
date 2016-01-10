package org.piwik.sdk.dispatcher;

import org.piwik.sdk.storage.StorageEngine;

public interface Dispatcher {

    /**
     * This will send all logged events to the destination receiver. It can be called from any thread.
     */
    void dispatch(StorageEngine storageEngine);
}
