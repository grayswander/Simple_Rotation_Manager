package net.grayswander.rotationmanager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Created by alexey on 11/11/15.
 */
public class SettingsActivity extends PreferenceActivity implements OnRequestPermissionsResultCallback {
    private static final int WRITE_SETTINGS_PERMISSION_REQUEST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_SETTINGS},
                    WRITE_SETTINGS_PERMISSION_REQUEST);

        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case WRITE_SETTINGS_PERMISSION_REQUEST:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.write_settings_permission_thanks, Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(this, R.string.write_settings_permission_scold, Toast.LENGTH_LONG).show();
                }
        }
    }
}