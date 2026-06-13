package com.coduel.helper;

import com.coduel.common.data.PageData;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Match;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.MatchEventType;
import com.coduel.model.constant.MatchmakingStatus;
import com.coduel.model.constant.Verdict;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.data.MatchEventData;
import com.coduel.model.data.MatchmakingData;
import com.coduel.model.data.ProblemData;
import com.coduel.model.data.SubmissionData;
import com.coduel.model.data.TestCaseData;
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

    public static TestCaseData convert(TestCase testCase) {
        TestCaseData data = new TestCaseData();
        data.setInput(testCase.getInput());
        data.setExpectedOutput(testCase.getExpectedOutput());
        return data;
    }

    public static ProblemData convert(ProblemWithVisibleTestCases result) {
        Problem problem = result.getProblem();
        ProblemData data = new ProblemData();
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

    public static MatchEventData toSubmissionJudgedEvent(Submission submission, Verdict verdict) {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.SUBMISSION_JUDGED);
        event.setSubmissionId(submission.getId());
        event.setUserId(submission.getUserId());
        event.setVerdict(verdict);
        return event;
    }

    public static MatchEventData toMatchOverEvent(Long winnerUserId) {
        MatchEventData event = new MatchEventData();
        event.setType(MatchEventType.MATCH_OVER);
        event.setWinnerUserId(winnerUserId);
        return event;
    }

    public static MatchmakingData toMatchmakingData(MatchmakingStatus status, Match match) {
        MatchmakingData data = new MatchmakingData();
        data.setStatus(status);
        if (match != null) {
            data.setMatchId(match.getId());
            data.setProblemId(match.getProblemId());
        }
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
