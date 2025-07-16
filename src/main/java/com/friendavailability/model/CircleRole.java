package com.friendavailability.model;

public enum CircleRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean canManageMembers(){
        return this == OWNER || this == ADMIN;
    }

    public boolean canModifyCircle(){
        return this == OWNER || this == ADMIN;
    }

    public boolean canDeleteCircle(){
        return this == OWNER;
    }

    public boolean canManageRoles(){
        return this == OWNER;
    }

    public boolean hasHigherPermission(CircleRole otherRole){
        if(otherRole == null) return true;

        return this.ordinal() < otherRole.ordinal();
    }

    public String getDisplayName() {
        return switch (this) {
            case OWNER -> "Circle Owner";
            case ADMIN -> "Circle Admin";
            case MEMBER -> "Member";
        };
    }
}
