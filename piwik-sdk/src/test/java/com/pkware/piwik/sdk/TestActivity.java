package com.pkware.piwik.sdk;

import android.app.Activity;
import android.os.Bundle;

public class TestActivity extends Activity {

    public static String getTestTitle() {
        return "Test Activity";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getTestTitle());
    }
}
