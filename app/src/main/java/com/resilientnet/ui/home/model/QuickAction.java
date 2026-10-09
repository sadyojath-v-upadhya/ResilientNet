package com.resilientnet.ui.home.model;

public class QuickAction {

    private final int icon;
    private final String title;
    private final String subtitle;

    public QuickAction(int icon, String title, String subtitle) {
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}