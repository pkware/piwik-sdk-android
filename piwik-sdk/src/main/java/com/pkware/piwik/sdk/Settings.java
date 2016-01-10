package com.pkware.piwik.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public final class Settings {
    private static final String PREFERENCE_FILE_NAME = "com.pkware.piwik";
    private static final String KEY_OPT_OUT = "opt_out.";
    private static final String KEY_VISITOR_ID = "visitor_id.";
    private static final String KEY_USER_ID = "user_id.";
    private static final String KEY_FIRST_VISIT = "time_of_first_visit.";
    private static final String KEY_VISIT_COUNT = "visit_count.";
    private static final String KEY_LAST_VISIT = "time_of_last_visit.";
    private static final String KEY_TRACKED_INSTALLS = "tracked_installs.";

    private final SharedPreferences preferences;

    public Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
    }

    // TODO is this random enough? Does it make sense to generate our own bits and encode to hex using Okio's ByteString?
    private static String makeRandomVisitorId() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);
    }

    /**
     * Use this to disable Piwik, e.g. if the user opted out of tracking.
     * Piwik will persist the choice and remain disable on next instance creation.</p>
     * The choice is stored in {@link #PREFERENCE_FILE_NAME} under the key {@link #KEY_OPT_OUT}.
     *
     * @param optOut true to disable reporting
     */
    public void setOptOut(int siteId, boolean optOut) {
        preferences.edit().putBoolean(KEY_OPT_OUT + siteId, optOut).apply();
    }

    public boolean isOptOut(int siteId) {
        return preferences.getBoolean(KEY_OPT_OUT + siteId, false);
    }

    /**
     * @param siteId
     * @return
     * @see QueryParams#VISITOR_ID
     */
    public String getVisitorId(int siteId) {
        String stored = preferences.getString(KEY_VISITOR_ID + siteId, null);
        if (stored == null) {

            // Generate and store a new ID
            String generated = makeRandomVisitorId();
            preferences.edit().putString(KEY_VISITOR_ID + siteId, generated).apply();
            return generated;
        } else {
            return stored;
        }

    }

    public void setUserId(int siteId, String userId) {
        preferences.edit().putString(KEY_USER_ID + siteId, userId).apply();
    }

    @Nullable
    public String getUserId(int siteId) {
        return preferences.getString(KEY_USER_ID + siteId, null);
    }

    public int incrementVisitCount(int siteId) {
        int visitCount = preferences.getInt(KEY_VISIT_COUNT + siteId, 0) + 1;
        preferences.edit().putInt(KEY_VISIT_COUNT + siteId, visitCount).apply();
        return visitCount;
    }

    public long getTimeOfFirstVisit(int siteId) {
        long time = preferences.getLong(KEY_FIRST_VISIT + siteId, -1);
        if (time == -1) {
            time = System.currentTimeMillis() / 1000;
            preferences.edit().putLong(KEY_FIRST_VISIT + siteId, time).apply();
        }
        return time;
    }

    public long resetLastVisitTimestamp(int siteId) {
        long last = preferences.getLong(KEY_LAST_VISIT + siteId, -1);
        preferences.edit().putLong(KEY_LAST_VISIT + siteId, System.currentTimeMillis() / 1000).apply();
        return last;
    }

    public boolean isTracked(int siteId, String packageName, int versionCode) {
        Set<String> tracked = preferences.getStringSet(KEY_TRACKED_INSTALLS + siteId, Collections.<String>emptySet());
        return tracked.contains(packageName + ":" + versionCode);
    }

    public void trackInstall(int siteId, String packageName, int versionCode) {
        Set<String> tracked = preferences.getStringSet(KEY_TRACKED_INSTALLS + siteId, Collections.<String>emptySet());
        tracked.add(packageName + ":" + versionCode);
        preferences.edit().putStringSet(KEY_TRACKED_INSTALLS + siteId, tracked).apply();
    }
}
