package com.s23010301.taskping.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;


@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey
    @NonNull
    private String id;

    private String title;
    private String type;       // "daily" or "priority"
    private String date;       // for filtering (startDate)
    private boolean done;

    // 🔽 New fields for detailed view
    private String description;
    private String endDate;
    private boolean hasLocation;
    private String location;   // Format: "lat,lng"

    @Ignore
    public Task(@NonNull String id, String title, String type, String date, boolean done) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.date = date;
        this.done = done;
    }

    // ✅ Optional: Full constructor including new fields
    public Task(@NonNull String id, String title, String type, String date, boolean done,
                String description, String endDate, boolean hasLocation, String location) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.date = date;
        this.done = done;
        this.description = description;
        this.endDate = endDate;
        this.hasLocation = hasLocation;
        this.location = location;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public boolean isDone() {
        return done;
    }

    public String getDescription() {
        return description;
    }

    public String getEndDate() {
        return endDate;
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    public String getLocation() {
        return location;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
