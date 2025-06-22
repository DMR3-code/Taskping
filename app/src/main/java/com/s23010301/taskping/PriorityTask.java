package com.s23010301.taskping;

public class PriorityTask {
    private String title;
    private int progress;
    private int daysRemaining;
    private int iconRes;

    public PriorityTask(String title, int progress, int daysRemaining, int iconRes) {
        this.title = title;
        this.progress = progress;
        this.daysRemaining = daysRemaining;
        this.iconRes = iconRes;
    }

    // Getters
    public String getTitle() { return title; }
    public int getProgress() { return progress; }
    public int getDaysRemaining() { return daysRemaining; }
    public int getIconRes() { return iconRes; }
}
