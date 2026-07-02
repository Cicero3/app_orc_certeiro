package com.example.api.orcamentos.application


import com.example.api.orcamentos.api.dto.OrcamentoCreateDto
import com.example.api.orcamentos.api.dto.OrcamentoSummaryDto
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
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
