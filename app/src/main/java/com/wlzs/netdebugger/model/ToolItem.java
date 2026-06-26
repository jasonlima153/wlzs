package com.wlzs.netdebugger.model;

public class ToolItem {
    private String name;
    private int icon;
    private String description;

    public ToolItem(String name, int icon, String description) {
        this.name = name;
        this.icon = icon;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
