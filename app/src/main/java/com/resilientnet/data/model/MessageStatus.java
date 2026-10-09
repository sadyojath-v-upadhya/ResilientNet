package com.resilientnet.data.model;

public final class MessageStatus {

    private MessageStatus() {
    }

    public static final String PENDING = "PENDING";
    public static final String SENT = "SENT";
    public static final String DELIVERED = "DELIVERED";
    public static final String FORWARDED = "FORWARDED";
    public static final String FAILED = "FAILED";
}