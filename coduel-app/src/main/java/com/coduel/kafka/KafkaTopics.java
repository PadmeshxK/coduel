package com.coduel.kafka;

/**
 * Kafka topic names — a FIXED contract between the producer and the judge worker, so they
 * live as constants in code, never as external config. If the topic name were configurable,
 * changing it in one place could silently drift the producer and consumer onto different
 * topics. Constants also let @KafkaListener(topics = ...) reference them at compile time.
 */
public final class KafkaTopics {

    public static final String JUDGE_REQUESTS = "judge.requests";
    public static final int JUDGE_REQUESTS_PARTITIONS = 3;

    private KafkaTopics() {
    }
}
