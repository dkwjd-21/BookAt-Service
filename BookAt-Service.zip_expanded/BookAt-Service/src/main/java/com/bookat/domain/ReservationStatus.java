package com.bookat.domain;

public enum ReservationStatus {
	
	REFUNDED(-1),
	CANCELED(0),
	RESERVED(1),
	CHANGE_DONE(2);
	
    public final int code;
    ReservationStatus(int code){ this.code = code; }
    
    public static ReservationStatus fromCode(int code) {
        for (var r : values()) if (r.code == code) return r;
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
    
}
