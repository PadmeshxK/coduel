package com.coduel.entity;

import com.coduel.model.constant.RoomState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A persistent private lobby. It outlives any single match: the host can start a match, it finishes,
 * everyone returns here, and the host can start another (rematch). Members live in RoomMember; the
 * in-progress match (if any) is the Match row whose roomId points here and whose state is ACTIVE.
 */
@Getter
@Setter
@Entity
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomState state;
}
