package com.mensajeria.interaction.model;

import com.mensajeria.auth.model.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poll_votes")
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_option_id", nullable = false)
    private PollOption pollOption;

    // Nullable: soporte para votaciones anónimas
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt;

    @PrePersist
    private void prePersist() {
        votedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public PollOption getPollOption() { return pollOption; }
    public void setPollOption(PollOption pollOption) { this.pollOption = pollOption; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getVotedAt() { return votedAt; }
}
