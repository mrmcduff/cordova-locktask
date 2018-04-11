package mrmcduff.cordova.plugin.locktask;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LockTask extends CordovaPlugin {

    private DevicePolicyManager devicePolicyManager;
    private boolean isLockTaskEnabled;
    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("setLocked")) {
            Boolean locked = args.getBoolean(0);
            setLocked(locked);
            return true;
        } else if (action.equals("isLocked")) {
            callbackContext.success(String.valueOf(isKioskEnabled));
            return true;
        }
        return false;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        setLocked(true);
        enterFullscreen();
    }

    private void enterFullscreen() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        });
    }

    private void hideSystemUI() {
        View decorView = cordova.getActivity().getWindow().getDecorView();
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                cordova.getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });
    }

    private void setLocked(boolean locked) {
        Activity activity = cordova.getActivity();
        boolean success = false;
        String message = "Set locktask mode succeeded.";

        if (locked && !isLockTaskEnabled) {
            success = LockTaskHelper.setDefaultCosuProperties(activity, true);
            isLockTaskEnabled = success;
        } else if (!locked && isLockTaskEnabled) {
            success = LockTaskHelper.setDefaultCosuProperties(activity, false);
            isLockTaskEnabled = !success;
        }

        if (!success) {
            message = "Didn't have permission to set lock task mode.";
        }
        callbackMessage(success, message);
    }

    private void callbackMessage(boolean success, String message) {
        if (callbackContext == null) {
            return;
        }
        if (success) {
            PluginResult dataResult = new PluginResult(PluginResult.Status.OK, message);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        } else {
            PluginResult dataResult = new PluginResult(PluginResult.Status.ERROR, message);
            dataResult.setKeepCallback(true);
            callbackContext.sendPluginResult(dataResult);
        }
        callbackContext = null;
    }
}
