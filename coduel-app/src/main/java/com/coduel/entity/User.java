package com.coduel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity

@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_user_google_id", columnNames = "google_id"))
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String googleId;

    @Column(nullable = false)
    private String email;

    private String displayName;

    private String avatarUrl;

    // Whether the user has explicitly chosen a (unique) display name via setup. New OAuth accounts
    // start false (name is just the provisional Google name) and are routed to setup on sign-in.
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean displayNameSet = false;
}
