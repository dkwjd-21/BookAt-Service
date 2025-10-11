package com.bookat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class OrderStatusSummary {
    private int created;
    private int paid;
    private int failed;
    private int cancelled;
    private int refunded;
    private int shipping;
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

    public void incrementShipping() {
        shipping++;
    }

    public void incrementFulfilled() {
        fulfilled++;
    }

    @JsonProperty("ready")
    public int getReady() {
        return paid;
    }

    @JsonProperty("completed")
    public int getCompleted() {
        return fulfilled;
    }

    @JsonProperty("returned")
    public int getReturned() {
        return cancelled + refunded;
    }
}
