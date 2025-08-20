package com.s23010301.taskping.helpers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.s23010301.taskping.R;
import com.s23010301.taskping.activities.MainActivity;
import com.s23010301.taskping.receivers.NotificationActionReceiver;

public class NotificationHelper {
    private static final String CHANNEL_ID = "taskping_notifications";
    private static final String CHANNEL_NAME = "TaskPing Notifications";
    private static final String CHANNEL_DESCRIPTION = "Task reminders and notifications";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 300, 200, 400});
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showNotification(Context context, String title, String message,
                                        String notificationId, String taskId) {
        createNotificationChannel(context);

        // Create intent to open app when notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("open_notifications", true);
        if (taskId != null) {
            intent.putExtra("task_id", taskId); // Pass task ID for deep linking
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create action button to mark as read
        Intent readIntent = new Intent(context, NotificationActionReceiver.class);
        readIntent.setAction("MARK_AS_READ");
        readIntent.putExtra("notification_id", notificationId);
        PendingIntent readPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId.hashCode() + 1,
                readIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_check, "Mark Read", readPendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notificationId.hashCode(), builder.build());
        }
    }
}