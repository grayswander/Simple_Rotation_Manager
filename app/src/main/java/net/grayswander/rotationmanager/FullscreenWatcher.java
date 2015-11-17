package net.grayswander.rotationmanager;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.widget.Toast;

import java.util.List;

/**
 * Created by alexey on 11/17/15.
 */
public class FullscreenWatcher extends Activity implements OnSystemUiVisibilityChangeListener{

    Context context;
    Configuration configuration;
    Resources resources;
    boolean previous_setting = false;

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("FullscreenWatcher", "Starting");

        this.context = getApplicationContext();
        configuration = new Configuration(this.context);
        this.resources = context.getResources();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);

        moveTaskToBack(true);
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        Log.d("FullscreenWatcher", "Received event " + visibility);
        if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            Log.d("FullscreenWatcher", "Went full screen");

           previous_setting = this.getAutoOrientationEnabled();
            if(!previous_setting) {
                this.setAutoOrientationEnabled(true);
            }
            this.sendFullscreenStarted();
        }
        else {
            Log.d("FullscreenWatcher", "Exited full screen");
            if(previous_setting != this.getAutoOrientationEnabled()) {
                this.setAutoOrientationEnabled(previous_setting);
            }
            this.sendFullscreenStopped();
        }

    }

    @Override
    protected void onDestroy() {
        Log.d("FullscreenWatcher", "Destroying");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        Log.d("FullscreenWatcher", "Stopping");
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if(intent.getBooleanExtra("stop", false)) {
            Log.d("FullscreenWatcher", "Shutting down by intent");
            finish();
        }
        else {
             Log.d("FullscreenWatcher", "Received intent");
        }

        super.onNewIntent(intent);
    }

    public void toast(String message) {
        Log.i("FullscreenWatcher", message);
        if(this.configuration.isShowNotifications()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void debug(String message) {
        if (this.configuration.isShowDebugNotifications()) {
//            toast(message);
            Log.d("FullscreenWatcher", message);
        }
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

    private void sendFullscreenStarted() {
        Intent serviceIntent = new Intent(RotationManagerService.class.getName());
        serviceIntent.setPackage("net.grayswander.rotationmanager");
        serviceIntent.putExtra("startedFullScreen", true);
        context.startService(serviceIntent);
    }

    private void sendFullscreenStopped() {
        Intent serviceIntent = new Intent(RotationManagerService.class.getName());
        serviceIntent.setPackage("net.grayswander.rotationmanager");
        serviceIntent.putExtra("stoppedFullScreen", true);
        context.startService(serviceIntent);
    }

    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }
}
