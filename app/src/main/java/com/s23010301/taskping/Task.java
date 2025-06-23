package com.s23010301.taskping;

public class Task {
    private String id;
    private String title;
    private String type; // "daily" or "priority"
    private boolean done;

    public Task() {
        // Required for Firestore
    }

    public Task(String id, String title, String type, boolean done) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.done = done;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
