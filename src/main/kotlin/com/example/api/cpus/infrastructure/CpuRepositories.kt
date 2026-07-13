package com.example.api.cpus.infrastructure

import com.example.api.cpus.domain.CpuPropria
import com.example.api.cpus.domain.FuncaoSalarial
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FuncaoSalarialRepository : JpaRepository<FuncaoSalarial, UUID> {
    fun findAllByOwnerIdOrderByNomeAsc(ownerId: UUID): List<FuncaoSalarial>
    fun existsByOwnerIdAndNomeIgnoreCase(ownerId: UUID, nome: String): Boolean
}

@Repository
interface CpuPropriaRepository : JpaRepository<CpuPropria, UUID> {
    fun findAllByOwnerIdOrderByCodigoAsc(ownerId: UUID, pageable: Pageable): Page<CpuPropria>

    @Query(
        """
        SELECT c FROM CpuPropria c
        WHERE c.ownerId = :ownerId
          AND (UPPER(c.descricao) LIKE UPPER(CONCAT('%', :termo, '%'))
               OR UPPER(c.codigo) LIKE UPPER(CONCAT('%', :termo, '%')))
        ORDER BY c.codigo ASC
        """
    )
    fun search(@Param("ownerId") ownerId: UUID, @Param("termo") termo: String, pageable: Pageable): Page<CpuPropria>

    fun existsByOwnerIdAndCodigoIgnoreCase(ownerId: UUID, codigo: String): Boolean
}
