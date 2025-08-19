package com.s23010301.taskping.models;

public class PriorityTask {
    private final String id;
    private final String title;
    private final int daysRemaining;



    public PriorityTask(String id,String title, int daysRemaining) {
        this.id = id;
        this.title = title;
        this.daysRemaining = daysRemaining;

    }

    // Getters

    public String getId() {return id;}
    public String getTitle() { return title; }
    public int getDaysRemaining() { return daysRemaining; }

}