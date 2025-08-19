package com.s23010301.taskping.models;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.s23010301.taskping.helpers.NotificationHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // This is the line you mentioned. It must be INSIDE the onReceive method.
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        // Check for errors
        if (Objects.requireNonNull(geofencingEvent).hasError()) {
            Log.e(TAG, "Geofencing Error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type (e.g., entering the geofence)
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "User has entered a geofence area.");

            // Get the specific geofence(s) that were triggered
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : Objects.requireNonNull(triggeringGeofences)) {
                String taskId = geofence.getRequestId();
                fetchTaskAndNotify(context, taskId);

                // Cancel the geofence after triggering
                GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);
                geofencingClient.removeGeofences(Arrays.asList(taskId));

            }
        }
    }

    private void fetchTaskAndNotify(Context context, String taskId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks").document(taskId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");

                        if (title != null && !title.isEmpty()) {
                            String notificationMessage = "Remember: " + title;
                            // Show the specific notification
                            NotificationHelper.showNotification(context, "Task Nearby", notificationMessage);
                        }

                    } else {
                        Log.w(TAG, "Task document not found for ID: " + taskId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching task from Firestore.", e));
    }
}