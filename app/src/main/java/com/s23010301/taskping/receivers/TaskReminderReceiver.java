package com.s23010301.taskping.receivers;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import com.s23010301.taskping.helpers.NotificationHelper;
import com.s23010301.taskping.db.NotificationRepository;

import java.util.Calendar;

public class TaskReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "TaskReminderReceiver";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_DESCRIPTION = "task_description";
    public static final String EXTRA_TASK_ID = "task_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TASK_TITLE);
        String description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION);
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);

        String message = description != null && !description.isEmpty() ?
                title + ": " + description : title;

        // 1. Show system notification
        NotificationHelper.showNotification(
                context,
                "Task Reminder",
                message,
                "reminder_" + taskId,
                taskId
        );

        // 2. Store notification in database
        NotificationRepository repository = new NotificationRepository(context);
        repository.addNotification(
                "Task Reminder: " + title,
                message,
                "task_reminder",
                taskId
        );

        Log.d(TAG, "Task reminder triggered: " + title);
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public static void scheduleReminder(Context context, String taskId, String title,
                                        String description, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.putExtra(EXTRA_TASK_TITLE, title);
        intent.putExtra(EXTRA_TASK_DESCRIPTION, description);
        intent.putExtra(EXTRA_TASK_ID, taskId); // Add task ID

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(), // Unique request code per task
                intent,
                flags
        );

        long triggerAtMillis = reminderTime.getTimeInMillis();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        Log.d(TAG, "Scheduled reminder for task: " + title + " at " + reminderTime.getTime());
    }

    public static void cancelReminder(Context context, String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskReminderReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                flags
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "Cancelled reminder for task ID: " + taskId);
    }
}
