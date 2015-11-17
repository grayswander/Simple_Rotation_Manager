package net.grayswander.rotationmanager;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;


public class RotationManagerService extends AccessibilityService {
    public RotationManagerService() {
    }

    String currentPackage = null;
    boolean lastSetRotation = false;
    Context context;
    Configuration configuration;
    Resources resources;
    boolean appStartedFullScreen = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        //Configure these here for compatibility with API 13 and below.
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= 16)
            //Just in case this helps
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);

        this.context = getApplicationContext();

        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

        configuration = new Configuration(this.context);
        this.resources = context.getResources();

    }

    @Override
    public void onDestroy() {
        this.configuration.saveConfiguration();
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if(event == null) {
            debug("Null event");
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            String package_name;
            String class_name;
            try {
                package_name = event.getPackageName().toString();
                class_name = event.getClassName().toString();
            } catch (Exception e) {
                debug("Unable to get component");
                return;
            }

            ComponentName componentName = new ComponentName(
                    package_name,
                    class_name
            );

            Log.d("Service", "Package: " + componentName.getPackageName() + " Class: " + componentName.getClassName());

            ActivityInfo activityInfo = tryGetActivity(componentName);
            boolean isActivity = activityInfo != null;
            if (!isActivity) {
                debug("Received event with NULL activity.");
                return;
            }


            debug("Received app " + package_name);


            if(package_name.equals(this.currentPackage)) {
                debug("App has not been changed");
                return;
            }

            debug("App has been changed");

            boolean is_rotation_enabled = this.getAutoOrientationEnabled();

            debug("Rotation: " + is_rotation_enabled);
            if(!appStartedFullScreen) {
                Log.d("Service", "Saving rotation settings, as fullscreen hack is inactive");
                if (is_rotation_enabled != this.lastSetRotation) {
                    debug("Setting rotation " + is_rotation_enabled + " for " + this.currentPackage);
                    this.configuration.setRotationSetting(this.currentPackage, is_rotation_enabled);
                }
            }
            else {
                Log.d("Service", "Not saving rotation settings, as fullscreen hack is active");
            }

            this.appStartedFullScreen = false;

            if(this.configuration.isForFullscreenWatcher(package_name)) {
                this.startFullscreenWatcher();
            }
            else {
                if(this.configuration.isForFullscreenWatcher(this.currentPackage)) {
                    this.stopFullscreenWatcher();
                }
            }

            this.currentPackage = package_name;

            boolean app_rotation_setting = this.getAppRotationSetting(componentName);

            debug("Got rotation " + app_rotation_setting + " for " + package_name);

            if(is_rotation_enabled != app_rotation_setting) {
                debug("Setting rotation " + app_rotation_setting);
                this.setAutoOrientationEnabled(app_rotation_setting);
            }
            
        }

    }

    private ActivityInfo tryGetActivity(ComponentName componentName) {
        try {
            return getPackageManager().getActivityInfo(componentName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {

    }

    public void setAutoOrientationEnabled(boolean enabled)
    {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.context, R.string.write_settings_permission_denied_message, Toast.LENGTH_LONG).show();
            return;
        }

        if(enabled)
            toast(getResources().getString(R.string.toast_rotation_enabled));
        else
            toast(getResources().getString(R.string.toast_rotation_disabled));


        try {
            Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
            this.lastSetRotation = enabled;
        } catch (Exception e) {
            e.printStackTrace();
            toast(this.resources.getString(R.string.toast_rotation_setting_failed) + "\n" + e.getMessage());
        }
    }


    private boolean getAutoOrientationEnabled() {
        int value = 0;

        try {
            value = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return value != 0;

    }

    public boolean getAppRotationSetting(ComponentName component_name) {
        return this.configuration.getRotationSetting(component_name);
    }



    public void toast(String message) {
        Log.i("RotationManager", message);
        if(this.configuration.isShowNotifications()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void debug(String message) {
        if (this.configuration.isShowDebugNotifications()) {
//            toast(message);
            Log.d("RotationManager", message);
        }
    }

    private void startFullscreenWatcher() {
        Intent startIntent = new Intent(this, FullscreenWatcher.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    private void stopFullscreenWatcher() {
        Intent stopIntent = new Intent(this, FullscreenWatcher.class);
        stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopIntent.putExtra("stop", true);
        startActivity(stopIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent.getBooleanExtra("startedFullScreen", false)) {
            Log.d("Service", "Received startedFullScreen event");
            this.appStartedFullScreen = true;
        }
        else if(intent.getBooleanExtra("stoppedFullScreen", false)) {
            Log.d("Service", "Received stoppedFullScreen event");
            this.appStartedFullScreen = false;
        }

        return START_STICKY;
    }
}
