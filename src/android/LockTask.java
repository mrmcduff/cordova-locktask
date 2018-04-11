package mrmcduff.cordova.plugin.locktask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.Settings;
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
            callbackContext.success(String.valueOf(isLockTaskEnabled));
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
            success = setDefaultCosuPolicies(activity, true);
            isLockTaskEnabled = success;
        } else if (!locked && isLockTaskEnabled) {
            success = setDefaultCosuPolicies(activity, false);
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
    }

    private static boolean setDefaultCosuPolicies(Activity activity, boolean active) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if(!devicePolicyManager.isDeviceOwnerApp(activity.getPackageName())){
            return false;
        }

        ComponentName adminComponentName = DeviceAdminReceiver.getComponentName(activity);

        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_CREATE_WINDOWS, active);
        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_ADJUST_VOLUME, active);

        // Remember to always Disallow Fun, because what fun would it be if you didn't?
        setUserRestriction(devicePolicyManager, adminComponentName, UserManager.DISALLOW_FUN, active);

        devicePolicyManager.setKeyguardDisabled(adminComponentName, active);
        devicePolicyManager.setStatusBarDisabled(adminComponentName, active);

        enableStayOnWhilePluggedIn(devicePolicyManager, adminComponentName, active);

        if (active){
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName, null);
        }

        devicePolicyManager.setLockTaskPackages(adminComponentName,
                active ? new String[]{activity.getPackageName()} : new String[]{});
        return true;
    }

    private static boolean setLockTaskMode(Activity activity, boolean shouldLock) {
        if (!isDevicePolicyManagementAllowed(activity)) {
            return false;
        }

        ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (shouldLock && am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
            activity.startLockTask();
        } else if (!shouldLock && am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED) {
            activity.stopLockTask();
        }
        return true;
    }

    private static void setUserRestriction(
            DevicePolicyManager devicePolicyManager,
            ComponentName adminComponentName,
            String restriction,
            boolean disallow){
        if (disallow) {
            devicePolicyManager.addUserRestriction(adminComponentName, restriction);
        } else {
            devicePolicyManager.clearUserRestriction(adminComponentName, restriction);
        }
    }

    private static void enableStayOnWhilePluggedIn(
            DevicePolicyManager devicePolicyManager,
            ComponentName adminComponentName,
            boolean enabled){
        if (enabled) {
            devicePolicyManager.setGlobalSetting(
                    adminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                            | BatteryManager.BATTERY_PLUGGED_USB
                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            devicePolicyManager.setGlobalSetting(
                    adminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
            );
        }
    }

    private static DevicePolicyManager getDevicePolicyManagerFromActivity(Activity activity) {
        return (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    private static boolean isDevicePolicyManagementAllowed(Activity activity) {
        DevicePolicyManager devicePolicyManager = getDevicePolicyManagerFromActivity(activity);
        return devicePolicyManager.isDeviceOwnerApp(activity.getApplicationContext().getPackageName());
    }
}
