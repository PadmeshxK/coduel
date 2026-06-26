package com.coduel.model.form;

import com.coduel.model.constant.MessageKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {

    @NotNull(message = "recipientUserId is required")
    private Long recipientUserId;

    // Optional at the form level (an IMAGE may have no caption); the flow requires text for TEXT/CODE
    // and an attachment for IMAGE.
    @Size(max = 5000, message = "exceeds the maximum allowed size")
    private String body;

    // Optional: the message being replied to (must belong to the same conversation).
    private Long replyToId;

    // null = TEXT. CODE carries source in body + a language label; IMAGE carries attachmentUrl;
    // PROBLEM_SHARE carries a problem slug in sharedRef.
    private MessageKind kind;

    @Size(max = 32, message = "codeLanguage is too long")
    private String codeLanguage;

    @Size(max = 512, message = "attachmentUrl is too long")
    private String attachmentUrl;

    @Size(max = 128, message = "sharedRef is too long")
    private String sharedRef;

    // VOICE: clip length in ms (attachmentUrl carries the audio).
    private Integer durationMs;
}
