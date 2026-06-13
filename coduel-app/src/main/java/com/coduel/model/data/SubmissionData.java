package com.coduel.model.data;

import com.coduel.execution.model.constant.Language;
import com.coduel.model.constant.Verdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionData {

    private Long submissionId;
    private Long userId;
    private Long problemId;
    private Long matchId;
    private Language language;
    private Verdict verdict;
    private Long runtimeMs;
}
