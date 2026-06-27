package com.coduel.flow;

import com.coduel.api.ConversationApi;
import com.coduel.api.ConversationSettingApi;
import com.coduel.api.FriendshipApi;
import com.coduel.api.MessageApi;
import com.coduel.api.MessageReactionApi;
import com.coduel.api.PinnedMessageApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Conversation;
import com.coduel.entity.ConversationSetting;
import com.coduel.entity.Friendship;
import com.coduel.entity.Message;
import com.coduel.entity.MessageReaction;
import com.coduel.entity.PinnedMessage;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import com.coduel.model.constant.MessageKind;
import com.coduel.model.form.ConversationSettingForm;
import com.coduel.model.result.ConversationView;
import com.coduel.model.result.DmSentResult;
import com.coduel.model.data.PinnedMessageData;
import com.coduel.model.result.MarkReadResult;
import com.coduel.model.result.MessageUpdateResult;
import com.coduel.model.result.MessageView;
import com.coduel.model.result.PinResult;
import com.coduel.model.result.ReactionResult;
import com.coduel.model.result.TypingSignalResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ChatFlow {

    // A message can only be edited within this window of being sent.
    private static final long EDIT_WINDOW_SECONDS = 300; // 5 minutes
    // Largest message-search page the API will serve.
    private static final int SEARCH_MAX_SIZE = 50;
    // Inbox preview shown when the thread's newest message has been deleted (client renders a tombstone).
    private static final String DELETED_PREVIEW = "Message deleted";

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private ConversationApi conversationApi;
    @Autowired
    private MessageApi messageApi;
    @Autowired
    private MessageReactionApi messageReactionApi;
    @Autowired
    private PinnedMessageApi pinnedMessageApi;
    @Autowired
    private ConversationSettingApi conversationSettingApi;
    @Autowired
    private ProblemApi problemApi;

    // Orchestration only: resolve both parties and guard (not self, must be friends), then let the Apis
    // create the message and refresh the thread. The Dto pushes the result to the recipient after commit.
    public DmSentResult sendDirectMessage(String senderGoogleId, Long recipientUserId, String body, Long replyToId,
                                          MessageKind kind, String codeLanguage, String attachmentUrl,
                                          String sharedRef, Integer durationMs) throws ApiException {
        User sender = userApi.getCheckByGoogleId(senderGoogleId);
        if (sender.getId().equals(recipientUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_135, List.of());
        }
        Friendship friendship = friendshipApi.findBetween(sender.getId(), recipientUserId);
        if (Objects.isNull(friendship) || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_134, List.of());
        }
        User recipient = userApi.getCheckById(recipientUserId);

        // Per-kind content rule: an image needs an attachment; a shared problem needs a (valid) slug;
        // text/code need a non-blank body. Body is an optional caption for image/problem.
        boolean hasBody = body != null && !body.isBlank();
        String preview;
        if (kind == MessageKind.IMAGE) {
            if (attachmentUrl == null || attachmentUrl.isBlank()) {
                throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_143, List.of());
            }
            preview = "Photo";
        } else if (kind == MessageKind.VOICE) {
            if (attachmentUrl == null || attachmentUrl.isBlank()) {
                throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_143, List.of());
            }
            preview = "Voice message";
        } else if (kind == MessageKind.PROBLEM_SHARE) {
            if (sharedRef == null || sharedRef.isBlank()) {
                throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_148, List.of());
            }
            // Validate the problem exists + use its title as the inbox preview ("Shared: Two Sum").
            preview = "Shared: " + problemApi.getCheckBySlug(sharedRef).getTitle();
        } else if (!hasBody) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_147, List.of());
        } else {
            preview = kind == MessageKind.CODE ? "Code snippet" : body;
        }

        Conversation conversation = conversationApi.getOrCreate(sender.getId(), recipientUserId);
        // A reply must target a message in THIS conversation (no quoting across threads).
        Message replyTo = null;
        if (!Objects.isNull(replyToId)) {
            replyTo = messageApi.getCheckById(replyToId);
            if (!replyTo.getConversationId().equals(conversation.getId())) {
                throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_138, List.of());
            }
        }
        Message message = messageApi.create(
                conversation.getId(), sender.getId(), body, replyToId, kind, codeLanguage, attachmentUrl,
                sharedRef, durationMs);
        conversationApi.recordLastMessage(conversation, sender.getId(), preview);

        return ConversionHelper.toDmSentResult(message, recipient.getGoogleId(), sender, replyTo);
    }

    // Search the caller's DMs for messages containing the query, newest-first, offset-paginated (same
    // page/size + PageData shape as the problem list). Each hit carries the other participant so the
    // client can open the thread. Blank query / no threads → an empty page.
    public com.coduel.common.data.PageData<com.coduel.model.data.MessageSearchData> searchMessages(
            String googleId, String query, Long conversationId, int page, int size) throws ApiException {
        int safeSize = Math.min(Math.max(size, 1), SEARCH_MAX_SIZE);
        int safePage = Math.max(page, 0);
        if (query == null || query.isBlank()) {
            return ConversionHelper.toPage(List.of(), safePage, safeSize, 0);
        }
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Map<Long, Conversation> byId = new HashMap<>();
        if (conversationId != null) {
            // In-thread search: scope to one conversation the caller is part of.
            Conversation conversation = conversationApi.getCheckById(conversationId);
            requireParticipant(conversation, me);
            byId.put(conversation.getId(), conversation);
        } else {
            for (Conversation c : conversationApi.getForUser(me)) {
                byId.put(c.getId(), c);
            }
        }
        if (byId.isEmpty()) {
            return ConversionHelper.toPage(List.of(), safePage, safeSize, 0);
        }
        List<Long> convIds = new ArrayList<>(byId.keySet());
        String q = query.strip();
        long total = messageApi.searchCount(convIds, q);
        List<Message> hits = messageApi.searchPage(convIds, q, safePage, safeSize);
        // Resolve the other participant per hit (cached so repeated threads aren't an N+1).
        Map<Long, User> userCache = new HashMap<>();
        List<com.coduel.model.data.MessageSearchData> content = new ArrayList<>();
        for (Message m : hits) {
            Conversation c = byId.get(m.getConversationId());
            if (c == null) {
                continue;
            }
            Long otherId = c.getLowerUserId().equals(me) ? c.getHigherUserId() : c.getLowerUserId();
            User other = userCache.get(otherId);
            if (other == null) {
                other = userApi.getCheckById(otherId);
                userCache.put(otherId, other);
            }
            content.add(ConversionHelper.toMessageSearchData(m, other));
        }
        return ConversionHelper.toPage(content, safePage, safeSize, total);
    }

    public List<ConversationView> listConversations(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        // Batch the caller's per-thread settings once (peerId -> setting) so decorating the inbox with
        // nicknames / accents / archive state doesn't fan out into an N+1.
        Map<Long, ConversationSetting> settings = new HashMap<>();
        for (ConversationSetting setting : conversationSettingApi.getForOwner(me)) {
            settings.put(setting.getPeerUserId(), setting);
        }
        List<Conversation> conversations = conversationApi.getForUser(me);
        // Batch-load every "other user" once (id -> user) so decorating the inbox isn't an N+1.
        List<Long> otherIds = conversations.stream()
                .map(c -> c.getLowerUserId().equals(me) ? c.getHigherUserId() : c.getLowerUserId())
                .distinct()
                .toList();
        Map<Long, User> users = new HashMap<>();
        for (User u : userApi.getByIds(otherIds)) {
            users.put(u.getId(), u);
        }
        List<ConversationView> views = new ArrayList<>();
        for (Conversation conversation : conversations) {
            Long otherId = conversation.getLowerUserId().equals(me) ? conversation.getHigherUserId() : conversation.getLowerUserId();
            boolean unread = conversationApi.isUnreadFor(conversation, me);
            ConversationSetting setting = settings.get(otherId);
            // Archived threads stay hidden — UNLESS they've gone unread, so an incoming message is never
            // silently buried in the archive.
            if (setting != null && setting.isArchived() && !unread) {
                continue;
            }
            views.add(ConversionHelper.toConversationView(conversation, users.get(otherId), unread, setting));
        }
        return views;
    }

    // Ephemeral typing signal: gate on friendship and resolve the recipient, returning who to notify
    // + the payload (null when they're not friends). No persistence — the Dto just forwards it.
    public TypingSignalResult composeTyping(String senderGoogleId, Long recipientUserId) throws ApiException {
        User sender = userApi.getCheckByGoogleId(senderGoogleId);
        Friendship friendship = friendshipApi.findBetween(sender.getId(), recipientUserId);
        if (Objects.isNull(friendship) || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            return null; // only friends can see each other typing
        }
        String recipientGoogleId = userApi.getCheckById(recipientUserId).getGoogleId();
        return ConversionHelper.toTypingSignalResult(recipientGoogleId,
                ConversionHelper.toTypingData(sender.getId()));
    }

    // Mark a thread read for the caller (participants only) — persists their read marker and returns
    // who to notify (the other participant) + the receipt, so the Dto can push a live "Seen" update.
    public MarkReadResult markRead(String googleId, Long conversationId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationApi.getCheckById(conversationId);
        if (!conversation.getLowerUserId().equals(me) && !conversation.getHigherUserId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_133, List.of(conversationId));
        }
        Instant readAt = conversationApi.markRead(conversation, me);
        Long otherId = conversation.getLowerUserId().equals(me)
                ? conversation.getHigherUserId()
                : conversation.getLowerUserId();
        // Read marker is always persisted (so the caller's unread badge clears + survives reload). The Dto
        // always clears the sender's DM cue from the inbox (otherUserId below). But if the caller turned
        // OFF read receipts for this peer, leave the receipt null so the Dto skips the live "Seen" push.
        ConversationSetting mySetting = conversationSettingApi.find(me, otherId);
        if (mySetting != null && !mySetting.isReadReceiptsEnabled()) {
            return ConversionHelper.toMarkReadResult(null, otherId, null);
        }
        String otherGoogleId = userApi.getCheckById(otherId).getGoogleId();
        return ConversionHelper.toMarkReadResult(otherGoogleId, otherId,
                ConversionHelper.toReadReceiptData(conversationId, me, readAt.toEpochMilli()));
    }

    public List<MessageView> loadMessages(String googleId, Long conversationId, Long beforeId, Long afterId, int limit)
            throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationApi.getCheckById(conversationId);
        requireParticipant(conversation, me);
        // afterId → the next NEWER page (windowed scroll-down, chronological); otherwise the older page.
        List<Message> messages = afterId != null
                ? messageApi.getNewerPage(conversationId, afterId, limit)
                : messageApi.getPage(conversationId, beforeId, limit);
        // Batch-load this page's reactions in one query, grouped by message (no N+1).
        Map<Long, List<MessageReaction>> byMessage = new HashMap<>();
        for (MessageReaction reaction : messageReactionApi.getForMessages(messages.stream().map(Message::getId).toList())) {
            byMessage.computeIfAbsent(reaction.getMessageId(), k -> new ArrayList<>()).add(reaction);
        }
        // Batch-load the targets of any replies on this page, to build their quoted previews (one query).
        List<Long> replyIds = messages.stream().map(Message::getReplyToId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Message> repliesById = new HashMap<>();
        for (Message r : messageApi.getByIds(replyIds)) {
            repliesById.put(r.getId(), r);
        }
        return messages.stream()
                .map(m -> ConversionHelper.toMessageView(
                        m,
                        byMessage.getOrDefault(m.getId(), List.of()),
                        Objects.isNull(m.getReplyToId()) ? null : repliesById.get(m.getReplyToId())))
                .toList();
    }

    // Set (or change) the caller's reaction on a message — one per (message, user); re-reacting replaces
    // the emoji. Returns who to notify + the live event. Gated on conversation membership.
    public ReactionResult react(String googleId, Long messageId, String emoji) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        String clean = Objects.isNull(emoji) ? "" : emoji.strip();
        if (clean.isEmpty() || clean.length() > 16) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_137, List.of());
        }
        Conversation conversation = conversationOf(messageId);
        requireParticipant(conversation, me);
        // find-then-build spans two Api calls, so the entity is assembled here (flow) via ConversionHelper
        // and handed to the Api to persist; an existing reaction is just re-pointed at the new emoji.
        MessageReaction reaction = messageReactionApi.find(messageId, me);
        if (Objects.isNull(reaction)) {
            reaction = ConversionHelper.toMessageReaction(messageId, me, clean);
        } else {
            reaction.setEmoji(clean);
        }
        messageReactionApi.save(reaction);
        return reactionResult(conversation, me, messageId, clean, false);
    }

    // Clear the caller's reaction on a message (no-op if none). Returns the live "removed" event.
    public ReactionResult unreact(String googleId, Long messageId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationOf(messageId);
        requireParticipant(conversation, me);
        MessageReaction reaction = messageReactionApi.find(messageId, me);
        if (!Objects.isNull(reaction)) {
            messageReactionApi.delete(reaction);
        }
        return reactionResult(conversation, me, messageId, null, true);
    }

    private Conversation conversationOf(Long messageId) throws ApiException {
        return conversationApi.getCheckById(messageApi.getCheckById(messageId).getConversationId());
    }

    private ReactionResult reactionResult(Conversation conversation, Long me, Long messageId, String emoji,
                                          boolean removed) throws ApiException {
        Long otherId = conversation.getLowerUserId().equals(me)
                ? conversation.getHigherUserId()
                : conversation.getLowerUserId();
        String otherGoogleId = userApi.getCheckById(otherId).getGoogleId();
        return ConversionHelper.toReactionResult(otherGoogleId,
                ConversionHelper.toReactionEventData(conversation.getId(), messageId, me, emoji, removed));
    }

    // Edit one of the caller's own messages (not a deleted one). Dirty-checked on commit; returns the
    // peer + the live update to push.
    public MessageUpdateResult editMessage(String googleId, Long messageId, String body) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Message message = messageApi.getCheckById(messageId);
        if (!message.getSenderId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_139, List.of());
        }
        // Can't edit a deleted message, or one past the edit window (matches the client gating).
        boolean expired = message.getCreatedAt() != null
                && message.getCreatedAt().isBefore(Instant.now().minusSeconds(EDIT_WINDOW_SECONDS));
        if (!Objects.isNull(message.getDeletedAt()) || expired) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_140, List.of());
        }
        message.setBody(body);
        message.setEditedAt(Instant.now());
        Conversation conversation = conversationApi.getCheckById(message.getConversationId());
        // If this is the newest message, the inbox snapshot now shows a stale preview — recompute it.
        refreshSnapshotIfLatest(conversation, message);
        return ConversionHelper.toMessageUpdateResult(otherGoogleId(conversation, me),
                ConversionHelper.toMessageUpdateData(message));
    }

    // Soft-delete one of the caller's own messages (idempotent) — the row stays so replies keep their
    // quote, but the body is never served again.
    public MessageUpdateResult deleteMessage(String googleId, Long messageId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Message message = messageApi.getCheckById(messageId);
        if (!message.getSenderId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_139, List.of());
        }
        if (Objects.isNull(message.getDeletedAt())) {
            message.setDeletedAt(Instant.now());
        }
        Conversation conversation = conversationApi.getCheckById(message.getConversationId());
        // Deleting the newest message would leave a stale (now-blanked) preview in the inbox — refresh it.
        refreshSnapshotIfLatest(conversation, message);
        return ConversionHelper.toMessageUpdateResult(otherGoogleId(conversation, me),
                ConversionHelper.toMessageUpdateData(message));
    }

    // After a message is edited/deleted, if it's the thread's newest message refresh the denormalized
    // inbox snapshot (preview + sender) so the inbox doesn't show stale or deleted text. A deleted last
    // message shows a tombstone preview (consistent with how the client renders the deleted message).
    private void refreshSnapshotIfLatest(Conversation conversation, Message message) throws ApiException {
        Message latest = messageApi.getLatest(conversation.getId());
        if (latest == null || !latest.getId().equals(message.getId())) {
            return;
        }
        String preview = Objects.isNull(message.getDeletedAt()) ? previewOf(message) : DELETED_PREVIEW;
        conversationApi.refreshLastPreview(conversation, message.getSenderId(), preview);
    }

    // The inbox preview for a (non-deleted) message — mirrors the per-kind preview built in
    // sendDirectMessage: media/share get a label, code is summarized, text is the body itself.
    private String previewOf(Message message) throws ApiException {
        MessageKind kind = message.getKind();
        if (kind == MessageKind.IMAGE) {
            return "Photo";
        }
        if (kind == MessageKind.VOICE) {
            return "Voice message";
        }
        if (kind == MessageKind.PROBLEM_SHARE) {
            return "Shared: " + problemApi.getCheckBySlug(message.getSharedRef()).getTitle();
        }
        if (kind == MessageKind.CODE) {
            return "Code snippet";
        }
        return message.getBody();
    }

    private String otherGoogleId(Conversation conversation, Long me) throws ApiException {
        Long otherId = conversation.getLowerUserId().equals(me)
                ? conversation.getHigherUserId()
                : conversation.getLowerUserId();
        return userApi.getCheckById(otherId).getGoogleId();
    }

    // Pin a message (shared per conversation; either participant, no cap). Idempotent. Can't pin a
    // deleted message. Returns the peer + the live event.
    public PinResult pin(String googleId, Long messageId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Message message = messageApi.getCheckById(messageId);
        if (!Objects.isNull(message.getDeletedAt())) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_141, List.of());
        }
        Conversation conversation = conversationApi.getCheckById(message.getConversationId());
        requireParticipant(conversation, me);
        if (Objects.isNull(pinnedMessageApi.find(conversation.getId(), messageId))) {
            pinnedMessageApi.save(ConversionHelper.toPinnedMessage(conversation.getId(), messageId, me));
        }
        return ConversionHelper.toPinResult(otherGoogleId(conversation, me),
                ConversionHelper.toPinEventData(conversation.getId(), messageId, true,
                        ConversionHelper.toPinnedMessageData(message, me)));
    }

    public PinResult unpin(String googleId, Long messageId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Message message = messageApi.getCheckById(messageId);
        Conversation conversation = conversationApi.getCheckById(message.getConversationId());
        requireParticipant(conversation, me);
        PinnedMessage existing = pinnedMessageApi.find(conversation.getId(), messageId);
        if (!Objects.isNull(existing)) {
            pinnedMessageApi.delete(existing);
        }
        return ConversionHelper.toPinResult(otherGoogleId(conversation, me),
                ConversionHelper.toPinEventData(conversation.getId(), messageId, false, null));
    }

    // The conversation's pins (newest-first), each resolved to a preview. Stale pins whose message was
    // deleted are skipped.
    public List<PinnedMessageData> getPinned(String googleId, Long conversationId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationApi.getCheckById(conversationId);
        requireParticipant(conversation, me);
        List<PinnedMessage> pins = pinnedMessageApi.getForConversation(conversationId);
        Map<Long, Message> byId = new HashMap<>();
        for (Message m : messageApi.getByIds(pins.stream().map(PinnedMessage::getMessageId).toList())) {
            byId.put(m.getId(), m);
        }
        List<PinnedMessageData> out = new ArrayList<>();
        for (PinnedMessage p : pins) {
            Message m = byId.get(p.getMessageId());
            if (!Objects.isNull(m) && Objects.isNull(m.getDeletedAt())) {
                out.add(ConversionHelper.toPinnedMessageData(m, p.getPinnedByUserId()));
            }
        }
        return out;
    }

    private void requireParticipant(Conversation conversation, Long userId) throws ApiException {
        if (!conversation.getLowerUserId().equals(userId) && !conversation.getHigherUserId().equals(userId)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_133, List.of(conversation.getId()));
        }
    }

    // The caller's personalization for their thread with peerUserId — a transient defaults row when they
    // never customized it, so the client always gets a complete settings object (no special-casing null).
    public ConversationSetting getSettings(String googleId, Long peerUserId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        ConversationSetting setting = conversationSettingApi.find(me, peerUserId);
        return setting != null ? setting : ConversionHelper.toConversationSetting(me, peerUserId);
    }

    // Full-replace the caller's settings for peerUserId. peer must be a real user and not the caller; the
    // row is lazily created on first edit (a missing row meant "all defaults"). Optimistic locking on the
    // existing row guards concurrent edits from two devices.
    public ConversationSetting updateSettings(String googleId, Long peerUserId, ConversationSettingForm form) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        if (me.equals(peerUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_135, List.of());
        }
        userApi.getCheckById(peerUserId); // 404 if the peer doesn't exist
        ConversationSetting setting = conversationSettingApi.find(me, peerUserId);
        if (Objects.isNull(setting)) {
            setting = ConversionHelper.toConversationSetting(me, peerUserId);
        }
        ConversionHelper.applyConversationSettingForm(setting, form);
        return conversationSettingApi.save(setting);
    }

    // Has ownerUserId muted peerUserId? (Used to suppress the DM notification, not the message itself.)
    public boolean isMuted(Long ownerUserId, Long peerUserId) {
        ConversationSetting setting = conversationSettingApi.find(ownerUserId, peerUserId);
        return setting != null && setting.isMuted();
    }
}
