package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.LevantamentoCreateDto
import com.example.api.orcamentos.api.dto.LevantamentoDto
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.Levantamento
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.LevantamentoRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Levantamentos de quantitativo das calculadoras (aba "2. LEV QNT").
 * Ao vincular a um item da EAP, a quantidade do item passa a ser o resultado
 * do levantamento — com a memória de cálculo (payload) auditável.
 */
@Service
class LevantamentoUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val eapItemRepository: EapItemRepository,
    private val levantamentoRepository: LevantamentoRepository
) {

    @Transactional(readOnly = true)
    fun listar(userId: UUID, orcamentoId: UUID): List<LevantamentoDto> {
        carregarOrcamentoDoDono(userId, orcamentoId)
        return levantamentoRepository.findAllByOrcamentoIdOrderByCreatedAtDesc(orcamentoId).map { it.toDto() }
    }

    @Transactional
    fun salvar(userId: UUID, orcamentoId: UUID, dto: LevantamentoCreateDto): LevantamentoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)

        val levantamento = Levantamento(
            orcamentoId = orcamentoId,
            tipo = dto.tipo,
            descricao = dto.descricao,
            unidade = dto.unidade,
            resultado = dto.resultado,
            payload = dto.payload
        )

        // Vincula ao item da EAP: a quantidade do item passa a ser o resultado do levantamento
        if (dto.eapItemId != null) {
            checkMutavel(orcamento)
            val item = eapItemRepository.findById(dto.eapItemId)
                .orElseThrow { EntityNotFoundException("Item EAP não encontrado") }
            if (item.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Item não pertence a este orçamento.")
            }
            item.atualizarValores(dto.resultado, item.valorMo, item.valorMat, item.valorSrv)
            eapItemRepository.save(item)
            levantamento.eapItemId = item.id
        }

        return levantamentoRepository.save(levantamento).toDto()
    }

    @Transactional
    fun excluir(userId: UUID, orcamentoId: UUID, levantamentoId: UUID) {
        carregarOrcamentoDoDono(userId, orcamentoId)
        val levantamento = levantamentoRepository.findById(levantamentoId)
            .orElseThrow { EntityNotFoundException("Levantamento não encontrado") }
        if (levantamento.orcamentoId != orcamentoId) {
            throw DomainSecurityException("Levantamento não pertence a este orçamento.")
        }
        levantamentoRepository.delete(levantamento)
    }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }

    private fun checkMutavel(orcamento: Orcamento) {
        if (orcamento.status.isImutavel) {
            throw DomainSecurityException("Orçamento em status ${orcamento.status} é imutável. Edição bloqueada.")
        }
    }

    private fun Levantamento.toDto() = LevantamentoDto(
        id = id, orcamentoId = orcamentoId, eapItemId = eapItemId, tipo = tipo,
        descricao = descricao, unidade = unidade, resultado = resultado,
        payload = payload, createdAt = createdAt
    )
}
