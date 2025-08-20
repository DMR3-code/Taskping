package com.s23010301.taskping.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.s23010301.taskping.receivers.GeofenceBroadcastReceiver;

public class GeofenceHelper {
    private final Context context;
    private static final int PENDING_INTENT_REQUEST_CODE = 1001;

    public GeofenceHelper(Context context) {
        this.context = context;
    }

    public GeofencingRequest getGeofencingRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }

    public PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // Use FLAG_MUTABLE for Android 12+ compatibility
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, PENDING_INTENT_REQUEST_CODE, intent, flags);
    }

    // Add this method for individual geofence pending intents
    public PendingIntent getPendingIntentForGeofence(String geofenceId) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.putExtra("geofence_id", geofenceId);

        // Use a unique request code for each geofence
        int requestCode = PENDING_INTENT_REQUEST_CODE + geofenceId.hashCode();

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public String getErrorString(Exception e) {
        return "Error: " + (e != null && e.getMessage() != null ? e.getMessage() : "Unknown");
    }
}