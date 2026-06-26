package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** The owner's personalization for one DM thread — enums sent as their names; null nickname/accent = default. */
@Getter
@Setter
public class ConversationSettingData {

    private Long peerUserId;
    private String themeMode;
    private String accentHex;
    private String backgroundPreset;
    private String backgroundImageUrl;
    private int backgroundDim;
    private int backgroundBlur;
    private String bubbleStyle;
    private String messageFont;
    private String messageTextSize;
    private String messageDensity;
    private String nickname;
    private String quickReactionEmoji;
    private boolean readReceiptsEnabled;
    private boolean muted;
    private boolean archived;
    private Integer disappearingTtlSeconds;
}
