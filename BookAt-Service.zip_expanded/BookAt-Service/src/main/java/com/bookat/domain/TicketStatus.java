package com.bookat.domain;

public enum TicketStatus {
	
	USED(0),
	ACTIVE(1),
	CANCELED(-1);
	
    public final int code;
    TicketStatus(int code){ this.code = code; }
    
    public static TicketStatus fromCode(int code) {
        for (var t : values()) if (t.code == code) return t;
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
    
}
