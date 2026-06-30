package com.example.api.auth.infrastructure

import com.example.api.auth.domain.UserConsent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserConsentRepository : JpaRepository<UserConsent, UUID> {
    fun findByUserIdOrderByConsentedAtDesc(userId: UUID): List<UserConsent>
}
