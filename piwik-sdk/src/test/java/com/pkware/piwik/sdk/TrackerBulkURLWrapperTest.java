package com.pkware.piwik.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;


@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TrackerBulkURLWrapperTest {

    private TrackerBulkURLWrapper createWrapper(String url, String... events) throws MalformedURLException {
        if (url == null) {
            url = "http://example.com/";
        }
        URL _url = new URL(url);

        return new TrackerBulkURLWrapper(_url, Collections.unmodifiableList(Arrays.asList(events)), "test_token");
    }

    @Test
    public void emptyIterator() throws MalformedURLException {
        TrackerBulkURLWrapper wrapper = createWrapper(null);
        assertThat(wrapper.iterator().hasNext()).isFalse();
        assertThat(wrapper.iterator().next()).isNull();
    }

    @Test
    public void pageIterator() throws MalformedURLException {
        TrackerBulkURLWrapper wrapper = createWrapper(null, "test1");
        assertThat(wrapper.iterator().hasNext()).isTrue();
        assertThat(wrapper.iterator().next().elementsCount()).isEqualTo(1);
        assertThat(wrapper.iterator().next()).isNull();
    }

    @Test
    public void page() throws JSONException, MalformedURLException {
        List<String> events = new LinkedList<>();
        for (int i = 0; i < TrackerBulkURLWrapper.getEventsPerPage() * 2; i++) {
            events.add("eve" + i);
        }
        TrackerBulkURLWrapper wrapper = new TrackerBulkURLWrapper(new URL("http://example.com/"), events, null);

        Iterator<TrackerBulkURLWrapper.Page> it = wrapper.iterator();
        assertThat(it.hasNext()).isTrue();
        while (it.hasNext()) {
            TrackerBulkURLWrapper.Page page = it.next();
            assertThat(page.elementsCount()).isEqualTo(TrackerBulkURLWrapper.getEventsPerPage());
            JSONArray requests = wrapper.getEvents(page).getJSONArray("requests");
            assertThat(requests.length()).isEqualTo(TrackerBulkURLWrapper.getEventsPerPage());
            assertThat(requests.get(0).toString()).startsWith("eve");
            assertThat(requests.get(TrackerBulkURLWrapper.getEventsPerPage() - 1).toString().length() >= 4).isTrue();
            assertThat(page.isEmpty()).isFalse();
        }
        assertThat(it.hasNext()).isFalse();
        assertThat(it.next()).isNull();
    }

    @Test
    public void getApiUrl() throws MalformedURLException {
        String url = "http://www.com/java.htm";
        TrackerBulkURLWrapper wrapper = createWrapper(url, "");
        assertThat(wrapper.getServerUrl().toString()).isEqualTo(url);
    }

    @Test
    public void getEvents() throws JSONException, MalformedURLException {
        TrackerBulkURLWrapper wrapper = createWrapper(null, "?one=1", "?two=2");
        TrackerBulkURLWrapper.Page page = wrapper.iterator().next();

        assertThat(wrapper.getEvents(page).getJSONArray("requests").length()).isEqualTo(2);
        assertThat(wrapper.getEvents(page).getJSONArray("requests").get(0)).isEqualTo("?one=1");
        assertThat(wrapper.getEvents(page).getJSONArray("requests").get(1)).isEqualTo("?two=2");
        assertThat(wrapper.getEvents(page).getString("token_auth")).isEqualTo("test_token");
    }

    @Test
    public void getEventsRaw() throws MalformedURLException {
        TrackerBulkURLWrapper wrapper = createWrapper(null, "?one=1", "?two=2");
        TrackerBulkURLWrapper.Page page = wrapper.iterator().next();

        List<String> events = wrapper.getEventsRaw(page);
        assertThat(events).hasSize(2);
    }

    @Test
    public void getEventUrl() throws MalformedURLException {
        List<String> events = new LinkedList<>();
        for (int i = 0; i < TrackerBulkURLWrapper.getEventsPerPage() + 1; i++) {
            events.add("?eve" + i);
        }
        URL url = new URL("http://example.com/");
        TrackerBulkURLWrapper wrapper = new TrackerBulkURLWrapper(url, events, null);
        //skip first page
        wrapper.iterator().next();

        //get second with only element
        TrackerBulkURLWrapper.Page page = wrapper.iterator().next();
        assertThat(page.elementsCount()).isEqualTo(1);
        assertThat(page.isEmpty()).isFalse();
        assertThat(wrapper.getEventUrl(page)).isEqualTo(new URL("http://example.com/?eve20"));
    }
}