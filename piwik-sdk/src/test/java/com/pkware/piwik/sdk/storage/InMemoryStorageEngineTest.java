package com.pkware.piwik.sdk.storage;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public class InMemoryStorageEngineTest {

    InMemoryStorageEngine storageEngine;

    @Before
    public void setUp() {
        storageEngine = new InMemoryStorageEngine();
    }

    @Test
    public void add_single() {
        storageEngine.add("blah");

        assertThat(storageEngine.get()).containsExactly("blah");
    }

    @Test
    public void add_multiple() {
        storageEngine.add("a");
        storageEngine.add("b");

        assertThat(storageEngine.get()).containsExactly("a", "b");
    }

    /**
     * To produce a concurrent conflict situation, we'll have 1 thread per core, and have each thread
     * performing many operations on the engine
     */
    @Test
    public void add_concurrentModification() throws InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        for (int i = 0; i < cores; i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 300; j++) {
                        storageEngine.add("event");
                    }
                }
            });
        }
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void get_empty() {
        assertThat(storageEngine.get()).isEmpty();
    }

    @Test
    public void get_isACopy() {
        try {
            storageEngine.get().add("blah");
            assertThat(storageEngine.get()).doesNotContain("blah");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void remove_none() {
        storageEngine.add("a");
        storageEngine.add("b");

        storageEngine.remove(Collections.<String>emptySet());

        assertThat(storageEngine.get()).containsExactly("a", "b");
    }

    @Test
    public void remove_single() {
        storageEngine.add("a");
        storageEngine.add("b");

        storageEngine.remove(Collections.singleton("a"));

        assertThat(storageEngine.get()).containsExactly("b");
    }

    @Test
    public void remove_multiple() {
        storageEngine.add("a");
        storageEngine.add("b");

        storageEngine.remove(Arrays.asList("a", "b"));

        assertThat(storageEngine.get()).isEmpty();
    }

    /**
     * To
     */
    @Test
    public void remove_concurrentModification() throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            storageEngine.add("a");
            storageEngine.add("b");
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Void> collisionTask = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 0; i < 300; i++) {
                    storageEngine.remove(Collections.singleton("a"));
                    storageEngine.remove(Collections.singleton("b"));
                }
                return null;
            }
        };
        executor.invokeAll(Arrays.asList(collisionTask, collisionTask));
        executor.awaitTermination(2, TimeUnit.SECONDS);

        assertThat(storageEngine.isEmpty()).isTrue();
    }

    @Test
    public void isEmpty_true() {
        assertThat(storageEngine.isEmpty()).isTrue();
    }

    @Test
    public void isEmpty_false() {
        storageEngine.add("blah");
        assertThat(storageEngine.isEmpty()).isFalse();
    }
}
