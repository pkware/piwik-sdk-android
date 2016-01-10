/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.piwik.sdk.ecommerce.EcommerceItems;
import org.piwik.sdk.storage.StorageEngine;
import org.piwik.sdk.tools.CurrencyFormatter;
import org.piwik.sdk.tools.DeviceHelper;
import org.piwik.sdk.tools.Logy;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import okio.Okio;

/**
 * Main tracking class
 * This class is threadsafe.
 */
// FIXME hashcode && equals
public class Tracker {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Tracker";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.ENGLISH);

    // Piwik default parameter values
    private static final String DEFAULT_UNKNOWN_VALUE = "unknown";
    private static final String DEFAULT_TRUE_VALUE = "1";
    private static final String DEFAULT_RECORD_VALUE = DEFAULT_TRUE_VALUE;
    private static final String DEFAULT_API_VERSION_VALUE = "1";

    /**
     * The ID of the website we're tracking a visit/action for.
     */
    private final int siteId;
    private final Piwik mPiwik;
    private final Object mSessionLock = new Object();
    private final CustomVariables mVisitCustomVariable = new CustomVariables();
    private final StorageEngine storageEngine;
    private final Settings settings;
    private final Random mRandomAntiCachingValue = new Random(new Date().getTime());
    private final TrackMe mDefaultTrackMe = new TrackMe();

    private boolean isOptOut;
    private String mApplicationDomain;
    private long mSessionTimeout = 30 * 60 * 1000;
    private long mSessionStartTime;

    /**
     * Use {@link Piwik#newTracker(int)} to create new trackers
     *
     * @param storageEngine (required) Used to store events
     * @param siteId        (required) id of site
     * @param piwik         piwik object used to gain access to application params such as name, resolution or lang
     * @param settings
     */
    protected Tracker(@NonNull StorageEngine storageEngine, int siteId, @NonNull Piwik piwik, Settings settings) {
        mPiwik = piwik;
        this.siteId = siteId;
        this.storageEngine = storageEngine;
        this.settings = settings;
        isOptOut = settings.isOptOut(siteId);

        mDefaultTrackMe.set(QueryParams.USER_ID, settings.getUserId(siteId));

        mDefaultTrackMe.set(QueryParams.SESSION_START, DEFAULT_TRUE_VALUE);

        String resolution = DEFAULT_UNKNOWN_VALUE;
        int[] res = DeviceHelper.getResolution(mPiwik.getContext());
        if (res != null)
            resolution = String.format("%sx%s", res[0], res[1]);
        mDefaultTrackMe.set(QueryParams.SCREEN_RESOLUTION, resolution);

        mDefaultTrackMe.set(QueryParams.USER_AGENT, DeviceHelper.getUserAgent());
        mDefaultTrackMe.set(QueryParams.LANGUAGE, DeviceHelper.getUserLanguage());
        mDefaultTrackMe.set(QueryParams.COUNTRY, DeviceHelper.getUserCountry());
        mDefaultTrackMe.set(QueryParams.VISITOR_ID, settings.getVisitorId(siteId));
    }

    public void setOptOut(boolean optOut) {
        isOptOut = optOut;
        settings.setOptOut(optOut, siteId);
    }

    /**
     * Piwik will use the content of this object to fill in missing values before any transmission.
     * While you can modify it's values, you can also just set them in your {@link TrackMe} object as already set values will not be overwritten.
     *
     * @return the default TrackMe object
     */
    public TrackMe getDefaultTrackMe() {
        return mDefaultTrackMe;
    }

    public void startNewSession() {
        synchronized (mSessionLock) {
            mSessionStartTime = 0;
        }
    }

    /**
     * @return {@code true} if the previous session expired
     */
    private boolean tryNewSession() {
        synchronized (mSessionLock) {
            boolean expired = System.currentTimeMillis() - mSessionStartTime > mSessionTimeout;
            // Update the session timer
            mSessionStartTime = System.currentTimeMillis();
            return expired;
        }
    }

    /**
     * Default is 30min (30*60*1000).
     *
     * @return session timeout value in milliseconds
     */
    public long getSessionTimeout() {
        return mSessionTimeout;
    }

    public void setSessionTimeout(int milliseconds) {
        synchronized (mSessionLock) {
            mSessionTimeout = milliseconds;
        }
    }

    /**
     * @param userId passing null will delete the current user-id.
     * @see QueryParams#USER_ID
     * @see <a href="http://piwik.org/docs/user-id/">User ID Documentation</a>
     */
    public Tracker setUserId(String userId) {
        mDefaultTrackMe.set(QueryParams.USER_ID, userId);
        settings.setUserId(siteId, userId);
        return this;
    }

    protected String getApplicationDomain() {
        return mApplicationDomain != null ? mApplicationDomain : mPiwik.getApplicationDomain();
    }

    /**
     * Domain used to build required parameter url (http://developer.piwik.org/api-reference/tracking-api)
     * If domain wasn't set {@link Application#getPackageName()} method will be used
     *
     * @param domain your-domain.com
     */
    public Tracker setApplicationDomain(String domain) {
        mApplicationDomain = domain;
        return this;
    }

    /**
     * Tracking methods
     *
     * @param path required tracking param, for example: "/user/settings/billing"
     */
    public Tracker trackScreenView(String path) {
        return trackScreenView(path, null);
    }

    /**
     * @param trackMe the track me objects to use
     * @param path    required tracking param, for example: "/user/settings/billing"
     * @return this tracker
     */
    public Tracker trackScreenView(TrackMe trackMe, String path) {
        return trackScreenView(trackMe, path, null);
    }

    /**
     * @param path  for example: "/user/settings/billing"
     * @param title string The title of the action being tracked. It is possible to use
     *              slashes / to set one or several categories for this action.
     *              For example, Help / Feedback will create the Action Feedback in the category Help.
     * @return this tracker
     */
    public Tracker trackScreenView(String path, String title) {
        return trackScreenView(new TrackMe(), path, title);
    }

    /**
     * @param trackMe the track me objects to use
     * @param path    for example: "/user/settings/billing"
     * @param title   string The title of the action being tracked. It is possible to use
     *                slashes / to set one or several categories for this action.
     *                For example, Help / Feedback will create the Action Feedback in the category Help.
     * @return this tracker
     */
    public Tracker trackScreenView(TrackMe trackMe, String path, String title) {
        if (path == null)
            return this;
        trackMe.set(QueryParams.URL_PATH, path);
        trackMe.set(QueryParams.ACTION_NAME, title);
        return track(trackMe);
    }


    public Tracker trackEvent(String category, String action) {
        return track(new TrackMe()
                .set(QueryParams.EVENT_CATEGORY, category)
                .set(QueryParams.EVENT_ACTION, action));
    }

    public Tracker trackEvent(String category, String action, String label) {
        return track(new TrackMe()
                .set(QueryParams.EVENT_CATEGORY, category)
                .set(QueryParams.EVENT_ACTION, action)
                .set(QueryParams.EVENT_NAME, label));
    }

    /**
     * Events are a useful way to collect data about a user's interaction with interactive components of your app,
     * like button presses or the use of a particular item in a game.
     *
     * @param category (required) – this String defines the event category.
     *                 You might define event categories based on the class of user actions,
     *                 like clicks or gestures or voice commands, or you might define them based upon the
     *                 features available in your application (play, pause, fast forward, etc.).
     * @param action   (required) this String defines the specific event action within the category specified.
     *                 In the example, we are basically saying that the category of the event is user clicks,
     *                 and the action is a button click.
     * @param label    defines a label associated with the event. For example, if you have multiple Button controls on a
     *                 screen, you might use the label to specify the specific View control identifier that was clicked.
     * @param value    defines a numeric value associated with the event. For example, if you were tracking "Buy"
     *                 button clicks, you might log the number of items being purchased, or their total cost.
     */
    public Tracker trackEvent(String category, String action, String label, float value) {
        return track(new TrackMe()
                .set(QueryParams.EVENT_CATEGORY, category)
                .set(QueryParams.EVENT_ACTION, action)
                .set(QueryParams.EVENT_NAME, label)
                .set(QueryParams.EVENT_VALUE, value));

    }

    /**
     * By default, Goals in Piwik are defined as "matching" parts of the screen path or screen title.
     * In this case a conversion is logged automatically. In some situations, you may want to trigger
     * a conversion manually on other types of actions, for example:
     * when a user submits a form
     * when a user has stayed more than a given amount of time on the page
     * when a user does some interaction in your Android application
     *
     * @param idGoal id of goal as defined in piwik goal settings
     */
    public Tracker trackGoal(int idGoal) {
        if (idGoal < 0)
            return this;
        return track(new TrackMe().set(QueryParams.GOAL_ID, idGoal));
    }

    /**
     * Tracking request will trigger a conversion for the goal of the website being tracked with this ID
     *
     * @param idGoal  id of goal as defined in piwik goal settings
     * @param revenue a monetary value that was generated as revenue by this goal conversion.
     */
    public Tracker trackGoal(int idGoal, float revenue) {
        if (idGoal < 0)
            return this;
        return track(new TrackMe()
                .set(QueryParams.GOAL_ID, idGoal)
                .set(QueryParams.REVENUE, revenue));
    }

    /**
     * Tracks an  <a href="http://piwik.org/faq/new-to-piwik/faq_71/">Outlink</a>
     *
     * @param url HTTPS, HTTP and FTP are valid
     * @return this Tracker for chaining
     */
    public Tracker trackOutlink(URL url) {
        // TODO Who enforces which protocols are acceptable? Server or client? If client, can we adjust for android and do contet URIs?
        if (url.getProtocol().equals("http") || url.getProtocol().equals("https") || url.getProtocol().equals("ftp")) {
            return track(new TrackMe()
                    .set(QueryParams.LINK, url.toExternalForm())
                    .set(QueryParams.URL_PATH, url.toExternalForm()));
        }
        return this;
    }

    /**
     * Fires a download for this app once per update.
     * The install will be tracked as:<p/>
     * 'http://packageName:versionCode/installerPackagename'
     * <p/>
     * Also see {@link #trackNewAppDownload(android.content.Context, org.piwik.sdk.Tracker.ExtraIdentifier)}
     *
     * @return this tracker again for chaining
     */
    public Tracker trackAppDownload() {
        return trackAppDownload(mPiwik.getContext(), ExtraIdentifier.INSTALLER_PACKAGENAME);
    }

    /**
     * Fires a download for an arbitrary app once per update.
     *
     * @param app   the app to track
     * @param extra {@link org.piwik.sdk.Tracker.ExtraIdentifier#APK_CHECKSUM} or {@link org.piwik.sdk.Tracker.ExtraIdentifier#INSTALLER_PACKAGENAME}
     * @return this tracker for chaining
     */
    public Tracker trackAppDownload(Context app, ExtraIdentifier extra) {
        try {
            PackageInfo pkgInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            if (!settings.isTracked(siteId, pkgInfo.packageName, pkgInfo.versionCode)) {
                trackNewAppDownload(app, extra);
                settings.trackInstall(siteId, pkgInfo.packageName, pkgInfo.versionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Track a download for a specific app
     * <p/>
     * Resulting download url:<p/>
     * Case {@link org.piwik.sdk.Tracker.ExtraIdentifier#APK_CHECKSUM}: http://packageName:versionCode/apk-md5-checksum <p/>
     * Case {@link org.piwik.sdk.Tracker.ExtraIdentifier#INSTALLER_PACKAGENAME}: http://packageName:versionCode/installerPackageName <p/>
     * Note: Usually the installer-packagename is something like "com.android.vending" (Google Play),
     * but users can modify this value, don't be surprised by some random values.
     *
     * @param app   the app you want to fire a download event for
     * @param extra {@link org.piwik.sdk.Tracker.ExtraIdentifier#APK_CHECKSUM} or {@link org.piwik.sdk.Tracker.ExtraIdentifier#INSTALLER_PACKAGENAME}
     * @return this tracker again, so you can chain calls
     */
    public Tracker trackNewAppDownload(Context app, ExtraIdentifier extra) {
        StringBuilder installationIdentifier = new StringBuilder();
        try {
            String pkg = app.getPackageName();
            installationIdentifier.append("http://").append(pkg); // Identifies the app

            PackageManager packMan = app.getPackageManager();
            PackageInfo pkgInfo = packMan.getPackageInfo(pkg, 0);
            installationIdentifier.append(":").append(pkgInfo.versionCode);

            // Usual USEFUL values of this field will be: "com.android.vending" or "com.android.browser", i.e. app packagenames.
            // This is not guaranteed, values can also look like: app_process /system/bin com.android.commands.pm.Pm install -r /storage/sdcard0/...
            String installerPackageName = packMan.getInstallerPackageName(pkg);
            if (installerPackageName == null || installerPackageName.length() > 200)
                installerPackageName = DEFAULT_UNKNOWN_VALUE;

            String extraIdentifier = DEFAULT_UNKNOWN_VALUE;
            if (extra == ExtraIdentifier.APK_CHECKSUM) {
                ApplicationInfo appInfo = packMan.getApplicationInfo(pkg, 0);
                if (appInfo.sourceDir != null) {
                    try {
                        extraIdentifier = Okio.buffer(Okio.source(new File(appInfo.sourceDir))).readByteString().md5().utf8();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (extra == ExtraIdentifier.INSTALLER_PACKAGENAME) {
                extraIdentifier = installerPackageName;
            }
            installationIdentifier.append("/").append(extraIdentifier);

            return track(new TrackMe()
                    .set(QueryParams.EVENT_CATEGORY, "Application")
                    .set(QueryParams.EVENT_ACTION, "downloaded")
                    .set(QueryParams.ACTION_NAME, "application/downloaded")
                    .set(QueryParams.URL_PATH, "/application/downloaded")
                    .set(QueryParams.DOWNLOAD, installationIdentifier.toString())
                    .set(QueryParams.REFERRER, installerPackageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return this;
        }
    }

    /**
     * Tracking the impressions
     *
     * @param contentName   The name of the content. For instance 'Ad Foo Bar'
     * @param contentPiece  The actual content. For instance the path to an image, video, audio, any text
     * @param contentTarget (optional) The target of the content. For instance the URL of a landing page.
     */
    public Tracker trackContentImpression(String contentName, String contentPiece, String contentTarget) {
        if (contentName == null || contentName.length() < 1)
            return this;
        return track(new TrackMe()
                .set(QueryParams.CONTENT_NAME, contentName)
                .set(QueryParams.CONTENT_PIECE, contentPiece)
                .set(QueryParams.CONTENT_TARGET, contentTarget));
    }

    /**
     * Tracking the interactions
     *
     * @param interaction   The name of the interaction with the content. For instance a 'click'
     * @param contentName   The name of the content. For instance 'Ad Foo Bar'
     * @param contentPiece  The actual content. For instance the path to an image, video, audio, any text
     * @param contentTarget (optional) The target the content leading to when an interaction occurs. For instance the URL of a landing page.
     */
    public Tracker trackContentInteraction(String interaction, String contentName, String contentPiece, String contentTarget) {
        if (contentName == null || contentName.length() < 1 || interaction == null || interaction.length() < 1)
            return this;
        return track(new TrackMe()
                .set(QueryParams.CONTENT_NAME, contentName)
                .set(QueryParams.CONTENT_PIECE, contentPiece)
                .set(QueryParams.CONTENT_TARGET, contentTarget)
                .set(QueryParams.CONTENT_INTERACTION, interaction));
    }

    /**
     * Tracks a shopping cart. Call this javascript function every time a user is adding, updating
     * or deleting a product from the cart.
     *
     * @param grandTotal total value of items in cart
     * @param items      (optional) the items included in the cart
     */
    public void trackEcommerceCartUpdate(int grandTotal, @Nullable EcommerceItems items) {
        if (items == null) {
            items = new EcommerceItems();
        }

        track(new TrackMe()
                .set(QueryParams.GOAL_ID, 0)
                .set(QueryParams.REVENUE, CurrencyFormatter.priceString(grandTotal))
                .set(QueryParams.ECOMMERCE_ITEMS, items.toJson()));
    }

    /**
     * Tracks an Ecommerce order, including any ecommerce item previously added to the order.  All
     * monetary values should be passed as an integer number of cents (or the smallest integer unit
     * for your currency)
     *
     * @param orderId    (required) A unique string identifying the order
     * @param grandTotal (required) total amount of the order, in cents
     * @param subTotal   (optional) the subTotal for the order, in cents
     * @param tax        (optional) the tax for the order, in cents
     * @param shipping   (optional) the shipping for the order, in cents
     * @param discount   (optional) the discount for the order, in cents
     * @param items      (optional) the items included in the order
     */
    public void trackEcommerceOrder(String orderId, Integer grandTotal, @Nullable Integer subTotal, @Nullable Integer tax, @Nullable Integer shipping, @Nullable Integer discount, @Nullable EcommerceItems items) {
        if (items == null) {
            items = new EcommerceItems();
        }

        TrackMe trackMe = new TrackMe()
                .set(QueryParams.GOAL_ID, 0)
                .set(QueryParams.ORDER_ID, orderId)
                .set(QueryParams.REVENUE, CurrencyFormatter.priceString(grandTotal))
                .set(QueryParams.ECOMMERCE_ITEMS, items.toJson());

        if (subTotal != null) {
            trackMe.set(QueryParams.SUBTOTAL, CurrencyFormatter.priceString(subTotal));
        }

        if (tax != null) {
            trackMe.set(QueryParams.TAX, CurrencyFormatter.priceString(tax));
        }

        if (shipping != null) {
            trackMe.set(QueryParams.SHIPPING, CurrencyFormatter.priceString(shipping));
        }

        if (discount != null) {
            trackMe.set(QueryParams.DISCOUNT, CurrencyFormatter.priceString(discount));
        }
        track(trackMe);
    }

    /**
     * Caught exceptions are errors in your app for which you've defined exception handling code,
     * such as the occasional timeout of a network connection during a request for data.
     * <p/>
     * This is just a different way to define an event.
     * Keep in mind Piwik is not a crash tracker, use this sparingly.
     * <p/>
     * For this to be useful you should ensure that proguard does not remove all classnames and line numbers.
     * Also note that if this is used across different app versions and obfuscation is used, the same exception might be mapped to different obfuscated names by proguard.
     * This would mean the same exception (event) is tracked as different events by Piwik.
     *
     * @param ex          exception instance
     * @param description exception message
     * @param isFatal     true if it's fatal exception
     */
    public void trackException(Throwable ex, String description, boolean isFatal) {
        String className;
        try {
            StackTraceElement trace = ex.getStackTrace()[0];
            className = trace.getClassName() + "/" + trace.getMethodName() + ":" + trace.getLineNumber();
        } catch (Exception e) {
            Logy.w(Tracker.LOGGER_TAG, "Couldn't get stack info", e);
            className = ex.getClass().getName();
        }
        String actionName = "exception/" + (isFatal ? "fatal/" : "") + (className + "/") + description;
        track(new TrackMe()
                .set(QueryParams.ACTION_NAME, actionName)
                .set(QueryParams.EVENT_CATEGORY, "Exception")
                .set(QueryParams.EVENT_ACTION, className)
                .set(QueryParams.EVENT_NAME, description)
                .set(QueryParams.EVENT_VALUE, isFatal ? 1 : 0));
    }

    /**
     * There parameters are only interesting for the very first query.
     */
    private void injectInitialParams(TrackMe trackMe) {
        long firstVisitTime;
        int visitCount;
        long previousVisit;

        // Protected against Trackers on other threads trying to do the same thing.
        // This works because they would use the same preference object.
        synchronized (settings) {
            visitCount = settings.incrementVisitCount(siteId);
            firstVisitTime = settings.getTimeOfFirstVisit(siteId);
            previousVisit = settings.resetLastVisitTimestamp(siteId);
        }

        // trySet because the developer could have modded these after creating the Tracker
        mDefaultTrackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, firstVisitTime);
        mDefaultTrackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, visitCount);
        if (previousVisit != -1)
            mDefaultTrackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, previousVisit);

        trackMe.trySet(QueryParams.SESSION_START, mDefaultTrackMe.get(QueryParams.SESSION_START));
        trackMe.trySet(QueryParams.SCREEN_RESOLUTION, mDefaultTrackMe.get(QueryParams.SCREEN_RESOLUTION));
        trackMe.trySet(QueryParams.USER_AGENT, mDefaultTrackMe.get(QueryParams.USER_AGENT));
        trackMe.trySet(QueryParams.LANGUAGE, mDefaultTrackMe.get(QueryParams.LANGUAGE));
        trackMe.trySet(QueryParams.COUNTRY, mDefaultTrackMe.get(QueryParams.COUNTRY));
        trackMe.trySet(QueryParams.FIRST_VISIT_TIMESTAMP, mDefaultTrackMe.get(QueryParams.FIRST_VISIT_TIMESTAMP));
        trackMe.trySet(QueryParams.TOTAL_NUMBER_OF_VISITS, mDefaultTrackMe.get(QueryParams.TOTAL_NUMBER_OF_VISITS));
        trackMe.trySet(QueryParams.PREVIOUS_VISIT_TIMESTAMP, mDefaultTrackMe.get(QueryParams.PREVIOUS_VISIT_TIMESTAMP));
    }

    /**
     * These parameters are required for all queries.
     */
    private void injectBaseParams(TrackMe trackMe) {
        trackMe.trySet(QueryParams.SITE_ID, siteId);
        trackMe.trySet(QueryParams.RECORD, DEFAULT_RECORD_VALUE);
        trackMe.trySet(QueryParams.API_VERSION, DEFAULT_API_VERSION_VALUE);
        trackMe.trySet(QueryParams.RANDOM_NUMBER, mRandomAntiCachingValue.nextInt(100000));
        trackMe.trySet(QueryParams.DATETIME_OF_REQUEST, dateFormat.format(new Date()));
        trackMe.trySet(QueryParams.SEND_IMAGE, "0");

        trackMe.trySet(QueryParams.VISITOR_ID, mDefaultTrackMe.get(QueryParams.VISITOR_ID));
        trackMe.trySet(QueryParams.USER_ID, mDefaultTrackMe.get(QueryParams.USER_ID));

        trackMe.trySet(QueryParams.VISIT_SCOPE_CUSTOM_VARIABLES, mVisitCustomVariable.toString());

        String urlPath = trackMe.get(QueryParams.URL_PATH);
        if (urlPath == null) {
            urlPath = getApplicationBaseURL() + "/";
        } else if (urlPath.startsWith("/")) {
            urlPath = getApplicationBaseURL() + urlPath;
        } else if (urlPath.startsWith("http://") || urlPath.startsWith("https://") || urlPath.startsWith("ftp://")) {
            // URL is fine as it is
        } else if (!urlPath.startsWith("/")) {
            urlPath = getApplicationBaseURL() + "/" + urlPath;
        }
        trackMe.set(QueryParams.URL_PATH, urlPath);
    }

    public Tracker track(TrackMe trackMe) {
        boolean newSession;
        synchronized (mSessionLock) {
            newSession = tryNewSession();
            if (newSession) {
                injectInitialParams(trackMe);
            }
        }

        injectBaseParams(trackMe);
        String event = trackMe.build();
        if (isOptOut) {
            Logy.d(Tracker.LOGGER_TAG, String.format("URL omitted due to opt out: %s", event));
        } else {
            Logy.d(Tracker.LOGGER_TAG, String.format("URL added to the queue: %s", event));
            storageEngine.add(event);
        }

        return this;
    }

    /**
     * A custom variable is a custom name-value pair that you can assign to your users or screen views,
     * and then visualize the reports of how many visits, conversions, etc. for each custom variable.
     * A custom variable is defined by a name — for example,
     * "User status" — and a value – for example, "LoggedIn" or "Anonymous".
     * You can track up to 5 custom variables for each user to your app.
     *
     * @param index this Integer accepts values from 1 to 5.
     *              A given custom variable name must always be stored in the same "index" per session.
     *              For example, if you choose to store the variable name = "Gender" in
     *              index = 1 and you record another custom variable in index = 1, then the
     *              "Gender" variable will be deleted and replaced with the new custom variable stored in index 1.
     * @param name  String defines the name of a specific Custom Variable such as "User type".
     * @param value String defines the value of a specific Custom Variable such as "Customer".
     */
    public Tracker setVisitCustomVariable(int index, String name, String value) {
        mVisitCustomVariable.put(index, name, value);
        return this;
    }

    protected String getApplicationBaseURL() {
        return String.format("http://%s", getApplicationDomain());
    }

    public enum ExtraIdentifier {
        APK_CHECKSUM, INSTALLER_PACKAGENAME
    }
}

