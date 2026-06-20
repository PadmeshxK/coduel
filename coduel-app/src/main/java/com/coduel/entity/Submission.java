package com.coduel.entity;

import com.coduel.execution.model.constant.Language;
import com.coduel.model.constant.Verdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(indexes = {
        @Index(name = "idx_submission_problem", columnList = "problem_id"),
        @Index(name = "idx_submission_match", columnList = "match_id"),
        @Index(name = "idx_submission_user", columnList = "user_id")
})
public class Submission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who submitted — set from the authenticated principal, never from the request body.
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long problemId;

    // nullable: null = solo submission (no duel); set when the submission belongs to a match.
    private Long matchId;

    // VARCHAR, not a native MySQL/TiDB ENUM: adding a language must never require a schema change
    // (a native enum column truncates unknown values — see the CPP "Data truncated" failure).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 32)
    private Verdict verdict = Verdict.PENDING;

    // nullable until judged: the judge worker fills this in (step 4).
    private Long runtimeMs;

    // judging detail: how many test cases passed out of the total (set by the judge; null until judged).
    private Integer passedTests;
    private Integer totalTests;

    // outbox flag: false until the judge request has been relayed to Kafka. The relay flips it true,
    // so persistence + "needs dispatch" are one atomic write (no dual-write race, no orphan PENDING).
    @Column(nullable = false)
    private boolean dispatched = false;
}
