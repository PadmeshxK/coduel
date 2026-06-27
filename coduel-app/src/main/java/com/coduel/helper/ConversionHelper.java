package com.coduel.helper;

import com.coduel.common.data.PageData;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Match;
import com.coduel.entity.Friendship;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.entity.Room;
import com.coduel.entity.RoomMember;
import com.coduel.entity.User;
import com.coduel.entity.Leaderboard;
import com.coduel.entity.Conversation;
import com.coduel.entity.ConversationSetting;
import com.coduel.entity.Message;
import com.coduel.entity.MessageReaction;
import com.coduel.entity.PinnedMessage;
import com.coduel.execution.model.constant.ExecutionVerdict;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.model.constant.*;
import com.coduel.model.data.ChallengeData;
import com.coduel.model.data.ConversationData;
import com.coduel.model.data.ConversationSettingData;
import com.coduel.model.data.MessageData;
import com.coduel.model.data.MessageSearchData;
import com.coduel.model.data.MessageUpdateData;
import com.coduel.model.data.PinEventData;
import com.coduel.model.data.PinnedMessageData;
import com.coduel.model.data.ReactionData;
import com.coduel.model.data.ReactionEventData;
import com.coduel.model.data.ReplyPreviewData;
import com.coduel.model.data.UploadData;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.data.FilterOptionsData;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.FriendRequestData;
import com.coduel.model.data.MatchData;
import com.coduel.model.data.NotificationData;
import com.coduel.model.data.PresenceData;
import com.coduel.model.data.ReadReceiptData;
import com.coduel.model.data.RoomChatData;
import com.coduel.model.data.TypingData;
import com.coduel.model.result.MarkReadResult;
import com.coduel.model.data.RunAcceptedData;
import com.coduel.model.message.RunTask;
import com.coduel.model.data.RoomData;
import com.coduel.model.data.RoomParticipantData;
import com.coduel.model.result.RoomDetailResult;
import com.coduel.model.data.RoomEventData;
import com.coduel.model.data.MatchEventData;
import com.coduel.model.data.MatchParticipantData;
import com.coduel.model.data.MatchmakingData;
import com.coduel.model.data.ProblemData;
import com.coduel.model.data.LeaderboardData;
import com.coduel.model.data.SubmissionData;
import com.coduel.model.data.TestCaseData;
import com.coduel.model.data.UserProfileData;
import com.coduel.model.form.ConversationSettingForm;
import com.coduel.model.form.ExecutionForm;
import com.coduel.model.form.ProblemForm;
import com.coduel.model.form.SubmissionForm;
import com.coduel.model.form.TestCaseForm;
import com.coduel.model.result.ChallengeResult;
import com.coduel.model.result.ConversationView;
import com.coduel.model.result.DmSentResult;
import com.coduel.model.result.MessageUpdateResult;
import com.coduel.model.result.MessageView;
import com.coduel.model.result.PinResult;
import com.coduel.model.result.ReactionResult;
import com.coduel.model.result.TypingSignalResult;
import com.coduel.model.result.FilterOptionsResult;
import com.coduel.model.result.FriendListResult;
import com.coduel.model.result.FriendResult;
import com.coduel.model.result.IncomingFriendRequestResult;
import com.coduel.model.result.JudgingInputResult;
import com.coduel.model.result.VisibleProblemResult;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConversionHelper {

    public static JudgingInputResult toJudgingInputResult(Submission submission, Problem problem, List<TestCase> testCases) {
        JudgingInputResult inputs = new JudgingInputResult();
        inputs.setSubmission(submission);
        inputs.setProblem(problem);
        inputs.setTestCases(testCases);
        return inputs;
    }

    // One execution request carrying the whole job: the source + every test case. The executor loops
    // them internally, so the judge no longer issues one request per test case.
    public static ExecRequest toExecRequest(Submission submission, List<TestCase> testCases, long timeoutMs) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(submission.getLanguage());
        request.setCode(submission.getSourceCode());
        request.setTestCases(testCases.stream().map(ConversionHelper::toExecTestCase).toList());
        request.setTimeout(Duration.ofMillis(timeoutMs));
        return request;
    }

    private static com.coduel.execution.model.request.TestCase toExecTestCase(TestCase testCase) {
        com.coduel.execution.model.request.TestCase execTestCase = new com.coduel.execution.model.request.TestCase();
        execTestCase.setInput(testCase.getInput());
        execTestCase.setExpectedOutput(testCase.getExpectedOutput());
        return execTestCase;
    }

    // Execution-engine verdict -> domain verdict. The shared names are intentionally identical; the
    // domain enum only adds PENDING / INTERNAL_ERROR, which the engine never returns.
    public static Verdict toVerdict(ExecutionVerdict verdict) {
        return Verdict.valueOf(verdict.name());
    }

    // Synchronous Run: build the same kind of request the judge uses, from the cases the UI sent.
    public static ExecRequest toExecRequest(ExecutionForm form, long timeoutMs) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(form.getLanguage());
        request.setCode(form.getCode());
        request.setTestCases(form.getTestCases().stream().map(ConversionHelper::toExecTestCase).toList());
        request.setTimeout(Duration.ofMillis(timeoutMs));
        return request;
    }

    private static com.coduel.execution.model.request.TestCase toExecTestCase(TestCaseForm testCase) {
        com.coduel.execution.model.request.TestCase execTestCase = new com.coduel.execution.model.request.TestCase();
        execTestCase.setInput(testCase.getInput());
        execTestCase.setExpectedOutput(testCase.getExpectedOutput());
        return execTestCase;
    }

    public static ExecutionData convert(ExecResponse response, int totalTests) {
        ExecutionData data = new ExecutionData();
        data.setVerdict(toVerdict(response.getVerdict()));
        data.setPassedTests(response.getPassedTests());
        data.setTotalTests(totalTests);
        data.setDurationMs(response.getDurationMs());
        data.setStdout(response.getStdout());
        data.setStderr(response.getStderr());
        data.setFailedInput(response.getFailedInput());
        data.setExpectedOutput(response.getExpectedOutput());
        data.setCompilerLogs(response.getCompilerLogs());
        return data;
    }

    // ---- async runs ----

    public static RunTask toRunTask(String runId, String googleId, ExecutionForm form, long timeoutMs) {
        RunTask task = new RunTask();
        task.setRunId(runId);
        task.setGoogleId(googleId);
        task.setLanguage(form.getLanguage());
        task.setCode(form.getCode());
        task.setTestCases(form.getTestCases());
        task.setTimeoutMs(timeoutMs);
        return task;
    }

    public static ExecRequest toExecRequest(RunTask task) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(task.getLanguage());
        request.setCode(task.getCode());
        request.setTestCases(task.getTestCases().stream().map(ConversionHelper::toExecTestCase).toList());
        request.setTimeout(Duration.ofMillis(task.getTimeoutMs()));
        return request;
    }

    public static RunAcceptedData toRunAcceptedData(String runId) {
        RunAcceptedData data = new RunAcceptedData();
        data.setRunId(runId);
        return data;
    }

    // A run that blew up in the worker — surfaced as a result (not an HTTP error) so the editor unblocks.
    public static ExecutionData toFailedRunResult(int totalTests) {
        ExecutionData data = new ExecutionData();
        data.setVerdict(Verdict.INTERNAL_ERROR);
        data.setPassedTests(0);
        data.setTotalTests(totalTests);
        return data;
    }

    // ---- chat / direct messages ----

    public static Message toMessage(Long conversationId, Long senderId, String body, Long replyToId,
                                    MessageKind kind, String codeLanguage, String attachmentUrl, String sharedRef,
                                    Integer durationMs) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setBody(body);
        message.setReplyToId(replyToId);
        message.setKind(kind == null ? MessageKind.TEXT : kind);
        message.setCodeLanguage(codeLanguage);
        message.setAttachmentUrl(attachmentUrl);
        message.setSharedRef(sharedRef);
        message.setDurationMs(durationMs);
        return message;
    }

    public static Conversation toConversation(Long lowerUserId, Long higherUserId) {
        Conversation conversation = new Conversation();
        conversation.setLowerUserId(lowerUserId);
        conversation.setHigherUserId(higherUserId);
        return conversation;
    }

    public static Room toRoom() {
        Room room = new Room();
        room.setState(RoomState.OPEN);
        room.setCreatedAtMs(Instant.now().toEpochMilli());
        return room;
    }

    public static RoomMember toRoomMember(Long userId) {
        RoomMember member = new RoomMember();
        member.setUserId(userId);
        return member;
    }

    public static MessageData toMessageData(Message message) {
        MessageData data = new MessageData();
        data.setMessageId(message.getId());
        data.setConversationId(message.getConversationId());
        data.setSenderId(message.getSenderId());
        boolean deleted = message.getDeletedAt() != null;
        data.setDeleted(deleted);
        data.setBody(deleted ? "" : message.getBody()); // never serve a deleted message's text
        data.setCreatedAtMs(message.getCreatedAt() != null ? message.getCreatedAt().toEpochMilli() : null);
        data.setEditedAtMs(message.getEditedAt() != null ? message.getEditedAt().toEpochMilli() : null);
        // A message is CODE only if it actually carries a language. This guards legacy rows (the kind
        // column was added later, so old messages can have a non-TEXT value but no language) from being
        // mis-rendered as code — they fall back to their original text body.
        boolean isCode = message.getKind() == MessageKind.CODE
                && message.getCodeLanguage() != null && !message.getCodeLanguage().isBlank();
        boolean isImage = message.getKind() == MessageKind.IMAGE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        boolean isProblem = message.getKind() == MessageKind.PROBLEM_SHARE
                && message.getSharedRef() != null && !message.getSharedRef().isBlank();
        boolean isVoice = message.getKind() == MessageKind.VOICE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        data.setKind(isCode ? MessageKind.CODE
                : isImage ? MessageKind.IMAGE
                : isProblem ? MessageKind.PROBLEM_SHARE
                : isVoice ? MessageKind.VOICE
                : MessageKind.TEXT);
        data.setCodeLanguage(isCode && !deleted ? message.getCodeLanguage() : null);
        // attachmentUrl backs both IMAGE and VOICE.
        data.setAttachmentUrl((isImage || isVoice) && !deleted ? message.getAttachmentUrl() : null);
        data.setSharedRef(isProblem && !deleted ? message.getSharedRef() : null);
        data.setDurationMs(isVoice && !deleted ? message.getDurationMs() : null);
        return data;
    }

    // A message with its reactions + replied-to quote attached (thread-page load, and the echoed/pushed
    // send). The bare overload above is used where neither applies.
    public static MessageData toMessageData(Message message, List<MessageReaction> reactions, Message replyTo) {
        MessageData data = toMessageData(message);
        if (data.isDeleted()) {
            return data; // a tombstone carries no reactions / quote
        }
        data.setReactions(reactions.stream().map(ConversionHelper::toReactionData).toList());
        data.setReplyToId(message.getReplyToId());
        if (replyTo != null) {
            data.setReplyTo(toReplyPreviewData(replyTo));
        }
        return data;
    }

    // A search hit: the matched message snippet + the thread (other participant) it belongs to.
    public static MessageSearchData toMessageSearchData(Message message, User other) {
        MessageSearchData data = new MessageSearchData();
        data.setMessageId(message.getId());
        data.setConversationId(message.getConversationId());
        data.setSenderId(message.getSenderId());
        String body = message.getBody() == null ? "" : message.getBody();
        data.setSnippet(body.length() <= 160 ? body : body.substring(0, 160));
        // Same guard as toMessageData: a kind only counts if its payload is actually present, so legacy
        // rows (kind column backfilled when added) don't mislabel plain text as code/image/etc.
        boolean isCode = message.getKind() == MessageKind.CODE
                && message.getCodeLanguage() != null && !message.getCodeLanguage().isBlank();
        boolean isImage = message.getKind() == MessageKind.IMAGE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        boolean isProblem = message.getKind() == MessageKind.PROBLEM_SHARE
                && message.getSharedRef() != null && !message.getSharedRef().isBlank();
        boolean isVoice = message.getKind() == MessageKind.VOICE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        data.setKind(isCode ? MessageKind.CODE : isImage ? MessageKind.IMAGE
                : isProblem ? MessageKind.PROBLEM_SHARE : isVoice ? MessageKind.VOICE : MessageKind.TEXT);
        data.setCreatedAtMs(message.getCreatedAt() != null ? message.getCreatedAt().toEpochMilli() : null);
        data.setOtherUserId(other.getId());
        data.setOtherDisplayName(other.getDisplayName());
        data.setOtherAvatarUrl(other.getAvatarUrl());
        return data;
    }

    public static MessageUpdateData toMessageUpdateData(Message message) {
        MessageUpdateData data = new MessageUpdateData();
        data.setConversationId(message.getConversationId());
        data.setMessageId(message.getId());
        boolean deleted = message.getDeletedAt() != null;
        data.setDeleted(deleted);
        data.setBody(deleted ? "" : message.getBody());
        data.setEditedAtMs(message.getEditedAt() != null ? message.getEditedAt().toEpochMilli() : null);
        return data;
    }

    public static MessageUpdateResult toMessageUpdateResult(String otherGoogleId, MessageUpdateData update) {
        MessageUpdateResult result = new MessageUpdateResult();
        result.setOtherGoogleId(otherGoogleId);
        result.setUpdate(update);
        return result;
    }

    public static PinnedMessage toPinnedMessage(Long conversationId, Long messageId, Long pinnedByUserId) {
        PinnedMessage pin = new PinnedMessage();
        pin.setConversationId(conversationId);
        pin.setMessageId(messageId);
        pin.setPinnedByUserId(pinnedByUserId);
        return pin;
    }

    public static PinnedMessageData toPinnedMessageData(Message message, Long pinnedByUserId) {
        PinnedMessageData data = new PinnedMessageData();
        data.setMessageId(message.getId());
        data.setSenderId(message.getSenderId());
        // Carry the kind so the client can render an icon + label; an image's body is only a caption,
        // code's body is source we don't dump into the pin bar.
        String body = message.getBody();
        boolean isImage = message.getKind() == MessageKind.IMAGE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        boolean isCode = message.getKind() == MessageKind.CODE
                && message.getCodeLanguage() != null && !message.getCodeLanguage().isBlank();
        boolean isProblem = message.getKind() == MessageKind.PROBLEM_SHARE
                && message.getSharedRef() != null && !message.getSharedRef().isBlank();
        boolean isVoice = message.getKind() == MessageKind.VOICE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        // Non-text previews are an icon + label on the client; only TEXT dumps its body.
        String preview = (isImage || isCode || isProblem || isVoice || body == null) ? "" : body;
        data.setKind(isImage ? MessageKind.IMAGE : isCode ? MessageKind.CODE
                : isProblem ? MessageKind.PROBLEM_SHARE : isVoice ? MessageKind.VOICE : MessageKind.TEXT);
        data.setPreview(preview.length() <= 140 ? preview : preview.substring(0, 140));
        data.setPinnedByUserId(pinnedByUserId);
        return data;
    }

    public static PinEventData toPinEventData(Long conversationId, Long messageId, boolean pinned, PinnedMessageData pin) {
        PinEventData data = new PinEventData();
        data.setConversationId(conversationId);
        data.setMessageId(messageId);
        data.setPinned(pinned);
        data.setPin(pin);
        return data;
    }

    public static PinResult toPinResult(String otherGoogleId, PinEventData event) {
        PinResult result = new PinResult();
        result.setOtherGoogleId(otherGoogleId);
        result.setEvent(event);
        return result;
    }

    public static UploadData toUploadData(String url) {
        UploadData data = new UploadData();
        data.setUrl(url);
        return data;
    }

    public static ReplyPreviewData toReplyPreviewData(Message message) {
        ReplyPreviewData data = new ReplyPreviewData();
        data.setMessageId(message.getId());
        data.setSenderId(message.getSenderId());
        // Carry the (guarded) kind so the quote can show an icon + label; only TEXT shows its body.
        boolean isCode = message.getKind() == MessageKind.CODE
                && message.getCodeLanguage() != null && !message.getCodeLanguage().isBlank();
        boolean isImage = message.getKind() == MessageKind.IMAGE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        boolean isProblem = message.getKind() == MessageKind.PROBLEM_SHARE
                && message.getSharedRef() != null && !message.getSharedRef().isBlank();
        boolean isVoice = message.getKind() == MessageKind.VOICE
                && message.getAttachmentUrl() != null && !message.getAttachmentUrl().isBlank();
        data.setKind(isCode ? MessageKind.CODE : isImage ? MessageKind.IMAGE
                : isProblem ? MessageKind.PROBLEM_SHARE : isVoice ? MessageKind.VOICE : MessageKind.TEXT);
        String body = message.getBody() == null ? "" : message.getBody();
        String preview = (isCode || isImage || isProblem || isVoice) ? "" : body;
        data.setPreview(preview.length() <= 140 ? preview : preview.substring(0, 140));
        return data;
    }

    public static ReactionData toReactionData(MessageReaction reaction) {
        ReactionData data = new ReactionData();
        data.setUserId(reaction.getUserId());
        data.setEmoji(reaction.getEmoji());
        return data;
    }

    public static MessageReaction toMessageReaction(Long messageId, Long userId, String emoji) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageId(messageId);
        reaction.setUserId(userId);
        reaction.setEmoji(emoji);
        return reaction;
    }

    public static MessageView toMessageView(Message message, List<MessageReaction> reactions, Message replyTo) {
        MessageView view = new MessageView();
        view.setMessage(message);
        view.setReactions(reactions);
        view.setReplyTo(replyTo);
        return view;
    }

    public static ReactionEventData toReactionEventData(Long conversationId, Long messageId, Long userId,
                                                        String emoji, boolean removed) {
        ReactionEventData data = new ReactionEventData();
        data.setConversationId(conversationId);
        data.setMessageId(messageId);
        data.setUserId(userId);
        data.setEmoji(emoji);
        data.setRemoved(removed);
        return data;
    }

    public static ReactionResult toReactionResult(String otherGoogleId, ReactionEventData event) {
        ReactionResult result = new ReactionResult();
        result.setOtherGoogleId(otherGoogleId);
        result.setEvent(event);
        return result;
    }

    public static ConversationData toConversationData(Conversation conversation, User other, boolean unread,
                                                      ConversationSetting setting) {
        ConversationData data = new ConversationData();
        data.setConversationId(conversation.getId());
        data.setOtherUserId(other.getId());
        data.setOtherDisplayName(other.getDisplayName());
        data.setOtherAvatarUrl(other.getAvatarUrl());
        data.setLastPreview(conversation.getLastPreview());
        data.setLastMessageAtMs(conversation.getLastMessageAt() != null
                ? conversation.getLastMessageAt().toEpochMilli() : null);
        data.setLastSenderId(conversation.getLastSenderId());
        data.setUnread(unread);
        // The other participant's read marker, so the thread can show how far they've read ("Seen").
        java.time.Instant otherRead = other.getId().equals(conversation.getLowerUserId())
                ? conversation.getLowerUserLastReadAt()
                : conversation.getHigherUserLastReadAt();
        data.setOtherLastReadAtMs(otherRead != null ? otherRead.toEpochMilli() : null);
        // The viewer's personalization for this row (null setting = defaults).
        data.setNickname(setting != null ? setting.getNickname() : null);
        data.setAccentHex(setting != null ? setting.getAccentHex() : null);
        data.setMuted(setting != null && setting.isMuted());
        return data;
    }

    public static ReadReceiptData toReadReceiptData(Long conversationId, Long readerUserId, Long readAtMs) {
        ReadReceiptData data = new ReadReceiptData();
        data.setConversationId(conversationId);
        data.setReaderUserId(readerUserId);
        data.setReadAtMs(readAtMs);
        return data;
    }

    // otherUserId is always set (the Dto clears that sender's DM cue from the reader's inbox); receipt
    // is null when the reader has read receipts off (no live "Seen" push), otherGoogleId then unused.
    public static MarkReadResult toMarkReadResult(String otherGoogleId, Long otherUserId, ReadReceiptData receipt) {
        MarkReadResult result = new MarkReadResult();
        result.setOtherGoogleId(otherGoogleId);
        result.setOtherUserId(otherUserId);
        result.setReceipt(receipt);
        return result;
    }

    public static DmSentResult toDmSentResult(Message message, String recipientGoogleId, User sender, Message replyTo) {
        DmSentResult result = new DmSentResult();
        result.setMessage(message);
        result.setRecipientGoogleId(recipientGoogleId);
        result.setSender(sender);
        result.setReplyTo(replyTo);
        return result;
    }

    public static PresenceData toPresenceData(Long userId, boolean online) {
        PresenceData data = new PresenceData();
        data.setUserId(userId);
        data.setOnline(online);
        return data;
    }

    public static TypingData toTypingData(Long fromUserId) {
        TypingData data = new TypingData();
        data.setFromUserId(fromUserId);
        return data;
    }

    public static TypingSignalResult toTypingSignalResult(String recipientGoogleId, TypingData typing) {
        TypingSignalResult result = new TypingSignalResult();
        result.setRecipientGoogleId(recipientGoogleId);
        result.setTyping(typing);
        return result;
    }

    public static RoomChatData toRoomChatData(User sender, String body) {
        RoomChatData data = new RoomChatData();
        data.setSenderId(sender.getId());
        data.setSenderName(sender.getDisplayName());
        data.setSenderAvatarUrl(sender.getAvatarUrl());
        data.setBody(body);
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // The inbox key for a sender's DM cue — one entry per sender, added on receive, removed on read.
    public static String dmNotificationId(Long senderUserId) {
        return "dm:" + senderUserId;
    }

    // Pushed to the recipient's notification queue when a DM arrives — a toast cue ("from" = the sender),
    // clicked to open the thread. Not an actionable bell item; the conversation lives on the Messages page.
    public static NotificationData toDmNotification(User sender, MessageKind messageKind) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.DM_RECEIVED);
        // One inbox entry per sender (keyed dm:<senderId>) — repeated DMs collapse onto it. No time
        // expiry: a DM persists until the thread is READ (removed then). Matches the client's
        // notificationKey for DM_RECEIVED.
        data.setId(dmNotificationId(sender.getId()));
        data.setFromUserId(sender.getId());
        data.setFromDisplayName(sender.getDisplayName());
        data.setFromAvatarUrl(sender.getAvatarUrl());
        data.setMessageKind(messageKind);
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    public static ConversationView toConversationView(Conversation conversation, User other, boolean unread,
                                                      ConversationSetting setting) {
        ConversationView view = new ConversationView();
        view.setConversation(conversation);
        view.setOther(other);
        view.setUnread(unread);
        view.setSetting(setting);
        return view;
    }

    // A fresh, unsaved settings row carrying all the defaults (via the entity's field initializers). Used
    // both for "never customized" reads and as the base a first edit mutates before persisting.
    public static ConversationSetting toConversationSetting(Long ownerUserId, Long peerUserId) {
        ConversationSetting setting = new ConversationSetting();
        setting.setOwnerUserId(ownerUserId);
        setting.setPeerUserId(peerUserId);
        return setting;
    }

    // Full-replace the personalization from the form (validated upstream). quickReactionEmoji is only
    // overwritten when non-blank so a client that omits it keeps the current emoji rather than clearing it.
    public static void applyConversationSettingForm(ConversationSetting setting, ConversationSettingForm form) {
        setting.setThemeMode(form.getThemeMode());
        setting.setAccentHex(form.getAccentHex());
        setting.setBackgroundPreset(form.getBackgroundPreset());
        setting.setBackgroundImageUrl(form.getBackgroundImageUrl());
        setting.setBackgroundDim(form.getBackgroundDim());
        setting.setBackgroundBlur(form.getBackgroundBlur());
        setting.setBubbleStyle(form.getBubbleStyle());
        setting.setMessageFont(form.getMessageFont());
        setting.setMessageTextSize(form.getMessageTextSize() != null
                ? form.getMessageTextSize() : com.coduel.model.constant.MessageTextSize.MEDIUM);
        setting.setMessageDensity(form.getMessageDensity() != null
                ? form.getMessageDensity() : com.coduel.model.constant.MessageDensity.COZY);
        setting.setNickname(form.getNickname());
        if (form.getQuickReactionEmoji() != null && !form.getQuickReactionEmoji().isBlank()) {
            setting.setQuickReactionEmoji(form.getQuickReactionEmoji());
        }
        setting.setReadReceiptsEnabled(form.isReadReceiptsEnabled());
        setting.setMuted(form.isMuted());
        setting.setArchived(form.isArchived());
        // Stamp the enable time on the off→on transition (and clear it on on→off) so the sweep only
        // affects messages sent while disappearing was on. A TTL change while it stays on keeps the
        // original enable instant.
        Integer oldTtl = setting.getDisappearingTtlSeconds();
        Integer newTtl = form.getDisappearingTtlSeconds();
        if (newTtl != null && oldTtl == null) {
            setting.setDisappearingEnabledAt(Instant.now());
        } else if (newTtl == null) {
            setting.setDisappearingEnabledAt(null);
        }
        setting.setDisappearingTtlSeconds(newTtl);
    }

    public static ConversationSettingData toConversationSettingData(ConversationSetting setting) {
        ConversationSettingData data = new ConversationSettingData();
        data.setPeerUserId(setting.getPeerUserId());
        data.setThemeMode(setting.getThemeMode().name());
        data.setAccentHex(setting.getAccentHex());
        data.setBackgroundPreset(setting.getBackgroundPreset().name());
        data.setBackgroundImageUrl(setting.getBackgroundImageUrl());
        data.setBackgroundDim(setting.getBackgroundDim());
        data.setBackgroundBlur(setting.getBackgroundBlur());
        data.setBubbleStyle(setting.getBubbleStyle().name());
        data.setMessageFont(setting.getMessageFont().name());
        // Legacy rows (column added later) can be null → MEDIUM.
        data.setMessageTextSize((setting.getMessageTextSize() != null
                ? setting.getMessageTextSize() : com.coduel.model.constant.MessageTextSize.MEDIUM).name());
        data.setMessageDensity((setting.getMessageDensity() != null
                ? setting.getMessageDensity() : com.coduel.model.constant.MessageDensity.COZY).name());
        data.setNickname(setting.getNickname());
        data.setQuickReactionEmoji(setting.getQuickReactionEmoji());
        data.setReadReceiptsEnabled(setting.isReadReceiptsEnabled());
        data.setMuted(setting.isMuted());
        data.setArchived(setting.isArchived());
        data.setDisappearingTtlSeconds(setting.getDisappearingTtlSeconds());
        return data;
    }

    public static Problem convert(ProblemForm form) {
        Problem problem = new Problem();
        problem.setSlug(form.getSlug());
        problem.setTitle(form.getTitle());
        problem.setStatement(form.getStatement());
        problem.setTimeLimitMs(form.getTimeLimitMs());
        problem.setRating(form.getRating());
        if (form.getTags() != null) {
            problem.setTags(form.getTags());
        }
        return problem;
    }

    public static Friendship convert(Long requesterId, Long addresseeId) {
        Friendship friendship = new Friendship();
        friendship.setRequesterId(requesterId);
        friendship.setAddresseeId(addresseeId);
        friendship.setStatus(FriendshipStatus.PENDING);
        return friendship;
    }

    public static TestCase convert(TestCaseForm form) {
        TestCase testCase = new TestCase();
        testCase.setInput(form.getInput());
        testCase.setExpectedOutput(form.getExpectedOutput());
        testCase.setHidden(form.isHidden());
        return testCase;
    }

    // ---- batch helpers: keep the form→entity stream plumbing out of the Dto ----

    public static List<TestCase> toTestCases(ProblemForm form) {
        return form.getTestCases().stream().map(ConversionHelper::convert).toList();
    }

    public static FilterOptionsResult toFilterOptionsResult(List<Integer> ratings, List<String> tags) {
        FilterOptionsResult result = new FilterOptionsResult();
        result.setRatings(ratings);
        result.setTags(tags);
        return result;
    }

    public static FilterOptionsData toFilterOptionsData(FilterOptionsResult result) {
        FilterOptionsData data = new FilterOptionsData();
        data.setRatings(result.getRatings());
        data.setTags(result.getTags());
        return data;
    }

    public static List<Problem> toProblems(List<ProblemForm> forms) {
        return forms.stream().map(ConversionHelper::convert).toList();
    }

    public static List<List<TestCase>> toTestCaseGroups(List<ProblemForm> forms) {
        return forms.stream().map(ConversionHelper::toTestCases).toList();
    }

    public static List<ProblemData> toProblemDataList(List<VisibleProblemResult> results) {
        return results.stream().map(ConversionHelper::convert).toList();
    }

    public static TestCaseData convert(TestCase testCase) {
        TestCaseData data = new TestCaseData();
        data.setInput(testCase.getInput());
        data.setExpectedOutput(testCase.getExpectedOutput());
        return data;
    }

    public static ProblemData convert(VisibleProblemResult result) {
        Problem problem = result.getProblem();
        ProblemData data = new ProblemData();
        data.setId(problem.getId());
        data.setSlug(problem.getSlug());
        data.setTitle(problem.getTitle());
        data.setStatement(problem.getStatement());
        data.setTimeLimitMs(problem.getTimeLimitMs());
        data.setRating(problem.getRating());
        data.setTags(problem.getTags());
        data.setTestCases(result.getVisibleTestCases().stream().map(ConversionHelper::convert).toList());
        data.setStatus(result.getStatus());
        data.setSolved(result.isSolved());
        if (result.getSubmissions() != null) {
            data.setSubmissions(result.getSubmissions().stream().map(ConversionHelper::convert).toList());
        }
        return data;
    }

    public static VisibleProblemResult toResult(Problem problem, List<TestCase> visibleTestCases) {
        VisibleProblemResult result = new VisibleProblemResult();
        result.setProblem(problem);
        result.setVisibleTestCases(visibleTestCases);
        return result;
    }

    public static List<VisibleProblemResult> pairWithVisibleTestCases(List<Problem> problems, List<TestCase> visibleTestCases) {
        Map<Long, List<TestCase>> byProblemId = visibleTestCases.stream()
                .collect(Collectors.groupingBy(TestCase::getProblemId));
        return problems.stream()
                .map(problem -> toResult(problem, byProblemId.getOrDefault(problem.getId(), List.of())))
                .toList();
    }

    public static Submission convert(SubmissionForm form) {
        Submission submission = new Submission();
        submission.setProblemId(form.getProblemId());
        submission.setMatchId(form.getMatchId());
        submission.setLanguage(form.getLanguage());
        submission.setSourceCode(form.getSourceCode());
        return submission;
    }

    public static SubmissionData convert(Submission submission) {
        SubmissionData data = new SubmissionData();
        data.setSubmissionId(submission.getId());
        data.setUserId(submission.getUserId());
        data.setProblemId(submission.getProblemId());
        data.setMatchId(submission.getMatchId());
        data.setLanguage(submission.getLanguage());
        data.setVerdict(submission.getVerdict());
        data.setRuntimeMs(submission.getRuntimeMs());
        data.setPassedTests(submission.getPassedTests());
        data.setTotalTests(submission.getTotalTests());
        data.setCreatedAtMs(submission.getCreatedAt() != null ? submission.getCreatedAt().toEpochMilli() : null);
        data.setSourceCode(submission.getSourceCode());
        return data;
    }

    public static Match toMatch(GameMode gameMode, Long problemId, MatchState state) {
        Match match = new Match();
        match.setGameMode(gameMode);
        match.setProblemId(problemId);
        match.setState(state);
        return match;
    }

    public static MatchParticipant toParticipant(Long matchId, Long userId) {
        MatchParticipant participant = new MatchParticipant();
        participant.setMatchId(matchId);
        participant.setUserId(userId);
        return participant;
    }

    public static MatchEventData toSubmissionJudgedEvent(Submission submission, Verdict verdict,
                                                         int passedTests, int totalTests) {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.SUBMISSION_JUDGED);
        event.setSubmissionId(submission.getId());
        event.setUserId(submission.getUserId());
        event.setVerdict(verdict);
        event.setPassedTests(passedTests);
        event.setTotalTests(totalTests);
        return event;
    }

    public static MatchEventData toMatchReadyEvent() {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.MATCH_READY);
        return event;
    }

    public static MatchEventData toMatchOverEvent(Long winnerUserId, MatchEndReason reason) {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.MATCH_OVER);
        event.setWinnerUserId(winnerUserId);
        event.setEndReason(reason);
        return event;
    }

    // A player forfeited while the match plays on — carries who dropped out so the scoreboard updates.
    public static MatchEventData toPlayerForfeitEvent(Long userId) {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.PLAYER_FORFEIT);
        event.setUserId(userId);
        return event;
    }

    public static MatchParticipantData toMatchParticipantData(MatchParticipant participant, User user) {
        MatchParticipantData data = new MatchParticipantData();
        data.setUserId(participant.getUserId());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        data.setForfeit(participant.isForfeit());
        return data;
    }

    public static MatchData toMatchData(Match match, Problem problem, List<MatchParticipantData> participants) {
        MatchData data = new MatchData();
        data.setMatchId(match.getId());
        data.setRoomId(match.getRoomId());
        data.setState(match.getState());
        data.setSlug(problem.getSlug());
        data.setProblemTitle(problem.getTitle());
        data.setWinnerUserId(match.getWinnerUserId());
        data.setEndReason(match.getEndReason());
        // createdAt is the match start (see Match entity); endedAt is null while live.
        data.setStartedAtMs(match.getCreatedAt() != null ? match.getCreatedAt().toEpochMilli() : null);
        data.setEndedAtMs(match.getEndedAt() != null ? match.getEndedAt().toEpochMilli() : null);
        data.setParticipants(participants);
        return data;
    }

    public static MatchmakingData toMatchmakingData(MatchmakingStatus status, Match match, String slug) {
        MatchmakingData data = new MatchmakingData();
        data.setStatus(status);
        data.setSlug(slug);
        if (match != null) {
            data.setMatchId(match.getId());
            data.setProblemId(match.getProblemId());
        }
        return data;
    }

    public static Leaderboard toLeaderboard(Long userId) {
        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setUserId(userId);
        return leaderboard;
    }

    public static LeaderboardData toLeaderboardData(User user, Leaderboard leaderboard) {
        LeaderboardData data = new LeaderboardData();
        data.setUserId(user.getId());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        data.setWins(leaderboard.getWins());
        data.setLosses(leaderboard.getLosses());
        return data;
    }

    public static UserProfileData toUserProfileData(User user) {
        UserProfileData data = new UserProfileData();
        data.setId(user.getId());
        data.setEmail(user.getEmail());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        data.setDisplayNameSet(user.isDisplayNameSet());
        return data;
    }

    public static FriendData toFriendData(User user) {
        FriendData data = new FriendData();
        data.setUserId(user.getId());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        return data;
    }

    // Search hit + relationship flags (friend / pending) for the directory action.
    public static FriendData toFriendData(FriendResult result) {
        FriendData data = toFriendData(result.getUser());
        data.setFriend(result.isFriend());
        data.setPending(result.isPending());
        return data;
    }

    public static FriendListResult toFriendListResult(User friend, Long sinceMs) {
        FriendListResult result = new FriendListResult();
        result.setFriend(friend);
        result.setSinceMs(sinceMs);
        return result;
    }

    // Friend-list row: the friend's public view plus when the friendship began ("Friends for…").
    public static FriendData toFriendData(FriendListResult result) {
        FriendData data = toFriendData(result.getFriend());
        data.setFriendsSinceMs(result.getSinceMs());
        return data;
    }

    public static FriendRequestData toFriendRequestData(IncomingFriendRequestResult view) {
        User requester = view.getRequester();
        Friendship friendship = view.getFriendship();
        FriendRequestData data = new FriendRequestData();
        data.setRequestId(friendship.getId());
        data.setUserId(requester.getId());
        data.setDisplayName(requester.getDisplayName());
        data.setAvatarUrl(requester.getAvatarUrl());
        data.setCreatedAtMs(friendship.getCreatedAt() != null ? friendship.getCreatedAt().toEpochMilli() : null);
        return data;
    }

    public static RoomData toRoomData(RoomDetailResult view) {
        RoomData data = new RoomData();
        data.setRoomId(view.getRoom().getId());
        data.setState(view.getRoom().getState());
        data.setHost(view.getRequestingUserId().equals(view.getHostId()));
        data.setMaxPlayers(com.coduel.flow.RoomFlow.MAX_ROOM_PLAYERS);
        data.setActiveMatchId(view.getActiveMatchId());
        data.setParticipants(view.getMembers().stream()
                .map(m -> toRoomParticipantData(m, view.getProfiles().get(m.getUserId()), view.getHostId()))
                .toList());
        return data;
    }

    public static RoomParticipantData toRoomParticipantData(RoomMember member, User user, Long hostId) {
        RoomParticipantData data = new RoomParticipantData();
        data.setUserId(member.getUserId());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        boolean host = member.getUserId().equals(hostId);
        data.setHost(host);
        // Host is implicitly ready (starting is their signal); others reflect their own flag.
        data.setReady(host || member.isReady());
        return data;
    }

    // Lifetimes for the (single) NotificationStore — enforced logically on read, so they can differ.
    private static final long ROOM_INVITE_TTL_MS = 60 * 60 * 1000L;   // 1h: a lobby stays open a while
    private static final long DUEL_CHALLENGE_TTL_MS = 90 * 1000L;     // 90s: no answer == declined

    // Store key for a room invite — shared by the builder and the remover so they never drift.
    public static String roomNotificationId(Long roomId) {
        return "room:" + roomId;
    }

    public static NotificationData toRoomInviteNotification(Long roomId, User fromUser) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.ROOM_INVITE);
        data.setId(roomNotificationId(roomId));
        data.setRoomId(roomId);
        data.setFromUserId(fromUser.getId());
        data.setFromDisplayName(fromUser.getDisplayName());
        data.setFromAvatarUrl(fromUser.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        data.setExpiresAtMs(Instant.now().toEpochMilli() + ROOM_INVITE_TTL_MS);
        return data;
    }

    public static NotificationData toFriendRequestNotification(Friendship friendship, User requester) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.FRIEND_REQUEST);
        data.setRequestId(friendship.getId());
        data.setFromUserId(requester.getId());
        data.setFromDisplayName(requester.getDisplayName());
        data.setFromAvatarUrl(requester.getAvatarUrl());
        data.setCreatedAtMs(friendship.getCreatedAt() != null ? friendship.getCreatedAt().toEpochMilli() : null);
        return data;
    }

    // Pushed to the requester when the addressee accepts — "from" is the acceptor. No requestId
    // (it's not actionable), just a live "you're now friends" cue.
    public static NotificationData toFriendAcceptedNotification(User acceptor) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.FRIEND_ACCEPTED);
        data.setFromUserId(acceptor.getId());
        data.setFromDisplayName(acceptor.getDisplayName());
        data.setFromAvatarUrl(acceptor.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // Silent push to the requester when their request is declined — "from" is the decliner. The
    // client uses it only to revert the "Requested" button to "Add"; it shows no toast.
    public static NotificationData toFriendDeclinedNotification(User decliner) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.FRIEND_DECLINED);
        data.setFromUserId(decliner.getId());
        data.setFromDisplayName(decliner.getDisplayName());
        data.setFromAvatarUrl(decliner.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // ---- duel challenges ----

    // One carrier for every challenge action: opponent = the other party (target on create, accepter
    // /decliner otherwise); challengeId set on create, matchId on accept.
    public static ChallengeResult toChallengeResult(String challengeId, Long matchId,
                                                    User challenger, User opponent) {
        ChallengeResult result = new ChallengeResult();
        result.setChallengeId(challengeId);
        result.setMatchId(matchId);
        result.setChallenger(challenger);
        result.setOpponent(opponent);
        return result;
    }

    public static ChallengeData toChallengeData(String challengeId, Long matchId, String opponentDisplayName) {
        ChallengeData data = new ChallengeData();
        data.setChallengeId(challengeId);
        data.setMatchId(matchId);
        data.setOpponentDisplayName(opponentDisplayName);
        return data;
    }

    // Pushed to the target — actionable accept/decline, carries the challengeId.
    public static NotificationData toDuelChallengeNotification(String challengeId, User challenger, String problemSlug) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.DUEL_CHALLENGE);
        data.setId(challengeId);
        data.setChallengeId(challengeId);
        data.setProblemSlug(problemSlug);
        data.setFromUserId(challenger.getId());
        data.setFromDisplayName(challenger.getDisplayName());
        data.setFromAvatarUrl(challenger.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        data.setExpiresAtMs(Instant.now().toEpochMilli() + DUEL_CHALLENGE_TTL_MS);
        return data;
    }

    // Pushed to BOTH players when a challenge is accepted — carries the matchId to jump into.
    public static NotificationData toChallengeAcceptedNotification(Long matchId, User fromUser) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.CHALLENGE_ACCEPTED);
        data.setMatchId(matchId);
        data.setFromUserId(fromUser.getId());
        data.setFromDisplayName(fromUser.getDisplayName());
        data.setFromAvatarUrl(fromUser.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // Pushed to both players when ranked matchmaking pairs them — carries the matchId to jump into.
    public static NotificationData toMatchmakingFoundNotification(Long matchId) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.MATCHMAKING_FOUND);
        data.setMatchId(matchId);
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // Pushed to the challenger when their challenge is declined.
    public static NotificationData toChallengeDeclinedNotification(User fromUser) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.CHALLENGE_DECLINED);
        data.setFromUserId(fromUser.getId());
        data.setFromDisplayName(fromUser.getDisplayName());
        data.setFromAvatarUrl(fromUser.getAvatarUrl());
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // Live signal to the challenged user that the challenger withdrew — carries the challengeId so the
    // client drops that exact pending popup/bell row (it was also removed from their inbox server-side).
    public static NotificationData toChallengeWithdrawnNotification(String challengeId) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.CHALLENGE_WITHDRAWN);
        data.setChallengeId(challengeId);
        data.setCreatedAtMs(Instant.now().toEpochMilli());
        return data;
    }

    // ---- room (persistent lobby) topic events ----

    public static RoomEventData toRoomRosterChanged() {
        RoomEventData event = new RoomEventData();
        event.setType(RoomEventType.ROSTER_CHANGED);
        return event;
    }

    public static RoomEventData toRoomMatchStarted(Long matchId) {
        RoomEventData event = new RoomEventData();
        event.setType(RoomEventType.MATCH_STARTED);
        event.setMatchId(matchId);
        return event;
    }

    public static RoomEventData toRoomClosed() {
        RoomEventData event = new RoomEventData();
        event.setType(RoomEventType.ROOM_CLOSED);
        return event;
    }

    public static <T> PageData<T> toPage(List<T> content, int page, int size, long totalElements) {
        PageData<T> data = new PageData<>();
        data.setContent(content);
        data.setPage(page);
        data.setSize(size);
        data.setTotalElements(totalElements);
        data.setTotalPages(size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size));
        return data;
    }
}
