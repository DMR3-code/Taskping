package com.s23010301.taskping.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.s23010301.taskping.db.NotificationRepository;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("MARK_AS_READ".equals(intent.getAction())) {
            String notificationId = intent.getStringExtra("notification_id");
            NotificationRepository repository = new NotificationRepository(context);
            repository.markAsRead(notificationId);
        }
    }
}