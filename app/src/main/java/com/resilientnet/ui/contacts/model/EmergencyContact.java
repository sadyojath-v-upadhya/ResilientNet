package com.resilientnet.ui.contacts.model;

public class EmergencyContact {

    private final String id;
    private final String name;
    private final String phoneNumber;
    private final String category;
    private final String description;
    private final boolean isPreloaded;

    public EmergencyContact(
            String id,
            String name,
            String phoneNumber,
            String category,
            String description,
            boolean isPreloaded) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.category = category;
        this.description = description;
        this.isPreloaded = isPreloaded;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPreloaded() {
        return isPreloaded;
    }
}
