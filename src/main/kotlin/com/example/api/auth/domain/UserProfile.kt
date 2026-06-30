package com.example.api.auth.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    val id: UUID? = null,  // Será preenchido automaticamente pelo @MapsId

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "display_name")
    var displayName: String? = null,

    @Column(name = "current_level", nullable = false)
    var currentLevel: String = "A1",

    @Column(name = "total_xp", nullable = false)
    var totalXp: Int = 0,

    @Column(name = "streak_days", nullable = false)
    var streakDays: Int = 0,

    @Column(name = "last_active_at")
    var lastActiveAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)