package com.s23010301.taskping.db;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.s23010301.taskping.models.Notification;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {
    private final NotificationDao notificationDao;

    public NotificationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.notificationDao = db.notificationDao();
    }

    public LiveData<List<Notification>> getAllNotifications() {
        return notificationDao.getAllNotifications();
    }

    public LiveData<List<Notification>> getUnreadNotifications() {
        return notificationDao.getUnreadNotifications();
    }

    public LiveData<Integer> getUnreadCount() {
        return notificationDao.getUnreadCount();
    }

    public void addNotification(String title, String message, String type, String taskId) {
        Notification notification = new Notification(
                UUID.randomUUID().toString(),
                title,
                message,
                type,
                false,
                new Date(),
                taskId
        );
        new Thread(() -> notificationDao.insert(notification)).start();
    }

    public void markAsRead(String notificationId) {
        new Thread(() -> notificationDao.markAsRead(notificationId)).start();
    }

    public void markAllAsRead() {
        new Thread(() -> notificationDao.markAllAsRead()).start();
    }
}