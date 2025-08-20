package com.s23010301.taskping.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.firebase.firestore.FirebaseFirestore;
import com.s23010301.taskping.helpers.NotificationHelper;
import com.s23010301.taskping.db.NotificationRepository;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent != null && geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing Error: " + geofencingEvent.getErrorCode());
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "User has entered a geofence area.");

            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            for (Geofence geofence : triggeringGeofences) {
                String taskId = geofence.getRequestId();
                Log.d(TAG, "Triggered geofence for task: " + taskId);
                fetchTaskAndNotify(context, taskId);
            }
        }
    }

    private void fetchTaskAndNotify(Context context, String taskId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks").document(taskId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");

                        if (title != null && !title.isEmpty()) {
                            String notificationMessage = "You're near the location for: " + title;

                            // 1. Show system notification
                            NotificationHelper.showNotification(
                                    context,
                                    "Task Nearby",
                                    notificationMessage,
                                    "geofence_" + taskId,
                                    taskId
                            );

                            // 2. Store in database
                            NotificationRepository repository = new NotificationRepository(context);
                            repository.addNotification(
                                    "Location Reminder: " + title,
                                    notificationMessage,
                                    "location_reminder",
                                    taskId
                            );
                        }
                    } else {
                        Log.w(TAG, "Task document not found for ID: " + taskId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching task from Firestore.", e));
    }
}
