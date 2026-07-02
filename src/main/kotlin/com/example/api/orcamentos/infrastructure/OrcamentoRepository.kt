package com.example.api.orcamentos.infrastructure

import com.example.api.orcamentos.domain.Orcamento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrcamentoRepository : JpaRepository<Orcamento, UUID>
