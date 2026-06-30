package com.example.api.auth.application

import com.example.api.auth.infrastructure.UserProfileRepository
import com.example.api.common.learner.LearnerProfilePort
import com.example.api.common.learner.LearnerProfileView
import jakarta.persistence.EntityNotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service

/**
 * Adapter de auth para a porta LearnerProfilePort. Sem @Transactional próprio:
 * roda na transação do chamador (progress) para honrar CLAUDE.md §3.4.
 */
@Service
class LearnerProfileAdapter(
    private val userProfileRepository: UserProfileRepository
) : LearnerProfilePort {

    override fun getProfile(userId: UUID): LearnerProfileView? =
        userProfileRepository.findById(userId).map {
            LearnerProfileView(
                userId = userId,
                totalXp = it.totalXp,
                streakDays = it.streakDays,
                currentLevel = it.currentLevel
            )
        }.orElse(null)

    override fun addXp(userId: UUID, amount: Int) {
        val profile = userProfileRepository.findById(userId).orElseThrow {
            EntityNotFoundException("UserProfile $userId not found")
        }
        profile.totalXp += amount
        profile.lastActiveAt = Instant.now()
        userProfileRepository.save(profile)
    }
}
