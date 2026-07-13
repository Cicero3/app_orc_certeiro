package com.example.api.orcamentos.application


import com.example.api.orcamentos.api.dto.OrcamentoCreateDto
import com.example.api.orcamentos.api.dto.OrcamentoDetailDto
import com.example.api.orcamentos.api.dto.OrcamentoSummaryDto
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class GerenciarOrcamentoUseCase(
    private val orcamentoRepository: OrcamentoRepository
) {

    @Transactional
    fun criarOrcamento(userId: UUID, dto: OrcamentoCreateDto): OrcamentoSummaryDto {
        val novoOrcamento = Orcamento(
            tenantId = userId, // O Tenant inicialmente é o próprio usuário (Single-user por enquanto)
            ownerId = userId,
            titulo = dto.titulo,
            bdi = dto.bdi ?: BigDecimal.ZERO
        )

        val salvo = orcamentoRepository.saveAndFlush(novoOrcamento)
        return mapToSummaryDto(salvo)
    }

    @Transactional(readOnly = true)
    fun listarMeusOrcamentos(userId: UUID): List<OrcamentoSummaryDto> {
        val orcamentos = orcamentoRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId)
        return orcamentos.map { mapToSummaryDto(it) }
    }

    @Transactional(readOnly = true)
    fun detalharOrcamento(userId: UUID, orcamentoId: UUID): OrcamentoDetailDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        return PlanilhaMapper.toDetailDto(orcamento)
    }

    @Transactional
    fun atualizarBdi(userId: UUID, orcamentoId: UUID, novoBdi: BigDecimal): OrcamentoDetailDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        orcamento.atualizarBdi(novoBdi)
        orcamentoRepository.save(orcamento)
        return PlanilhaMapper.toDetailDto(orcamento)
    }

    @Transactional
    fun excluirOrcamento(userId: UUID, orcamentoId: UUID) {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        orcamentoRepository.delete(orcamento)
    }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado.") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }

    private fun mapToSummaryDto(orcamento: Orcamento): OrcamentoSummaryDto {
        return OrcamentoSummaryDto(
            id = orcamento.id,
            titulo = orcamento.titulo,
            status = orcamento.status.name,
            valorTotal = orcamento.valorTotal,
            createdAt = orcamento.createdAt,
            updatedAt = orcamento.updatedAt
        )
    }
}
