package com.example.api.catalogos.infrastructure

import com.example.api.catalogos.domain.CatalogoItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CatalogoItemRepository : JpaRepository<CatalogoItem, UUID> {

    @Query("SELECT c FROM CatalogoItem c LEFT JOIN FETCH c.insumos WHERE c.id = :id")
    fun findByIdWithInsumos(id: UUID): CatalogoItem?

    fun findByDescricaoContainingIgnoreCaseOrCodigoContainingIgnoreCase(
        descricao: String,
        codigo: String,
        pageable: org.springframework.data.domain.Pageable
    ): org.springframework.data.domain.Page<CatalogoItem>
}
