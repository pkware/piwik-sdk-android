package org.piwik.sdk.storage;

import java.util.Collection;
import java.util.List;

/**
 * A {@linkplain StorageEngine} provides a way of storing analytics events that are meant to be
 * batched.
 * <p/>
 * No guarantees about persistence must be made, but the engine must be thread safe.
 */
public interface StorageEngine {

    void add(String event);

    /**
     * Does not remove any events from the storage
     *
     * @return a copy of the stored events
     */
    List<String> get();

    void remove(Collection<String> events);

    boolean isEmpty();
}
