package com.pkware.piwik.sdk.dispatcher;

import android.util.Log;

import com.pkware.piwik.sdk.FullEnvTestRunner;
import com.pkware.piwik.sdk.Piwik;
import com.pkware.piwik.sdk.TrackMe;
import com.pkware.piwik.sdk.Tracker;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;

@SuppressWarnings("deprecation")
@Config(emulateSdk = 18, manifest = Config.NONE)
@RunWith(FullEnvTestRunner.class)
public class DispatcherTest {

    public static void checkForMIAs(int expectedEvents, List<String> createdEvents, List<Packet> dryRunOutput) throws Exception {
        int previousEventCount = 0;
        int previousFlatQueryCount = 0;
        List<String> flattenedQueries;
        while (true) {
            Thread.sleep(500);
            flattenedQueries = getFlattenedQueries(new ArrayList<>(dryRunOutput));
            Log.d("checkForMIAs", createdEvents.size() + " events created, " + dryRunOutput.size() + " requests dispatched, containing " + flattenedQueries.size() + " flattened queries");
            if (flattenedQueries.size() == expectedEvents) {
                break;
            } else {
                int currentEventCount = createdEvents.size();
                int currentFlatQueryCount = flattenedQueries.size();
                assertThat(currentEventCount).isNotEqualTo(previousEventCount);
                assertThat(currentFlatQueryCount).isNotEqualTo(previousFlatQueryCount);
                previousEventCount = currentEventCount;
                previousFlatQueryCount = currentFlatQueryCount;
            }
        }

        assertThat(flattenedQueries).hasSize(expectedEvents);
        assertThat(createdEvents).hasSize(expectedEvents);

        // We are done, lets make sure can find all send queries in our dispatched results
        while (!createdEvents.isEmpty()) {
            String query = createdEvents.remove(0);
            assertThat(flattenedQueries.remove(query)).isTrue();
        }
        assertThat(createdEvents).isEmpty();
        assertThat(flattenedQueries).isEmpty();
        Log.d("checkForMIAs", "All send queries are accounted for.");
    }

    public static void launchTestThreads(final Tracker tracker, int threadCount, final int queryCount, final List<String> createdQueries) {
        Log.d("launchTestThreads", "Launching " + threadCount + " threads, " + queryCount + " queries each");
        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < queryCount; j++) {
                            Thread.sleep(new Random().nextInt(20 - 0) + 0);
                            TrackMe trackMe = new TrackMe()
                                    .set(QueryParams.EVENT_ACTION, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_CATEGORY, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_NAME, UUID.randomUUID().toString())
                                    .set(QueryParams.EVENT_VALUE, j);

                            tracker.track(trackMe);
                            createdQueries.add(tracker.getAPIUrl().toString() + trackMe.build());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        assert_().fail();
                    }
                }
            }).start();
        }
        Log.d("launchTestThreads", "All launched.");
    }

    public static List<String> getFlattenedQueries(List<Packet> packets) throws Exception {
        List<String> flattenedQueries = new ArrayList<>();
        for (Packet request : packets) {
            if (request.getJSONObject() != null) {
                JSONArray batchedRequests = request.getJSONObject().getJSONArray("requests");
                for (int json = 0; json < batchedRequests.length(); json++) {
                    String unbatchedRequest = request.getTargetURL().toExternalForm() + batchedRequests.get(json).toString();
                    flattenedQueries.add(unbatchedRequest);
                }
            } else {
                flattenedQueries.add(request.getTargetURL().toExternalForm());
            }
        }
        return flattenedQueries;
    }

    public Tracker createTracker() throws MalformedURLException {
        TestPiwikApplication app = (TestPiwikApplication) Robolectric.application;
        return Piwik.getInstance(Robolectric.application).newTracker(app.getTrackerUrl(), app.getSiteId());
    }

    public Piwik getPiwik() {
        return Piwik.getInstance(Robolectric.application);
    }

    @Before
    public void setup() {
        Piwik.getInstance(Robolectric.application).setDryRun(true);
        Piwik.getInstance(Robolectric.application).setOptOut(false);
        Piwik.getInstance(Robolectric.application).setDebug(false);
    }

    @Test
    public void setTimeout() throws Exception {
        WebDispatcher dispatcher = createTracker().getDispatcher();
        dispatcher.setTimeOut(100);
        assertThat(dispatcher.getTimeOut()).isEqualTo(100);
    }

    @Test
    public void forceDispatchTwice() throws Exception {
        WebDispatcher dispatcher = createTracker().getDispatcher();
        dispatcher.setDispatchInterval(-1);
        dispatcher.setTimeOut(20);
        dispatcher.submit("url");

        assertThat(dispatcher.forceDispatch()).isTrue();
        assertThat(dispatcher.forceDispatch()).isFalse();
    }

    @Test
    public void doPostFailed() throws Exception {
        WebDispatcher dispatcher = createTracker().getDispatcher();
        dispatcher.setTimeOut(1);
        assertThat(dispatcher.dispatch(new Packet(null, null))).isFalse();
        assertThat(dispatcher.dispatch(new Packet(new URL("http://test/?s=^test"), new JSONObject()))).isFalse();
    }

    @Test
    public void doGetFailed() throws Exception {
        WebDispatcher dispatcher = createTracker().getDispatcher();
        dispatcher.setTimeOut(1);
        assertThat(dispatcher.dispatch(new Packet(null))).isFalse();
    }

    @Test
    public void urlEncodeUTF8() throws Exception {
        assertThat(WebDispatcher.urlEncodeUTF8((String) null)).isEmpty();
    }

    @Test
    public void sessionStartRaceCondition() throws Exception {
        for (int i = 0; i < 10; i++) {
            Log.d("RaceConditionTest", (10 - i) + " race-condition tests to go.");
            getPiwik().setDryRun(true);
            final Tracker tracker = createTracker();
            tracker.setDispatchInterval(0);
            final int threadCount = 10;
            final int queryCount = 3;
            final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
            launchTestThreads(tracker, threadCount, queryCount, createdEvents);
            Thread.sleep(500);
            checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
            List<String> output = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            for (String out : output) {
                if (output.indexOf(out) == 0) {
                    assertThat(out.contains("lang")).isTrue();
                    assertThat(out.contains("_idts")).isTrue();
                    assertThat(out.contains("new_visit")).isTrue();
                } else {
                    assertThat(out.contains("lang")).isFalse();
                    assertThat(out.contains("_idts")).isFalse();
                    assertThat(out.contains("new_visit")).isFalse();
                }
            }
        }
    }

    @Test
    public void sessionStartRaceCondition() throws Exception {
        for (int i = 0; i < 10; i++) {
            Log.d("RaceConditionTest", (10 - i) + " race-condition tests to go.");
            getPiwik().setDryRun(true);
            final Tracker tracker = createTracker();
            tracker.setDispatchInterval(0);
            final int threadCount = 10;
            final int queryCount = 3;
            final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
            launchTestThreads(tracker, threadCount, queryCount, createdEvents);
            Thread.sleep(500);
            checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
            List<String> output = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            for (String out : output) {
                if (output.indexOf(out) == 0) {
                    assertThat(out.contains("lang")).isTrue();
                    assertThat(out.contains("_idts")).isTrue();
                    assertThat(out.contains("new_visit")).isTrue();
                } else {
                    assertThat(out.contains("lang")).isFalse();
                    assertThat(out.contains("_idts")).isFalse();
                    assertThat(out.contains("new_visit")).isFalse();
                }
            }
        }
    }

    @Test
    public void sessionStartRaceCondition() throws Exception {
        for (int i = 0; i < 10; i++) {
            Log.d("RaceConditionTest", (10 - i) + " race-condition tests to go.");
            getPiwik().setDryRun(true);
            final Tracker tracker = createTracker();
            tracker.setDispatchInterval(0);
            final int threadCount = 10;
            final int queryCount = 3;
            final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
            launchTestThreads(tracker, threadCount, queryCount, createdEvents);
            Thread.sleep(500);
            checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
            List<String> output = getFlattenedQueries(tracker.getDispatcher().getDryRunOutput());
            for (String out : output) {
                if (output.indexOf(out) == 0) {
                    assertThat(out.contains("lang")).isTrue();
                    assertThat(out.contains("_idts")).isTrue();
                    assertThat(out.contains("new_visit")).isTrue();
                } else {
                    assertThat(out.contains("lang")).isFalse();
                    assertThat(out.contains("_idts")).isFalse();
                    assertThat(out.contains("new_visit")).isFalse();
                }
            }
        }
    }

    @Test
    public void multiThreadDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(20);

        final int threadCount = 20;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void forceDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(-1);

        final int threadCount = 10;
        final int queryCount = 10;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);
        Thread.sleep(500);
        assertThat(createdEvents).hasSize(threadCount * queryCount);
        assertThat(tracker.getDispatcher().getDryRunOutput()).isEmpty();
        assertThat(tracker.dispatch()).isTrue();

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void batchDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(1500);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);
        Thread.sleep(1000);
        assertThat(createdEvents).hasSize(threadCount * queryCount);
        assertThat(tracker.getDispatcher().getDryRunOutput()).isEmpty();
        Thread.sleep(1000);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void batchDispatch() throws Exception {
        final Tracker tracker = createTracker();
        tracker.setDispatchInterval(1500);

        final int threadCount = 5;
        final int queryCount = 5;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());
        launchTestThreads(tracker, threadCount, queryCount, createdEvents);
        Thread.sleep(1000);
        assertThat(createdEvents).hasSize(threadCount * queryCount);
        assertThat(tracker.getDispatcher().getDryRunOutput()).isEmpty();
        Thread.sleep(1000);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }

    @Test
    public void randomDispatchIntervals() throws Exception {
        final Tracker tracker = createTracker();

        final int threadCount = 10;
        final int queryCount = 100;
        final List<String> createdEvents = Collections.synchronizedList(new ArrayList<String>());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (getFlattenedQueries(new ArrayList<>(tracker.getDispatcher().getDryRunOutput())).size() != threadCount * queryCount)
                        tracker.setDispatchInterval(new Random().nextInt(20 - -1) + -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();

        launchTestThreads(tracker, threadCount, queryCount, createdEvents);

        checkForMIAs(threadCount * queryCount, createdEvents, tracker.getDispatcher().getDryRunOutput());
    }
}