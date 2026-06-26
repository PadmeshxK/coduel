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
        // Pins are SHARED per conversation (both participants see them) — unlike the per-side
        // personalization. One pin per message; ordered newest-first by id for the pin bar.
        uniqueConstraints = @UniqueConstraint(name = "uk_pin_conversation_message",
                columnNames = {"conversation_id", "message_id"}),
        indexes = @Index(name = "idx_pin_conversation", columnList = "conversation_id"))
public class PinnedMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long messageId;

    @Column(nullable = false)
    private Long pinnedByUserId;
}
