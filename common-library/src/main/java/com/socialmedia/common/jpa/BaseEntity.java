package com.socialmedia.common.jpa;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Shared identity + audit-timestamp base for JPA entities whose primary key is
 * generated locally. Relies on {@link JpaAuditingAutoConfiguration} (auto-registered via
 * this library) to actually populate createdAt/updatedAt - without it, these fields
 * silently stay null on insert.
 */
@MappedSuperclass
public abstract class BaseEntity extends TimestampedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
