package com.coduel.model.form;

import com.coduel.common.annotation.NoTrim;
import com.coduel.model.constant.BackgroundPreset;
import com.coduel.model.constant.BubbleStyle;
import com.coduel.model.constant.MessageDensity;
import com.coduel.model.constant.MessageFont;
import com.coduel.model.constant.MessageTextSize;
import com.coduel.model.constant.ThemeMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * A full replace of one thread's personalization (the client always sends the complete object). Enums
 * bind from their names; unknown names are a 400 before this is even reached.
 */
@Getter
@Setter
public class ConversationSettingForm {

    @NotNull(message = "themeMode is required")
    private ThemeMode themeMode;

    // null allowed (= use theme accent). When present it's injected into CSS on the client, so it MUST
    // be a strict #RRGGBB hex — this is the guard against style/CSS injection.
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a #RRGGBB hex color")
    @NoTrim
    private String accentHex;

    @NotNull(message = "backgroundPreset is required")
    private BackgroundPreset backgroundPreset;

    @Size(max = 1000, message = "backgroundImageUrl is too long")
    private String backgroundImageUrl;

    @Min(value = 0, message = "backgroundDim must be 0–100")
    @Max(value = 100, message = "backgroundDim must be 0–100")
    private int backgroundDim;

    @Min(value = 0, message = "backgroundBlur must be 0–20")
    @Max(value = 20, message = "backgroundBlur must be 0–20")
    private int backgroundBlur;

    @NotNull(message = "bubbleStyle is required")
    private BubbleStyle bubbleStyle;

    @NotNull(message = "messageFont is required")
    private MessageFont messageFont;

    // Nullable for forward-compat (an older client may omit it) — defaulted to MEDIUM when applied.
    private MessageTextSize messageTextSize;

    // Nullable for forward-compat — defaulted to COZY when applied.
    private MessageDensity messageDensity;

    @Size(max = 40, message = "nickname is too long")
    private String nickname;

    @Size(max = 16, message = "quickReactionEmoji is too long")
    @NoTrim
    private String quickReactionEmoji;

    private boolean readReceiptsEnabled;
    private boolean muted;
    private boolean archived;

    @Min(value = 1, message = "disappearingTtlSeconds must be positive")
    private Integer disappearingTtlSeconds;
}
