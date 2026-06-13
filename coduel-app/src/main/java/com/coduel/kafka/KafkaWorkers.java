package com.coduel.kafka;

/**
 * Kafka consumer-group ids — a fixed identity (not env config, not a topic), so a constant.
 * All judge-worker instances share this group so Kafka splits the partitions among them and
 * no submission is judged twice.
 */
public final class KafkaWorkers {

    public static final String JUDGE_CONSUMER = "judge.consumer";

    private KafkaWorkers() {
    }
}
