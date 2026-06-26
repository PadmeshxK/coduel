package com.coduel.entity;

import com.coduel.model.constant.MessageKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

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

    // The message this one replies to (null = not a reply). Must be in the same conversation (enforced
    // at send). Not a hard FK so deleting the original doesn't cascade — the reply just loses its quote.
    private Long replyToId;

    // Non-null once the sender edits the message; drives the "edited" marker.
    private Instant editedAt;

    // Non-null once soft-deleted — the row stays (replies may quote it) but the body is never served.
    private Instant deletedAt;

    // What the message carries. TEXT = body is plain text; CODE = body is source + codeLanguage set.
    // Nullable so adding the column to a populated table can't backfill a bad NOT-NULL value; a null/
    // missing kind is treated as TEXT on read.
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MessageKind kind = MessageKind.TEXT;

    // For CODE: the language label (display only here; execution is a later slice).
    @Column(length = 32)
    private String codeLanguage;

    // For IMAGE: the stored media URL (body is an optional caption).
    @Column(length = 512)
    private String attachmentUrl;

    // For PROBLEM_SHARE: the shared problem's slug (body is an optional caption). The client resolves
    // the title/difficulty by slug to render the duel card.
    @Column(length = 128)
    private String sharedRef;

    // For VOICE: the clip length in milliseconds (attachmentUrl holds the audio) — drives the player.
    private Integer durationMs;
}
