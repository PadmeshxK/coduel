package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** The quoted snippet shown above a reply — enough to render the quote without a separate fetch. */
@Getter
@Setter
public class ReplyPreviewData {

    private Long messageId;
    private Long senderId;
    private String preview;
    // The quoted message's kind ("TEXT"/"IMAGE"/"CODE"/"PROBLEM_SHARE"), so the quote can show an
    // icon + label ("Photo", "Code snippet", "Duel challenge") instead of an empty/odd body.
    private String kind;
}
