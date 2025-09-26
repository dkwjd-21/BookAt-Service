package com.bookat.dto;

import lombok.Getter;

@Getter
public class OrderStatusSummary {
    private int created;
    private int paid;
    private int failed;
    private int cancelled;
    private int refunded;
    private int fulfilled;

    public void incrementCreated() {
        created++;
    }

    public void incrementPaid() {
        paid++;
    }

    public void incrementFailed() {
        failed++;
    }

    public void incrementCancelled() {
        cancelled++;
    }

    public void incrementRefunded() {
        refunded++;
    }

    public void incrementFulfilled() {
        fulfilled++;
    }
}
