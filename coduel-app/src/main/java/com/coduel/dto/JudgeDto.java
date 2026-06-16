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
import com.coduel.model.result.JudgingInputs;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        JudgingInputs inputs = judgeFlow.loadInputs(submissionId);
        Submission submission = inputs.getSubmission();
        // Idempotent: a redelivered / double-dispatched submission that's already judged is a no-op.
        if (submission.getVerdict() != Verdict.PENDING) {
            log.info("Submission {} already judged ({}) — skipping", submissionId, submission.getVerdict());
            return;
        }
        long timeoutMs = resolveTimeoutMs(inputs.getProblem());

        int totalTests = inputs.getTestCases().size();
        int passedTests = 0;
        Verdict verdict = Verdict.ACCEPTED;
        long runtimeMs = 0;
        for (TestCase testCase : inputs.getTestCases()) {
            ExecResponse response = codeExecutor.run(ConversionHelper.convert(submission, testCase, timeoutMs));
            runtimeMs = Math.max(runtimeMs, response.getDurationMs());
            verdict = evaluate(response, testCase);
            if (verdict != Verdict.ACCEPTED) {
                break; // stop at the first failing test case
            }
            passedTests++;
        }

        submissionApi.updateVerdict(submissionId, verdict, runtimeMs, passedTests, totalTests);
        log.info("Judged submission {} -> {} ({}/{} tests, {} ms)",
                submissionId, verdict, passedTests, totalTests, runtimeMs);

        Long matchId = submission.getMatchId();
        if (Objects.nonNull(matchId)) {
            // live: both players watching the match see this submission's verdict
            matchEventPublisher.publish(matchId,
                    ConversionHelper.toSubmissionJudgedEvent(submission, verdict, passedTests, totalTests));

            Long winnerId = submissionFlow.getMatchWinner(matchId);
            if(Objects.nonNull(winnerId)){
                if(matchFlow.finish(matchId, winnerId, MatchEndReason.SOLVED)){
                    matchEventPublisher.publish(matchId,
                            ConversionHelper.toMatchOverEvent(winnerId, MatchEndReason.SOLVED));
                }
            }
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

    private Verdict evaluate(ExecResponse response, TestCase testCase) {
        if (response.isTimedOut()) {
            return Verdict.TIME_LIMIT_EXCEEDED;
        }
        if (response.getExitCode() != 0) {
            return Verdict.RUNTIME_ERROR;
        }
        // Simple comparison: ignore trailing whitespace/newline differences (robust diffing later).
        String actual = response.getStdout() == null ? "" : response.getStdout().stripTrailing();
        String expected = testCase.getExpectedOutput() == null ? "" : testCase.getExpectedOutput().stripTrailing();
        return actual.equals(expected) ? Verdict.ACCEPTED : Verdict.WRONG_ANSWER;
    }

    private long resolveTimeoutMs(Problem problem) {
        if (problem.getTimeLimitMs() == null) {
            return properties.getDefaultTimeoutMs();
        }
        return Math.clamp(problem.getTimeLimitMs(), 1, properties.getMaxTimeoutMs());
    }
}
