package com.s23010301.taskping.models;

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
import com.s23010301.taskping.utils.DateUtils;

import java.util.Calendar;

public class TaskReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "TaskReminderReceiver";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_DESCRIPTION = "task_description";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TASK_TITLE);
        String description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION);

        String message = description != null && !description.isEmpty() ?
                title + ": " + description : title;

        NotificationHelper.showNotification(
                context,
                "Task Reminder",
                message
        );
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    public static void scheduleReminder(Context context, String taskId, String title,
                                        String description, Calendar reminderTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.putExtra(EXTRA_TASK_TITLE, title);
        intent.putExtra(EXTRA_TASK_DESCRIPTION, description);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(), // Unique request code per task
                intent,
                flags
        );

        long triggerAtMillis = reminderTime.getTimeInMillis();

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
        );

        Log.d(TAG, "Scheduled reminder for task: " + title);
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