package com.coduel.entity;

import com.coduel.model.constant.FriendshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_friendship_pair",
                columnNames = {"requester_id", "addressee_id"}),
        indexes = {
                @Index(name = "idx_friendship_requester", columnList = "requester_id"),
                @Index(name = "idx_friendship_addressee", columnList = "addressee_id")
        })
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who sent the request; the pair (requester, addressee) is unique, and the flow blocks the
    // reverse direction too, so there's at most one row per pair of users.
    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false)
    private Long addresseeId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private FriendshipStatus status;
}
