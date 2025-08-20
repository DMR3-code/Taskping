package com.s23010301.taskping.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey
    @NonNull
    private String id;
    private final String title;
    private final String message;
    private final String type;
    private boolean isRead;
    private final Date timestamp;
    private final String taskId;

    public Notification(@NonNull String id, String title, String message, String type,
                        boolean isRead, Date timestamp, String taskId) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.timestamp = timestamp;
        this.taskId = taskId;
    }

    // Getters and setters
    @NonNull public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public Date getTimestamp() { return timestamp; }
    public String getTaskId() { return taskId; }

    public void setRead(boolean read) { isRead = read; }
}