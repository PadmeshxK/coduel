package com.coduel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// One person in a room's persistent roster. The oldest member (lowest id) is the host.
@Getter
@Setter
@Entity
public class RoomMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private Long userId;

    // Lobby readiness. The host is implicitly ready (starting is their signal), so this only
    // applies to non-host members; the host can't start until everyone else is ready.
    @Column(nullable = false)
    private boolean ready;
}
