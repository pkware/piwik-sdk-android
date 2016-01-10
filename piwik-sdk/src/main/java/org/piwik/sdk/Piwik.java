package org.piwik.sdk;

import android.content.Context;

import org.piwik.sdk.dispatcher.Dispatcher;
import org.piwik.sdk.storage.InMemoryStorageEngine;
import org.piwik.sdk.storage.StorageEngine;

public final class Piwik {
    public static final String LOGGER_PREFIX = "PIWIK:";
    private final Context mContext;

    private final Settings settings;
    private final StorageEngine storageEngine;
    private final Dispatcher dispatcher;

    private Piwik(Builder builder) {
        mContext = builder.context;
        storageEngine = builder.storageEngine;
        dispatcher = builder.dispatcher;
        settings = new Settings(mContext);
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * @param siteId (required) id of site
     * @return Tracker object
     */
    public Tracker newTracker(int siteId) {
        return new Tracker(storageEngine, siteId, this, settings);
    }

    public String getApplicationDomain() {
        return mContext.getPackageName();
    }

    public void dispatch() {
        dispatcher.dispatch(storageEngine);
    }

    public static class Builder {
        private final Context context;
        private Dispatcher dispatcher;
        private StorageEngine storageEngine;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder dispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

        public Builder storageEngine(StorageEngine storageEngine) {
            this.storageEngine = storageEngine;
            return this;
        }

        public Piwik build() {
            if (dispatcher == null) {
                throw new IllegalArgumentException("You must provide a Dispatcher using dispatcher()");
            }
            if (storageEngine == null) {
                storageEngine = new InMemoryStorageEngine();
            }
            return new Piwik(this);
        }
    }
}
