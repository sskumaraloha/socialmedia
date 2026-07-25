package com.socialmedia.user.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "contacts", indexes = {
        @Index(name = "idx_contacts_owner_id", columnList = "ownerId"),
        @Index(name = "idx_contacts_pair", columnList = "ownerId,contactUserId", unique = true)
})
public class Contact extends BaseEntity {

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID contactUserId;

    private String nickname;

    protected Contact() {
    }

    public Contact(UUID ownerId, UUID contactUserId, String nickname) {
        this.ownerId = ownerId;
        this.contactUserId = contactUserId;
        this.nickname = nickname;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getContactUserId() {
        return contactUserId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
