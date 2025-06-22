package com.s23010301.taskping;

public class DailyTask {
    private String title;
    private boolean isDone;
    private boolean hasLocation;

    public DailyTask(String title, boolean isDone, boolean hasLocation) {
        this.title = title;
        this.isDone = isDone;
        this.hasLocation = hasLocation;
    }

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