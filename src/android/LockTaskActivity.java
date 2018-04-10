package mrmcduff.cordova.plugin.locktask;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;

import org.apache.cordova.*;

import android.view.View;
import android.widget.*;

public class LockTaskActivity extends CordovaActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }
}
