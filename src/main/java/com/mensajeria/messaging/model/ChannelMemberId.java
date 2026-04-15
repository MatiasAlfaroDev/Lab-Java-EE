package com.mensajeria.messaging.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ChannelMemberId implements Serializable {

    @Column(name = "channel_id", columnDefinition = "uuid")
    private UUID channelId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    public ChannelMemberId() {}

    public ChannelMemberId(UUID channelId, UUID userId) {
        this.channelId = channelId;
        this.userId = userId;
    }

    public UUID getChannelId() { return channelId; }
    public UUID getUserId() { return userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelMemberId)) return false;
        ChannelMemberId that = (ChannelMemberId) o;
        return Objects.equals(channelId, that.channelId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, userId);
    }
}
