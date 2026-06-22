package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/**
 * POST /code/execute response (202): the run was queued. The client holds this {@code runId} and
 * matches it against the result pushed to /user/queue/run-result.
 */
@Getter
@Setter
public class RunAcceptedData {

    private String runId;
}
