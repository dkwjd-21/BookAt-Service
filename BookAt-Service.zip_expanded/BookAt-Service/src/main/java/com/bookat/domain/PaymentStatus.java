package com.bookat.domain;

public enum PaymentStatus {
    READY(0),
    PAID(1),
    FAILED(-1),
    CANCELLED(2),
    PART_CANCELLED(3);
    public final int code;
    PaymentStatus(int code){ this.code = code; }
    
    public static PaymentStatus fromCode(int code) {
        for (var s : values()) if (s.code == code) return s;
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
