package com.jobnet.app.data.model;

public enum EmploymentType {
    FULL_TIME("Full Time"),
    PART_TIME("Part Time"),
    INTERNSHIP("Internship");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static EmploymentType from(String raw) {
        if (raw == null) {
            return FULL_TIME;
        }
        String normalized = raw.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        for (EmploymentType value : values()) {
            if (value.name().equals(normalized) || value.label.toUpperCase().replace(' ', '_').equals(normalized)) {
                return value;
            }
        }
        return FULL_TIME;
    }
}
