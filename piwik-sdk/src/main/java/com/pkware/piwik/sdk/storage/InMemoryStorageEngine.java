package com.pkware.piwik.sdk.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class InMemoryStorageEngine implements StorageEngine {

    private final List<String> list = Collections.synchronizedList(new LinkedList<String>());

    @Override
    public void add(String event) {
        list.add(event);
    }

    @Override
    public List<String> get() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public void remove(Collection<String> events) {
        list.removeAll(events);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }
}
