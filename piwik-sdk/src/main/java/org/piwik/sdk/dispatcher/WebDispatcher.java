/*
 * Android SDK for Piwik
 *
 * @link https://github.com/piwik/piwik-android-sdk
 * @license https://github.com/piwik/piwik-sdk-android/blob/master/LICENSE BSD-3 Clause
 */

package org.piwik.sdk.dispatcher;

import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.piwik.sdk.Piwik;
import org.piwik.sdk.TrackerBulkURLWrapper;
import org.piwik.sdk.storage.StorageEngine;
import org.piwik.sdk.tools.Logy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends json POST request to tracking url http://piwik.example.com/piwik.php with body
 * <p/>
 * {
 * "requests": [
 * "?idsite=1&url=http://example.org&action_name=Test bulk log Pageview&rec=1",
 * "?idsite=1&url=http://example.net/test.htm&action_name=Another bul k page view&rec=1"
 * ],
 * "token_auth": "33dc3f2536d3025974cccb4b4d2d98f4"
 * }
 */
public class WebDispatcher implements Dispatcher {
    private static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "Dispatcher";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final HttpUrl serverUrl;
    private final String authToken;
    private final OkHttpClient httpClient;

    /**
     * @param serverUrl for this {@linkplain Piwik} instance, for example, http://your-piwik-domain.tld/piwik.php
     */
    public WebDispatcher(HttpUrl serverUrl, String authToken, OkHttpClient httpClient) {
        this.authToken = authToken;
        this.httpClient = httpClient;

        // Validate the serverUrl is a valid Piwik url
        List<String> pathSegments = serverUrl.pathSegments();
        String lastPathSegment = pathSegments.get(pathSegments.size() - 1);
        if ("piwik.php".equals(lastPathSegment) || "piwik-proxy.php".equals(lastPathSegment)) {
            this.serverUrl = serverUrl;
        } else {
            this.serverUrl = serverUrl.newBuilder().addPathSegment("piwik.php").build();
        }
    }

    /**
     * http://stackoverflow.com/q/4737841
     *
     * @param param raw data
     * @return encoded string
     */
    public static String urlEncodeUTF8(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            Logy.w(LOGGER_TAG, String.format("Cannot encode %s", param), e);
            return "";
        } catch (NullPointerException e) {
            return "";
        }
    }

    /**
     * For bulk tracking purposes
     *
     * @param map query map
     * @return String "?idsite=1&url=http://example.org&action_name=Test bulk log view&rec=1"
     */
    public static String urlEncodeUTF8(Map<String, String> map) {
        StringBuilder sb = new StringBuilder(100);
        sb.append('?');
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(urlEncodeUTF8(entry.getKey()));
            sb.append('=');
            sb.append(urlEncodeUTF8(entry.getValue()));
            sb.append('&');
        }

        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public void dispatch(StorageEngine storageEngine) {
        int count = 0;
        List<String> availableEvents = storageEngine.get();
        TrackerBulkURLWrapper wrapper = new TrackerBulkURLWrapper(serverUrl, availableEvents, authToken);
        Iterator<TrackerBulkURLWrapper.Page> pageIterator = wrapper.iterator();
        while (pageIterator.hasNext()) {
            TrackerBulkURLWrapper.Page page = pageIterator.next();

            // use doGET when only event on current page
            if (page.elementsCount() > 1) {
                JSONObject eventData = wrapper.getEvents(page);
                if (eventData == null) {
                    continue;
                }
                if (dispatch(new Packet(wrapper.getServerUrl(), eventData))) {
                    count += page.elementsCount();
                }
                storageEngine.remove(wrapper.getEventsRaw(page));
            } else {
                HttpUrl targetURL = wrapper.getEventUrl(page);
                if (targetURL == null) {
                    continue;
                }
                if (dispatch(new Packet(targetURL))) {
                    count += 1;
                    storageEngine.remove(wrapper.getEventsRaw(page));
                }
            }
        }
        Logy.d(LOGGER_TAG, "Dispatched " + count + " events.");
    }

    private boolean dispatch(@NonNull Packet packet) {
        // Some error checking
        if (packet.getTargetURL() == null)
            return false;
        if (packet.getJSONObject() != null && packet.getJSONObject().length() == 0)
            return false;

        Request.Builder requestBuilder = new Request.Builder().url(packet.getTargetURL());

        // IF there is json data we want to do a post
        if (packet.getJSONObject() != null) {

            // POST
            RequestBody body = RequestBody.create(JSON, packet.getJSONObject().toString());
            requestBuilder = requestBuilder.post(body);
        }

        Request request = requestBuilder.build();
        Response response;
        try {
            response = httpClient.newCall(request).execute();
            int statusCode = response.code();
            Logy.d(LOGGER_TAG, String.format("status code %s", statusCode));
            return statusCode == 200 || statusCode == 204;
        } catch (IOException e) {
            Logy.w(LOGGER_TAG, "Cannot send request", e);
            return false;
        }
    }

    public int getTimeOut() {
        return httpClient.connectTimeoutMillis();
    }
}
