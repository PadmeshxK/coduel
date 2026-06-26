package com.coduel.entity;

import com.coduel.model.constant.BackgroundPreset;
import com.coduel.model.constant.BubbleStyle;
import com.coduel.model.constant.MessageDensity;
import com.coduel.model.constant.MessageFont;
import com.coduel.model.constant.MessageTextSize;
import com.coduel.model.constant.ThemeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        // One row per (owner, peer): this is OWNER's private view of their thread with PEER. The other
        // side has their own row, so customizing your side never touches theirs — same per-participant
        // spirit as the read markers on Conversation. Owner-scoped index drives the inbox lookup.
        uniqueConstraints = @UniqueConstraint(name = "uk_conv_setting_owner_peer",
                columnNames = {"owner_user_id", "peer_user_id"}),
        indexes = @Index(name = "idx_conv_setting_owner", columnList = "owner_user_id"))
public class ConversationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerUserId;

    @Column(nullable = false)
    private Long peerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ThemeMode themeMode = ThemeMode.INHERIT;

    // null = use the active theme's accent; otherwise a validated #RRGGBB override.
    @Column(length = 7)
    private String accentHex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BackgroundPreset backgroundPreset = BackgroundPreset.PARCHMENT;

    // Only meaningful when backgroundPreset = IMAGE.
    private String backgroundImageUrl;

    // 0–100 overlay opacity over the background so message text stays readable on busy art.
    @Column(nullable = false)
    private int backgroundDim = 40;

    // 0–20 px blur applied to the background.
    @Column(nullable = false)
    private int backgroundBlur = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BubbleStyle bubbleStyle = BubbleStyle.ROUNDED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageFont messageFont = MessageFont.SANS;

    // Nullable (column added later) so existing rows don't break; a null reads back as MEDIUM.
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MessageTextSize messageTextSize = MessageTextSize.MEDIUM;

    // Nullable (column added later); a null reads back as COZY.
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private MessageDensity messageDensity = MessageDensity.COZY;

    // Private label the owner gives the peer (only the owner ever sees it).
    @Column(length = 40)
    private String nickname;

    // The double-tap quick-reaction emoji. utf8mb4 required in prod to store emoji.
    @Column(nullable = false, length = 16)
    private String quickReactionEmoji = "🔥"; // 🔥

    // Owner shows "Seen" to the peer. Off = the read marker is still persisted (badge clears) but the
    // receipt isn't broadcast.
    @Column(nullable = false)
    private boolean readReceiptsEnabled = true;

    // Owner muted the peer → no toast/bell on their messages (the DM still arrives in the thread).
    @Column(nullable = false)
    private boolean muted = false;

    // Hidden from the owner's inbox — unless it goes unread, so an incoming message is never lost.
    @Column(nullable = false)
    private boolean archived = false;

    // null = off; otherwise messages auto-expire after this many seconds.
    private Integer disappearingTtlSeconds;

    // When disappearing was turned on (set on the off→on transition, cleared on on→off). The sweep only
    // purges messages sent at/after this instant, so history from before it was enabled is left intact.
    private java.time.Instant disappearingEnabledAt;
}
