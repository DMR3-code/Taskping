package com.s23010301.taskping;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e("Geofence", "Error: " + geofencingEvent.getErrorCode());
            return;
        }

        if (geofencingEvent.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("Geofence", "Entered geofence");
            NotificationHelper.showNotification(context, "Task Nearby", "You’ve arrived near a task location.");
        }
    }
}
