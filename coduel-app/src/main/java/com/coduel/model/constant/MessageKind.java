package com.coduel.model.constant;

/**
 * What a message carries. TEXT is the default; CODE = source snippet; IMAGE = an uploaded picture;
 * PROBLEM_SHARE = a reference to a practice problem (sharedRef = its slug) rendered as a duel card;
 * VOICE = an uploaded voice note (attachmentUrl = the audio, durationMs = its length).
 */
public enum MessageKind {
    TEXT,
    CODE,
    IMAGE,
    PROBLEM_SHARE,
    VOICE
}
