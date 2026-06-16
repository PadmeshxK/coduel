package com.coduel.helper;

import com.coduel.common.data.PageData;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Match;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.entity.User;
import com.coduel.entity.Leaderboard;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.MatchEventType;
import com.coduel.model.constant.MatchmakingStatus;
import com.coduel.model.constant.Verdict;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.data.MatchData;
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
import com.coduel.model.result.JudgingInputs;
import com.coduel.model.result.ProblemWithVisibleTestCases;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConversionHelper {

    public static ExecRequest convert(ExecutionForm form, long timeoutMs) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(form.getLanguage());
        request.setCode(form.getCode());
        request.setStdin(form.getStdin());
        request.setTimeout(Duration.ofMillis(timeoutMs));
        return request;
    }

    public static JudgingInputs toJudgingInputs(Submission submission, Problem problem, List<TestCase> testCases) {
        JudgingInputs inputs = new JudgingInputs();
        inputs.setSubmission(submission);
        inputs.setProblem(problem);
        inputs.setTestCases(testCases);
        return inputs;
    }

    public static ExecRequest convert(Submission submission, TestCase testCase, long timeoutMs) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(submission.getLanguage());
        request.setCode(submission.getSourceCode());
        request.setStdin(testCase.getInput());
        request.setTimeout(Duration.ofMillis(timeoutMs));
        return request;
    }

    public static ExecutionData convert(ExecResponse response) {
        ExecutionData data = new ExecutionData();
        data.setStdout(response.getStdout());
        data.setStderr(response.getStderr());
        data.setExitCode(response.getExitCode());
        data.setTimedOut(response.isTimedOut());
        data.setDurationMs(response.getDurationMs());
        return data;
    }

    public static Problem convert(ProblemForm form) {
        Problem problem = new Problem();
        problem.setSlug(form.getSlug());
        problem.setTitle(form.getTitle());
        problem.setStatement(form.getStatement());
        problem.setTimeLimitMs(form.getTimeLimitMs());
        return problem;
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

    public static List<Problem> toProblems(List<ProblemForm> forms) {
        return forms.stream().map(ConversionHelper::convert).toList();
    }

    public static List<List<TestCase>> toTestCaseGroups(List<ProblemForm> forms) {
        return forms.stream().map(ConversionHelper::toTestCases).toList();
    }

    public static List<ProblemData> toProblemDataList(List<ProblemWithVisibleTestCases> results) {
        return results.stream().map(ConversionHelper::convert).toList();
    }

    public static TestCaseData convert(TestCase testCase) {
        TestCaseData data = new TestCaseData();
        data.setInput(testCase.getInput());
        data.setExpectedOutput(testCase.getExpectedOutput());
        return data;
    }

    public static ProblemData convert(ProblemWithVisibleTestCases result) {
        Problem problem = result.getProblem();
        ProblemData data = new ProblemData();
        data.setId(problem.getId());
        data.setSlug(problem.getSlug());
        data.setTitle(problem.getTitle());
        data.setStatement(problem.getStatement());
        data.setTimeLimitMs(problem.getTimeLimitMs());
        data.setTestCases(result.getVisibleTestCases().stream().map(ConversionHelper::convert).toList());
        return data;
    }

    public static ProblemWithVisibleTestCases toResult(Problem problem, List<TestCase> visibleTestCases) {
        ProblemWithVisibleTestCases result = new ProblemWithVisibleTestCases();
        result.setProblem(problem);
        result.setVisibleTestCases(visibleTestCases);
        return result;
    }

    public static List<ProblemWithVisibleTestCases> pairWithVisibleTestCases(List<Problem> problems, List<TestCase> visibleTestCases) {
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

    public static MatchParticipantData toMatchParticipantData(User user) {
        MatchParticipantData data = new MatchParticipantData();
        data.setUserId(user.getId());
        data.setDisplayName(user.getDisplayName());
        data.setAvatarUrl(user.getAvatarUrl());
        return data;
    }

    public static MatchData toMatchData(Match match, Problem problem, List<MatchParticipantData> participants) {
        MatchData data = new MatchData();
        data.setMatchId(match.getId());
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
        return data;
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
