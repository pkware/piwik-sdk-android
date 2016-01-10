package com.piwik.demo;

import android.app.Application;
import android.os.Build;

import com.pkware.piwik.sdk.Piwik;
import com.pkware.piwik.sdk.dispatcher.Dispatcher;
import com.pkware.piwik.sdk.dispatcher.DryRunDispatcher;
import com.pkware.piwik.sdk.dispatcher.WebDispatcher;
import com.pkware.piwik.sdk.storage.InMemoryStorageEngine;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class DemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Dispatcher dispatcher;
        if (BuildConfig.DEBUG) {
            dispatcher = new DryRunDispatcher();
        } else {
            HttpUrl serverUrl = HttpUrl.parse("http://beacons.testing.piwik.pro/piwik.php");
            dispatcher = new WebDispatcher(serverUrl, null, new OkHttpClient());
        }
        Injector.piwik = new Piwik.Builder(this)
                .dispatcher(dispatcher)
                .storageEngine(new InMemoryStorageEngine())
                .build();
        Injector.marketingTracker = Injector.piwik.newTracker(4);
        Injector.engineeringTracker = Injector.piwik.newTracker(5);

        Injector.marketingTracker.trackAppDownload();

        // The developer must schedule the dispatch if events. GcmNetworkManager or JobScheduler are
        // best for this, but the demo can use a simple Timer
        Timer dispatchScheduler = new Timer();
        dispatchScheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Injector.piwik.dispatch();
                    }
                }).run();
            }
        }, 5000, 5000);
    }

    @Override
    public void onLowMemory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            Injector.piwik.dispatch();
        }
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if ((level == TRIM_MEMORY_UI_HIDDEN || level == TRIM_MEMORY_COMPLETE)) {
            Injector.piwik.dispatch();
        }
        super.onTrimMemory(level);
    }
}
