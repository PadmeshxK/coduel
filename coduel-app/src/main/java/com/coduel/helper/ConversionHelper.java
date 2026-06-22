package com.coduel.helper;

import com.coduel.common.data.PageData;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Match;
import com.coduel.entity.Friendship;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.entity.RoomMember;
import com.coduel.entity.User;
import com.coduel.entity.Leaderboard;
import com.coduel.execution.model.constant.ExecutionVerdict;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.model.constant.*;
import com.coduel.model.data.ChallengeData;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.data.FilterOptionsData;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.FriendRequestData;
import com.coduel.model.data.MatchData;
import com.coduel.model.data.NotificationData;
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
import com.coduel.model.form.ExecutionForm;
import com.coduel.model.form.ProblemForm;
import com.coduel.model.form.SubmissionForm;
import com.coduel.model.form.TestCaseForm;
import com.coduel.model.result.ChallengeResult;
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
    public static NotificationData toDuelChallengeNotification(String challengeId, User challenger) {
        NotificationData data = new NotificationData();
        data.setType(NotificationEventType.DUEL_CHALLENGE);
        data.setId(challengeId);
        data.setChallengeId(challengeId);
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
