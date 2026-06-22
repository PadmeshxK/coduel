package com.coduel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_pair",
                columnNames = {"lower_user_id", "higher_user_id"}),
        indexes = {
                @Index(name = "idx_conversation_lower", columnList = "lower_user_id"),
                @Index(name = "idx_conversation_higher", columnList = "higher_user_id")
        })
public class Conversation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A 1:1 DM thread, keyed by its two participants stored in canonical sorted order: lowerUserId
    // always holds the smaller userId, higherUserId the larger. Sorting makes the pair direction-
    // independent, so there's exactly one row per pair (enforced by the unique constraint) and it's
    // found the same way from either side.
    @Column(nullable = false)
    private Long lowerUserId;

    @Column(nullable = false)
    private Long higherUserId;

    // Denormalized so the inbox (conversation list, most-recent-first) renders without scanning messages.
    private Instant lastMessageAt;
    private Long lastSenderId;
    @Column(length = 200)
    private String lastPreview;

    // Per-participant read marker: when the lower / higher user last opened this thread. A conversation
    // is unread for a viewer when there's a newer message from the other side than their marker (null =
    // never opened). This is what makes "read" survive leaving and re-entering the thread.
    private Instant lowerUserLastReadAt;
    private Instant higherUserLastReadAt;
}
