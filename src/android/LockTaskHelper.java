package mrmcduff.cordova.plugin.locktask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.UserManager;
import android.provider.Settings;

public class LockTaskHelper {

    public static boolean setDefaultCosuPolicies(Activity activity, boolean active) {
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

    public static boolean setLockTaskMode(Activity activity, boolean shouldLock) {
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
