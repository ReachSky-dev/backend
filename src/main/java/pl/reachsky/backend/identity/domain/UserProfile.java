package pl.reachsky.backend.identity.domain;

import pl.reachsky.backend.shared.Ids;
import pl.reachsky.backend.shared.UserId;

import java.time.Instant;

public final class UserProfile {

    private final UserId id;
    private final String subject;
    private final String displayName;
    private final Instant createdAt;

    private UserProfile(UserId id, String subject, String displayName, Instant createdAt) {
        this.id = id;
        this.subject = subject;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public static UserProfile create(String subject, String displayName) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("subject must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        return new UserProfile(new UserId(Ids.next()), subject, displayName, Instant.now());
    }

    public static UserProfile reconstitute(UserId id, String subject, String displayName, Instant createdAt) {
        return new UserProfile(id, subject, displayName, createdAt);
    }

    public UserId getId() { return id; }
    public String getSubject() { return subject; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}
