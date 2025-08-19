
package com.s23010301.taskping.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.s23010301.taskping.models.GeofenceBroadcastReceiver;

public class GeofenceHelper {
    private final Context context;
    private PendingIntent pendingIntent;

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
        if (pendingIntent != null) return pendingIntent;

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        return pendingIntent;
    }

    public String getErrorString(Exception e) {
        return "Error: " + (e != null && e.getMessage() != null ? e.getMessage() : "Unknown");
    }
}
