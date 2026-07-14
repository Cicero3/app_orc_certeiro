package com.example.api.orcamentos.infrastructure

import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.AmbienteProjeto
import com.example.api.orcamentos.domain.ComposicaoPreco
import com.example.api.orcamentos.domain.CustoIndireto
import com.example.api.orcamentos.domain.FormacaoPreco
import com.example.api.orcamentos.domain.CronogramaAlocacao
import com.example.api.orcamentos.domain.DiarioObra
import com.example.api.orcamentos.domain.Levantamento
import com.example.api.orcamentos.domain.Risco
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

@Repository
interface CustoIndiretoRepository : JpaRepository<CustoIndireto, UUID> {
    fun findAllByOrcamentoIdOrderByCategoriaAscDescricaoAsc(orcamentoId: UUID): List<CustoIndireto>
}

@Repository
interface FormacaoPrecoRepository : JpaRepository<FormacaoPreco, UUID>

@Repository
interface LevantamentoRepository : JpaRepository<Levantamento, UUID> {
    fun findAllByOrcamentoIdOrderByCreatedAtDesc(orcamentoId: UUID): List<Levantamento>
}

@Repository
interface RiscoRepository : JpaRepository<Risco, UUID> {
    fun findAllByOrcamentoIdOrderByCreatedAtAsc(orcamentoId: UUID): List<Risco>
}

@Repository
interface DiarioObraRepository : JpaRepository<DiarioObra, UUID> {
    fun findAllByOrcamentoIdOrderByDataDesc(orcamentoId: UUID): List<DiarioObra>
    fun findByOrcamentoIdAndData(orcamentoId: UUID, data: java.time.LocalDate): DiarioObra?
}

@Repository
interface CronogramaAlocacaoRepository : JpaRepository<CronogramaAlocacao, UUID> {
    fun findAllByOrcamentoId(orcamentoId: UUID): List<CronogramaAlocacao>
    fun findByOrcamentoIdAndModuloIdAndPeriodo(orcamentoId: UUID, moduloId: UUID, periodo: Int): CronogramaAlocacao?
}
