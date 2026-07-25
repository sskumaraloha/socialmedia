package com.socialmedia.chat.domain;

public enum ChatMemberRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean hasAdminPrivileges() {
        return this == OWNER || this == ADMIN;
    }
}
