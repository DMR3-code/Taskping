package com.s23010301.taskping.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.s23010301.taskping.models.Notification;
import java.util.List;

@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Notification notification);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    LiveData<List<Notification>> getAllNotifications();

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    LiveData<List<Notification>> getUnreadNotifications();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    LiveData<Integer> getUnreadCount();

    @Update
    void update(Notification notification);

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markAsRead(String id);

    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM notifications")
    void clearAll();
}