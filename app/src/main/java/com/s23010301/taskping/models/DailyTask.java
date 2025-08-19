package com.s23010301.taskping.models;

public class DailyTask {
    private final String id;
    private final String title;
    private boolean isDone;
    private boolean hasLocation;

    public DailyTask(String id, String title, boolean isDone, boolean hasLocation) {
        this.id = id;
        this.title = title;
        this.isDone = isDone;
        this.hasLocation = hasLocation;
    }

    public String getId() {return id;}
    public String getTitle() {
        return title;
    }
    public boolean isDone() {
        return isDone;
    }
    public void setDone(boolean done) {
        isDone = done;
    }
    public boolean hasLocation() {
        return hasLocation;
    }
    public void setHasLocation(boolean hasLocation) {
        this.hasLocation = hasLocation;
    }
}
