package com.pkware.piwik.sdk;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Query parameters supported by the tracking HTTP API.
 * See <a href="http://developer.piwik.org/api-reference/tracking-api">Tracking HTTP API</a>
 */
public class QueryParams {

    /**
     * The ID of the website we're tracking a visit/action for.
     * <p/>
     * (required)
     */
    public static final String SITE_ID = "idsite";

    //Required parameters
    /**
     * Required for tracking, must be set to one, eg, &rec=1.
     * <p/>
     * (required)
     */
    public static final String RECORD = "rec";
    /**
     * The full URL for the current action.
     * <p/>
     * (required)
     */
    public static final String URL_PATH = "url";
    /**
     * The title of the action being tracked.<p>
     * It is possible to <a href="http://piwik.org/faq/how-to/#faq_62">use slashes / to set one or several categories for this action.</a>
     * For example, Help / Feedback will create the Action Feedback in the category Help.
     * <p/>
     * (recommended)
     */
    public static final String ACTION_NAME = "action_name";

    //Recommended parameters
    /**
     * The unique visitor ID, must be a 16 characters hexadecimal string.<p>
     * Every unique visitor must be assigned a different ID and this ID must not change after it is assigned.
     * If this value is not set Piwik will still track visits, but the unique visitors metric might be less accurate.
     * <p/>
     * (recommended)
     */
    public static final String VISITOR_ID = "_id";
    /**
     * Meant to hold a random value that is generated before each request.<p>
     * Using it helps avoid the tracking request being cached by the browser or a proxy.
     * <p/>
     * (recommended)
     */
    public static final String RANDOM_NUMBER = "rand";
    /**
     * The parameter &apiv=1 defines the api version to use (currently always set to 1)
     * <p/>
     * (recommended)
     */
    public static final String API_VERSION = "apiv";
    /**
     * The full HTTP Referrer URL.<p>
     * This value is used to determine how someone got to your website (ie, through a website, search engine or campaign).
     */
    public static final String REFERRER = "urlref";


    // Optional User info
    /**
     * Visit scope <a href="http://piwik.org/docs/custom-variables/">custom variables</a>.<p>
     * This is a JSON encoded string of the custom variable array.
     */
    public static final String VISIT_SCOPE_CUSTOM_VARIABLES = "_cvar";
    /**
     * The current count of visits for this visitor.<p>
     * To set this value correctly, it would be required to store the value for each visitor in your application (using sessions or persisting in a database).
     * Then you would manually increment the counts by one on each new visit or "session", depending on how you choose to define a visit.
     * This value is used to populate the report Visitors > Engagement > Visits by visit number.
     */
    public static final String TOTAL_NUMBER_OF_VISITS = "_idvc";
    /**
     * The UNIX timestamp of this visitor's previous visit (seconds since Jan 01 1970. (UTC)).<p>
     * This parameter is used to populate the report Visitors > Engagement > Visits by days since last visit.
     */
    public static final String PREVIOUS_VISIT_TIMESTAMP = "_viewts";
    /**
     * The UNIX timestamp of this visitor's first visit (seconds since Jan 01 1970. (UTC)).<p>
     * This could be set to the date where the user first started using your software/app, or when he/she created an account.
     * This parameter is used to populate the Goals > Days to Conversion report.
     */
    public static final String FIRST_VISIT_TIMESTAMP = "_idts";
    /**
     * The Campaign name (see <a href="http://piwik.org/docs/tracking-campaigns/">Tracking Campaigns</a>).<p>
     * Used to populate the Referrers > Campaigns report.
     * Note: this parameter will only be used for the first pageview of a visit.
     */
    public static final String CAMPAIGN_NAME = "_rcn";
    /**
     * The Campaign Keyword (see <a href="http://piwik.org/docs/tracking-campaigns/">Tracking Campaigns</a>).<p>
     * Used to populate the Referrers > Campaigns report (clicking on a campaign loads all keywords for this campaign).
     * Note: this parameter will only be used for the first pageview of a visit.
     */
    public static final String CAMPAIGN_KEYWORD = "_rck";
    /**
     * The resolution of the device the visitor is using, eg 1280x1024.
     */
    public static final String SCREEN_RESOLUTION = "res";
    /**
     * The current hour (local time).
     */
    public static final String HOURS = "h";
    /**
     * The current minute (local time).
     */
    public static final String MINUTES = "m";
    /**
     * The current second (local time).
     */
    public static final String SECONDS = "s";
    /**
     * An override value for the User-Agent HTTP header field.<p>
     * The user agent is used to detect the operating system and browser used.
     */
    public static final String USER_AGENT = "ua";
    /**
     * An override value for the Accept-Language HTTP header field.<p>
     * This value is used to detect the visitor's country if <a href="http://piwik.org/faq/troubleshooting/#faq_65">GeoIP</a> is not enabled.
     */
    public static final String LANGUAGE = "lang";
    /**
     * Defines the User ID for this request.<p>
     * User ID is any non empty unique string identifying the user (such as an email address or a username).
     * To access this value, users must be logged-in in your system so you can fetch this user ID from your system, and pass it to Piwik.
     * The User ID appears in the visitor log, the Visitor profile, and you can Segment reports for one or several User ID (userId segment).
     * When specified, the User ID will be "enforced". This means that if there is no recent visit with this User ID, a new one will be created.
     * If a visit is found in the last 30 minutes with your specified User ID, then the new action will be recorded to this existing visit.
     */
    public static final String USER_ID = "uid";
    /**
     * If set to 1, will force a new visit to be created for this action.
     */
    public static final String SESSION_START = "new_visit";
    /**
     * Page scope <a href="http://piwik.org/docs/custom-variables/">custom variables</a>.
     * This is a JSON encoded string of the custom variable array.
     */
    public static final String SCREEN_SCOPE_CUSTOM_VARIABLES = "cvar";


    // Optional Action info (measure Page view, Outlink, Download, Site search)
    /**
     * An external URL the user has opened.<p>
     * Used for tracking outlink clicks. We recommend to also set the url parameter to this same value.
     */
    public static final String LINK = "link";
    /**
     * URL of a file the user has downloaded.<p>
     * Used for tracking downloads. We recommend to also set the url parameter to this same value.
     */
    public static final String DOWNLOAD = "download";
    /**
     * The Site Search keyword.<p>
     * When specified, the request will not be tracked as a normal pageview but will instead be tracked as a <a href="http://piwik.org/docs/site-search/">Site Search</a> request.
     */
    public static final String SEARCH_KEYWORD = "search";
    /**
     * When {@link #SEARCH_KEYWORD} is specified, you can optionally specify a search category with this parameter.
     */
    public static final String SEARCH_CATEGORY = "search_cat";
    /**
     * When {@link #SEARCH_KEYWORD} is specified, we also recommend to set this to the number of search results.
     */
    public static final String SEARCH_NUMBER_OF_HITS = "search_count";
    /**
     * If specified, the tracking request will trigger a conversion for the goal of the website being tracked with this ID.
     */
    public static final String GOAL_ID = "idgoal";
    /**
     * A monetary value that was generated as revenue by this goal conversion.<p>
     * Only used if {@link #GOAL_ID} is specified in the request.
     */
    public static final String REVENUE = "revenue";
    /**
     * 32 character authorization key used to authenticate the API request.
     *
     * @deprecated due to security concerns.
     */
    @Deprecated
    public static final String AUTHENTICATION_TOKEN = "token_auth";
    /**
     * An override value for the country.<p>
     * Should be set to the two letter country code of the visitor (lowercase; eg fr, de, us.
     * Requires {@link #AUTHENTICATION_TOKEN}.
     */
    public static final String COUNTRY = "country";
    /**
     * An override value for the visitor's latitude, eg 22.456.<p>
     * Requires {@link #AUTHENTICATION_TOKEN}.
     */
    public static final String LATITUDE = "lat";
    /**
     * An override value for the visitor's longitude, eg 22.456.<p>
     * Requires {@link #AUTHENTICATION_TOKEN}.
     */
    public static final String LONGITUDE = "long";
    /**
     * Override for the datetime of the request (normally the current time is used).<p>
     * This can be used to record visits and page views in the past.
     * The expected format is: 2011-04-05 00:11:42 (remember to URL encode the value!).
     * The datetime must be sent in UTC timezone.
     * Note: if you record data in the past, you will need to <a href="http://piwik.org/faq/how-to/#faq_59">force Piwik to re-process reports for the past dates.</a>
     * If you set cdt to a datetime older than four hours then token_auth must be set.
     * If you set cdt with a datetime in the last four hours then you don't need to pass {@link #AUTHENTICATION_TOKEN}.
     */
    public static final String DATETIME_OF_REQUEST = "cdt";
    /**
     * The name of the content. For instance 'Ad Foo Bar'
     *
     * @see <a href="http://piwik.org/docs/content-tracking/">Content Tracking</a>
     */
    public static final String CONTENT_NAME = "c_n";
    /**
     * The actual content piece. For instance the path to an image, video, audio, any text
     *
     * @see <a href="http://piwik.org/docs/content-tracking/">Content Tracking</a>
     */
    public static final String CONTENT_PIECE = "c_p";
    /**
     * The target of the content. For instance the URL of a landing page
     *
     * @see <a href="http://piwik.org/docs/content-tracking/">Content Tracking</a>
     */
    public static final String CONTENT_TARGET = "c_t";
    /**
     * The name of the interaction with the content. For instance a 'click'
     *
     * @see <a href="http://piwik.org/docs/content-tracking/">Content Tracking</a>
     */
    public static final String CONTENT_INTERACTION = "c_i";
    /**
     * The event category. Must not be empty. (eg. Videos, Music, Games...)
     *
     * @see <a href="http://piwik.org/docs/event-tracking/">Event Tracking</a>
     */
    public static final String EVENT_CATEGORY = "e_c";
    /**
     * The event action. Must not be empty. (eg. Play, Pause, Duration, Add Playlist, Downloaded, Clicked...)
     *
     * @see <a href="http://piwik.org/docs/event-tracking/">Event Tracking</a>
     */
    public static final String EVENT_ACTION = "e_a";
    /**
     * The event name. (eg. a Movie name, or Song name, or File name...)
     *
     * @see <a href="http://piwik.org/docs/event-tracking/">Event Tracking</a>
     */
    public static final String EVENT_NAME = "e_n";
    /**
     * The event value. Must be a float or integer value (numeric), not a string.
     *
     * @see <a href="http://piwik.org/docs/event-tracking/">Event Tracking</a>
     */
    public static final String EVENT_VALUE = "e_v";
    /**
     * Items in your cart or order for ecommerce tracking
     */
    public static final String ECOMMERCE_ITEMS = "ec_items";

    // Ecommerce parameters
    /**
     * The amount of tax paid for the order
     */
    public static final String TAX = "ec_tx";
    /**
     * The unique identifier for the order
     */
    public static final String ORDER_ID = "ec_id";
    /**
     * The amount of shipping paid on the order
     */
    public static final String SHIPPING = "ec_sh";
    /**
     * The amount of the discount on the order
     */
    public static final String DISCOUNT = "ec_dt";
    /**
     * The sub total amount of the order
     */
    public static final String SUBTOTAL = "ec_st";
    /**
     * If set to 0 (send_image=0) Piwik will respond with a HTTP 204 response code instead of a GIF image.<p>
     * This improves performance and can fix errors if images are not allowed to be obtained directly (eg Chrome Apps). Available since Piwik 2.10.0
     */
    public static final String SEND_IMAGE = "send_image";

    // Other parameters
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SITE_ID,
            RECORD,
            URL_PATH,
            ACTION_NAME,
            VISITOR_ID,
            RANDOM_NUMBER,
            API_VERSION,
            REFERRER,
            VISIT_SCOPE_CUSTOM_VARIABLES,
            TOTAL_NUMBER_OF_VISITS,
            PREVIOUS_VISIT_TIMESTAMP,
            FIRST_VISIT_TIMESTAMP,
            CAMPAIGN_NAME,
            CAMPAIGN_KEYWORD,
            SCREEN_RESOLUTION,
            HOURS,
            MINUTES,
            SECONDS,
            USER_AGENT,
            LANGUAGE,
            USER_ID,
            SESSION_START,
            SCREEN_SCOPE_CUSTOM_VARIABLES,
            LINK,
            DOWNLOAD,
            SEARCH_KEYWORD,
            SEARCH_CATEGORY,
            SEARCH_NUMBER_OF_HITS,
            GOAL_ID,
            REVENUE,
            AUTHENTICATION_TOKEN,
            COUNTRY,
            LATITUDE,
            LONGITUDE,
            DATETIME_OF_REQUEST,
            CONTENT_NAME,
            CONTENT_PIECE,
            CONTENT_TARGET,
            CONTENT_INTERACTION,
            EVENT_CATEGORY,
            EVENT_ACTION,
            EVENT_NAME,
            EVENT_VALUE,
            ECOMMERCE_ITEMS,
            TAX,
            ORDER_ID,
            SHIPPING,
            DISCOUNT,
            SUBTOTAL,
            SEND_IMAGE
    })
    public @interface QueryParam {
    }
}
