package com.coduel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
// (conversation_id, id) supports keyset pagination of a thread (newest-first, scroll up for history).
@Table(indexes = @Index(name = "idx_message_conversation", columnList = "conversation_id, id"))
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
}
