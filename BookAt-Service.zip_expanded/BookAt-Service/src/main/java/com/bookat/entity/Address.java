package com.bookat.entity;

import lombok.Data;

@Data
public class Address {
    private int addrId;
    private String userId;
    private String addrName;
    private String recipientName;
    private String recipientPhone;
    private String addr;
}
