package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** A single DM — returned in a thread page and pushed live over /user/queue/dm. */
@Getter
@Setter
public class MessageData {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String body;
    private Long createdAtMs;
    // Reactions on this message (empty for a brand-new/live-pushed message; populated on a thread page).
    private List<ReactionData> reactions = List.of();
    // Set when this message is a reply: the target id + a quoted preview (null preview if the original
    // is gone).
    private Long replyToId;
    private ReplyPreviewData replyTo;
    // Non-null once edited; deleted=true renders a tombstone (and the body is blanked server-side).
    private Long editedAtMs;
    private boolean deleted;
    // "TEXT" / "CODE" / "IMAGE" / "PROBLEM_SHARE"; codeLanguage for CODE, attachmentUrl for IMAGE,
    // sharedRef (problem slug) for PROBLEM_SHARE (body = caption).
    private String kind;
    private String codeLanguage;
    private String attachmentUrl;
    private String sharedRef;
    // VOICE: clip length in ms (attachmentUrl carries the audio) — drives the player.
    private Integer durationMs;
}
