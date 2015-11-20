package net.grayswander.rotationmanager;
/**
 * This file is part of Simple Rotation Manager.
 *
 * Simple Rotation Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Simple Rotation Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */


import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class Configuration implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String ROTATION_CONFIGURATION = "net.grayswander.rotationmanager.configuration.rotation";

    private SharedPreferences generalConfiguration;
    private SharedPreferences rotationConfiguration;

    private Context context;

    private boolean showNotifications = true;
    private boolean showDebugNotifications = false;

    private boolean defaultRotationSetting = false;

    private Set<String> fullScreenWatcherList;


    public Configuration(Context context) {
        this.context = context;

        this.fullScreenWatcherList = new HashSet<>();
        this.generalConfiguration = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.rotationConfiguration = this.context.getSharedPreferences(ROTATION_CONFIGURATION, Context.MODE_PRIVATE);
        this.generalConfiguration.registerOnSharedPreferenceChangeListener(this);

        loadConfiguration();
    }

    public SharedPreferences getGeneralConfiguration() {
        return generalConfiguration;
    }

    public SharedPreferences getRotationConfiguration() {
        return rotationConfiguration;
    }

    public boolean isShowNotifications() {
        return showNotifications;
    }

    public void setShowNotifications(boolean showNotifications) {
        this.showNotifications = showNotifications;
    }

    public boolean isShowDebugNotifications() {
        return showDebugNotifications;
    }

    public void setShowDebugNotifications(boolean showDebugNotifications) {
        this.showDebugNotifications = showDebugNotifications;

    }

    public boolean getDefaultRotationSetting() {
        return defaultRotationSetting;
    }

    public void setDefaultRotationSetting(boolean defaultRotationSetting) {
        this.defaultRotationSetting = defaultRotationSetting;
    }

    public void loadConfiguration() {
        this.showNotifications = this.generalConfiguration.getBoolean("showNotifications", true);
        this.showDebugNotifications = this.generalConfiguration.getBoolean("showDebugNotifications", false);
        this.defaultRotationSetting = this.generalConfiguration.getBoolean("defaultRotationSetting", false);

        if(this.generalConfiguration.getBoolean("facebookHackEnabled", true)) {
            this.fullScreenWatcherList.add("com.facebook.katana");
        }

        Log.d("RotationManager", "Loaded configuration");
    }

    public void saveConfiguration() {
        SharedPreferences.Editor editor = this.generalConfiguration.edit();
        editor.putBoolean("showNotifications", showNotifications);
        editor.putBoolean("showDebugNotifications", showDebugNotifications);
        editor.putBoolean("defaultRotationSetting", defaultRotationSetting);
        editor.apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        this.loadConfiguration();
    }

    public boolean getRotationSetting(String package_name) {
        return this.rotationConfiguration.getBoolean(package_name, this.getDefaultRotationSetting());
    }

    public boolean getRotationSetting(ComponentName component_name) {
//        Log.d("Configuration", "Package: " + component_name.getPackageName() + " Class: " + component_name.getClassName());
        switch (component_name.getPackageName()) {
            default:
                return this.getRotationSetting(component_name.getPackageName());
        }
    }

    public void setRotationSetting(String package_name, boolean enabled) {
        this.rotationConfiguration.edit().putBoolean(package_name, enabled).apply();
    }

    public boolean isForFullscreenWatcher(String package_name) {
        return this.fullScreenWatcherList.contains(package_name);
    }
}
