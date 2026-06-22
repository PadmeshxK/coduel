package com.coduel.rabbitmq;

public final class RabbitMQConstants {

    public static final String SUBMISSION_QUEUE = "submission.queue";
    public static final String CODUEL_EXCHANGE = "coduel.submissionExchange";

    // Ephemeral code-runs (the editor's Run button). Separate topology from submissions: results are
    // delivered live over WebSocket and the messages self-expire (no DLQ — a lost run is just re-run).
    public static final String RUN_QUEUE = "run.queue";
    public static final String RUN_EXCHANGE = "coduel.runExchange";

    // Dead-letter topology: messages that exhaust retries are parked here instead of being lost.
    public static final String DEAD_LETTER_EXCHANGE = "coduel.dlx";
    public static final String DEAD_LETTER_QUEUE = "submission.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "submission.dead";

    private  RabbitMQConstants() {
    }
}
