package com.friendavailability.model;

import lombok.Getter;

@Getter
public enum AvailabilitySource {
    MANUAL("Manual Entry"),
    GOOGLE_CALENDAR("Google Calendar"),
    OVERRIDE("Manual Override"),
    RECURRING("Recurring Pattern");

    private final String displayName;

    AvailabilitySource(String displayName){
        this.displayName = displayName;
    }

    public int getPriority(){
        return switch (this) {
            case OVERRIDE -> 4;
            case MANUAL -> 3;
            case GOOGLE_CALENDAR -> 2;
            case RECURRING -> 1;
        };
    }

    public boolean isEditable(){
        return this == OVERRIDE
                || this == MANUAL
                || this == RECURRING;
    }

    public boolean isManual(){
        return this == MANUAL || this == OVERRIDE;
    }

    public boolean isImported(){
        return this == GOOGLE_CALENDAR;
    }

    public boolean isCurrentlySupported(){
        return this == MANUAL;
    }

    @Override
    public String toString(){
        return displayName;
    }
}
