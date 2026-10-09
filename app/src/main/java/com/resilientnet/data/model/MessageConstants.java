package com.resilientnet.data.model;

public final class MessageConstants {

    private MessageConstants() {
        // Utility class
    }

    // Message types
    public static final String NORMAL_MESSAGE = "NORMAL_MESSAGE";
    public static final String SOS = "SOS";
    public static final String SOS_UPDATE = "SOS_UPDATE";
    public static final String ACKNOWLEDGEMENT = "ACKNOWLEDGEMENT";
    public static final String SYSTEM_MESSAGE = "SYSTEM_MESSAGE";

    // Message statuses
    public static final String PENDING = "PENDING";
    public static final String SENDING = "SENDING";
    public static final String SENT = "SENT";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";

    // Priorities
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_EMERGENCY = 2;
}