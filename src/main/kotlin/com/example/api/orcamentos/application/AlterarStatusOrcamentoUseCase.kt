package com.example.api.orcamentos.application

import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoStatus
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AlterarStatusOrcamentoUseCase(
    private val orcamentoRepository: OrcamentoRepository
) {
    @Transactional
    fun execute(orcamentoId: UUID, usuarioAprovadorId: UUID, isAdmin: Boolean, acao: String) {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { IllegalArgumentException("Orçamento não encontrado") }

        // Dono opera o próprio orçamento; admin pode operar qualquer um (aprovação interna).
        if (orcamento.ownerId != usuarioAprovadorId && !isAdmin) {
            throw DomainSecurityException("Você não tem permissão para alterar o status deste orçamento.")
        }

        when (acao.uppercase()) {
            "SOLICITAR_APROVACAO" -> orcamento.solicitarAprovacao(usuarioAprovadorId)
            "APROVAR" -> orcamento.aprovar(usuarioAprovadorId, isAdmin)
            "REJEITAR" -> orcamento.rejeitarInternamente(usuarioAprovadorId, isAdmin)
            "ENVIAR_CLIENTE" -> orcamento.enviarAoCliente()
            "ACEITAR_CLIENTE" -> orcamento.registrarRespostaCliente(true)
            "RECUSAR_CLIENTE" -> orcamento.registrarRespostaCliente(false)
            "CANCELAR" -> orcamento.cancelar()
            else -> throw IllegalArgumentException("Ação de status desconhecida: $acao")
        }

        orcamentoRepository.save(orcamento)
    }
}
