/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import org.piwik.sdk.tools.Logy;

/**
 * An exception handler that wraps the existing exception handler and dispatches event to a {@link org.piwik.sdk.Tracker}.
 * <p/>
 * Also see documentation for {@link org.piwik.sdk.QuickTrack#trackUncaughtExceptions(Tracker)}
 */
public class PiwikExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Piwik piwik;
    private final Tracker tracker;
    private final Thread.UncaughtExceptionHandler defaultExceptionHandler;

    public PiwikExceptionHandler(Piwik piwik, Tracker tracker) {
        this.piwik = piwik;
        this.tracker = tracker;
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            String excInfo = ex.getMessage();
            tracker.trackException(ex, excInfo, true);
            // Immediately dispatch as the app might be dying after rethrowing the exception
            piwik.dispatch();
        } catch (Exception e) {
            Logy.e(Tracker.LOGGER_TAG, "Couldn't track uncaught exception", e);
        } finally {
            // re-throw critical exception further to the os (important)
            if (defaultExceptionHandler != null && defaultExceptionHandler != this) {
                defaultExceptionHandler.uncaughtException(thread, ex);
            }
        }
    }
}
