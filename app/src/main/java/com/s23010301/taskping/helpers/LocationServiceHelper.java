package com.s23010301.taskping.helpers;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

public class LocationServiceHelper {
    private static final String TAG = "LocationServiceHelper";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public static boolean checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "Google Play Services not available but can be resolved");
            } else {
                Log.e(TAG, "This device does not support Google Play Services");
            }
            return false;
        }
        return true;
    }

    public static boolean isGeofencingSupported(Context context) {
        return checkPlayServices(context);
    }

    public static Task<LocationSettingsResponse> checkLocationSettings(Context context) {
        SettingsClient settingsClient = LocationServices.getSettingsClient(context);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .setAlwaysShow(true); // Show dialog if needed

        return settingsClient.checkLocationSettings(builder.build());
    }
}