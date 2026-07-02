package com.example.api.orcamentos.infrastructure

import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.AmbienteProjeto
import com.example.api.orcamentos.domain.ComposicaoPreco
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OrcamentoModuloRepository : JpaRepository<OrcamentoModulo, UUID>

@Repository
interface EapItemRepository : JpaRepository<EapItem, UUID>

@Repository
interface AmbienteProjetoRepository : JpaRepository<AmbienteProjeto, UUID>

@Repository
interface ComposicaoPrecoRepository : JpaRepository<ComposicaoPreco, UUID>
