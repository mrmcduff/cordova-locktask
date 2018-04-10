package mrmcduff.cordova.plugin.locktask;

import android.content.ComponentName;
import android.content.Context;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
    }
}
