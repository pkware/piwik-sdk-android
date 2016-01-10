/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;
import android.os.Build;

import org.piwik.sdk.dispatcher.WebDispatcher;
import org.piwik.sdk.storage.InMemoryStorageEngine;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public abstract class PiwikApplication extends Application {
    private Piwik piwik;
    private Tracker tracker;

    public synchronized Piwik getPiwik() {
        if (piwik == null) {
            piwik = new Piwik.Builder(this)
                    .dispatcher(new WebDispatcher(getTrackerUrl(), null, new OkHttpClient()))
                    .storageEngine(new InMemoryStorageEngine())
                    .build();
        }
        return piwik;
    }

    /**
     * Gives you an all purpose thread-safe persisted Tracker object.
     *
     * @return a shared Tracker
     */
    public synchronized Tracker getTracker() {
        if (tracker == null) {
            tracker = piwik.newTracker(getSiteId());
        }
        return tracker;
    }

    /**
     * The URL of your remote Piwik server.
     */
    public abstract HttpUrl getTrackerUrl();

    /**
     * The siteID you specified for this application in Piwik.
     */
    public abstract int getSiteId();


    @Override
    public void onLowMemory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH && tracker != null) {
            piwik.dispatch();
        }
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE) && tracker != null) {
            piwik.dispatch();
        }
        super.onTrimMemory(level);
    }

}
