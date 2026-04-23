package com.jobnet.app.data.model;

public enum WorkMode {
    ONSITE("On-site"),
    HYBRID("Hybrid"),
    REMOTE("Remote");

    private final String label;

    WorkMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static WorkMode from(String raw) {
        if (raw == null) {
            return ONSITE;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        for (WorkMode value : values()) {
            if (value.name().equals(normalized) || value.label.toUpperCase().replace('-', '_').replace(' ', '_').equals(normalized)) {
                return value;
            }
        }
        return ONSITE;
    }
}
