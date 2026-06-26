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

@Getter
@Setter
@Entity
@Table(
        // One reaction per (message, user) — like iMessage/Instagram DMs: re-reacting replaces the emoji,
        // tapping the same one again removes it (the client sends a delete). Indexed by message for the
        // batch load that decorates a thread page.
        uniqueConstraints = @UniqueConstraint(name = "uk_reaction_message_user",
                columnNames = {"message_id", "user_id"}),
        indexes = @Index(name = "idx_reaction_message", columnList = "message_id"))
public class MessageReaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private Long userId;

    // utf8mb4 required in prod to store emoji.
    @Column(nullable = false, length = 16)
    private String emoji;
}
