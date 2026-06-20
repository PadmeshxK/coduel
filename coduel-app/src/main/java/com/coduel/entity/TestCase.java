package com.coduel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(indexes = @Index(name = "idx_testcase_problem", columnList = "problem_id"))
public class TestCase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long problemId;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(nullable = false)
    private boolean hidden;
}
