package com.coduel.dto;

import com.coduel.api.SubmissionApi;
import com.coduel.common.exception.ApiException;
import com.coduel.config.AppProperties;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.execution.interfaces.CodeExecutor;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.flow.JudgeFlow;
import com.coduel.flow.MatchFlow;
import com.coduel.flow.SubmissionFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.Verdict;
import com.coduel.model.result.JudgingInputResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates judging a submission, mirroring how SubmissionDto orchestrates the HTTP path:
 * load inputs (JudgeFlow, multi-Api), run the code against each test case (CodeExecutor, the
 * external call), persist the verdict (SubmissionApi). Not @Transactional — execution is slow,
 * so we never hold a DB transaction across it.
 */
@Component
@Log4j2
public class JudgeDto {

    @Autowired
    private JudgeFlow judgeFlow;
    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private SubmissionFlow submissionFlow;
    @Autowired
    private MatchEventPublisher matchEventPublisher;
    @Autowired
    private CodeExecutor codeExecutor;
    @Autowired
    private AppProperties properties;

    public void judge(Long submissionId) throws ApiException {
        JudgingInputResult inputs = judgeFlow.loadInputs(submissionId);
        Submission submission = inputs.getSubmission();

        // Idempotent: a redelivered / double-dispatched submission that's already judged is a no-op.
        if (submission.getVerdict() != Verdict.PENDING) {
            log.info("Submission {} already judged ({}) — skipping", submissionId, submission.getVerdict());
            return;
        }

        // One call does it all: compile once, run every test case, sum the runtimes, stop at the
        // first failure, and return a single summarized verdict.
        List<TestCase> testCases = inputs.getTestCases();
        long timeoutMs = resolveTimeoutMs(inputs.getProblem());
        ExecResponse result = codeExecutor.run(ConversionHelper.toExecRequest(submission, testCases, timeoutMs));

        Verdict verdict = ConversionHelper.toVerdict(result.getVerdict());
        int passedTests = result.getPassedTests();
        int totalTests = testCases.size();

        submissionApi.updateVerdict(submissionId, verdict, result.getDurationMs(), passedTests, totalTests);
        log.info("Judged submission {} -> {} ({}/{} tests, {} ms)",
                submissionId, verdict, passedTests, totalTests, result.getDurationMs());
        logFirstError(submissionId, verdict, result);

        if (Objects.nonNull(submission.getMatchId())) {
            broadcastToMatch(submission, verdict, passedTests, totalTests);
        }
    }

    // Live duel: push this submission's verdict to both players, and if it just won the match,
    // finish it and announce the winner — exactly once (finish() returns false if already ended).
    private void broadcastToMatch(Submission submission, Verdict verdict, int passedTests, int totalTests)
            throws ApiException {
        Long matchId = submission.getMatchId();
        matchEventPublisher.publish(matchId,
                ConversionHelper.toSubmissionJudgedEvent(submission, verdict, passedTests, totalTests));

        Long winnerId = submissionFlow.getMatchWinner(matchId);
        if (Objects.nonNull(winnerId) && matchFlow.finish(matchId, winnerId, MatchEndReason.SOLVED)) {
            matchEventPublisher.publish(matchId, ConversionHelper.toMatchOverEvent(winnerId, MatchEndReason.SOLVED));
        }
    }

    // Terminal failure path: judging never produced a verdict (DLQ exhaustion or orphan sweep).
    // Idempotent — only the PENDING -> INTERNAL_ERROR transition publishes, exactly once — so the
    // user (and the duel scoreboard) stops waiting and sees a definitive result.
    public void markInternalError(Long submissionId) throws ApiException {
        Submission submission = submissionApi.failIfPending(submissionId);
        if (Objects.isNull(submission)) {
            return; // already resolved by the real judge or a prior failure handler
        }
        log.warn("Submission {} -> INTERNAL_ERROR (judging did not complete)", submissionId);
        Long matchId = submission.getMatchId();
        if (Objects.nonNull(matchId)) {
            matchEventPublisher.publish(matchId,
                    ConversionHelper.toSubmissionJudgedEvent(submission, Verdict.INTERNAL_ERROR, 0, 0));
        }
    }

    // The first failing test case's detail now arrives alongside the verdict (compiler diagnostics
    // for COMPILE_ERROR, otherwise the program's stderr). Logged, not persisted — we don't surface
    // hidden test inputs to the opponent.
    private void logFirstError(Long submissionId, Verdict verdict, ExecResponse response) {
        if (verdict == Verdict.ACCEPTED) {
            return;
        }
        String detail = verdict == Verdict.COMPILE_ERROR ? response.getCompilerLogs() : response.getStderr();
        if (detail != null && !detail.isBlank()) {
            log.info("Submission {} first error ({}): {}", submissionId, verdict, abbreviate(detail));
        }
    }

    private String abbreviate(String text) {
        return text.length() <= 500 ? text : text.substring(0, 500) + "…";
    }

    private long resolveTimeoutMs(Problem problem) {
        if (problem.getTimeLimitMs() == null) {
            return properties.getDefaultTimeoutMs();
        }
        return Math.clamp(problem.getTimeLimitMs(), 1, properties.getMaxTimeoutMs());
    }
}
