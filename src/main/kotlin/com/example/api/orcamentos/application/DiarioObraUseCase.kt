package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.DiarioObraDto
import com.example.api.orcamentos.api.dto.DiarioObraUpsertDto
import com.example.api.orcamentos.domain.CondicaoClima
import com.example.api.orcamentos.domain.DiarioObra
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.DiarioObraRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** Relatório Diário de Obra — RDO (planilha 006). Um registro por orçamento+data (upsert). */
@Service
class DiarioObraUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val diarioRepository: DiarioObraRepository
) {

    @Transactional(readOnly = true)
    fun listar(userId: UUID, orcamentoId: UUID): List<DiarioObraDto> {
        carregarOrcamentoDoDono(userId, orcamentoId)
        return diarioRepository.findAllByOrcamentoIdOrderByDataDesc(orcamentoId).map { it.toDto() }
    }

    /** Cria ou atualiza o RDO da data informada. */
    @Transactional
    fun salvar(userId: UUID, orcamentoId: UUID, dto: DiarioObraUpsertDto): DiarioObraDto {
        carregarOrcamentoDoDono(userId, orcamentoId)

        val existente = diarioRepository.findByOrcamentoIdAndData(orcamentoId, dto.data)
        val diario = if (existente != null) {
            existente.atualizar(
                parseClima(dto.climaManha), parseClima(dto.climaTarde), parseClima(dto.climaNoite),
                dto.maoDeObra, dto.equipamentos, dto.atividades, dto.ocorrencias, dto.observacoes
            )
            existente
        } else {
            DiarioObra(
                orcamentoId = orcamentoId,
                data = dto.data,
                climaManha = parseClima(dto.climaManha),
                climaTarde = parseClima(dto.climaTarde),
                climaNoite = parseClima(dto.climaNoite),
                maoDeObra = dto.maoDeObra,
                equipamentos = dto.equipamentos,
                atividades = dto.atividades,
                ocorrencias = dto.ocorrencias,
                observacoes = dto.observacoes
            )
        }
        return diarioRepository.save(diario).toDto()
    }

    @Transactional
    fun excluir(userId: UUID, orcamentoId: UUID, diarioId: UUID) {
        carregarOrcamentoDoDono(userId, orcamentoId)
        val diario = diarioRepository.findById(diarioId)
            .orElseThrow { EntityNotFoundException("Diário não encontrado") }
        if (diario.orcamentoId != orcamentoId) {
            throw DomainSecurityException("Diário não pertence a este orçamento.")
        }
        diarioRepository.delete(diario)
    }

    private fun parseClima(valor: String?): CondicaoClima? =
        valor?.takeIf { it.isNotBlank() }?.let {
            runCatching { CondicaoClima.valueOf(it.uppercase()) }
                .getOrElse { _ -> throw IllegalArgumentException("Condição de clima desconhecida: $valor") }
        }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }

    private fun DiarioObra.toDto() = DiarioObraDto(
        id = id, data = data,
        climaManha = climaManha?.name, climaTarde = climaTarde?.name, climaNoite = climaNoite?.name,
        maoDeObra = maoDeObra, equipamentos = equipamentos, atividades = atividades,
        ocorrencias = ocorrencias, observacoes = observacoes, createdAt = createdAt
    )
}
