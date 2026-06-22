package com.coduel.entity;

import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.MatchState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
// Singular entity name; table mapped to "matches" because "match" is reserved in MySQL.
@Table(name = "matches")
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode gameMode;

    // null for matchmaking duels; set for a match spawned from a persistent private room.
    private Long roomId;

    @Column(nullable = false)
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchState state;

    // null until the match is decided (state = FINISHED).
    private Long winnerUserId;

    // null while ACTIVE; set when the match ends, regardless of how (solve, forfeit, no-show, timeout).
    @Enumerated(EnumType.STRING)
    private MatchEndReason endReason;

    // when the match finished. createdAt (BaseEntity) serves as the start time.
    private Instant endedAt;
}
