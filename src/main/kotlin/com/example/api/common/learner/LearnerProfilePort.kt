package com.example.api.common.learner

import java.util.UUID

/**
 * Porta para o perfil do aluno (XP, streak, nível), cujos dados pertencem
 * à feature auth/users. Permite que progress credite XP e leia o perfil
 * dentro da MESMA transação (CLAUDE.md §3.4) sem importar auth diretamente.
 */
interface LearnerProfilePort {
    fun getProfile(userId: UUID): LearnerProfileView?

    /** Credita XP e atualiza last_active_at. Deve rodar na transação do chamador. */
    fun addXp(userId: UUID, amount: Int)
}

data class LearnerProfileView(
    val userId: UUID,
    val totalXp: Int,
    val streakDays: Int,
    val currentLevel: String
)
