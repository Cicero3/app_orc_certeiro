package com.example.api.orcamentos.application

import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.AmbienteProjeto
import com.example.api.orcamentos.domain.OrcamentoStatus
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CriarRevisaoOrcamentoUseCase(
    private val orcamentoRepository: OrcamentoRepository
) {
    @Transactional
    fun execute(orcamentoOriginalId: UUID, usuarioId: UUID): Orcamento {
        val orcamentoOriginal = orcamentoRepository.findById(orcamentoOriginalId)
            .orElseThrow { IllegalArgumentException("Orçamento original não encontrado") }

        if (orcamentoOriginal.status != OrcamentoStatus.REJEITADO && orcamentoOriginal.status != OrcamentoStatus.CANCELADO) {
            throw DomainSecurityException("Só é possível criar revisão de orçamentos rejeitados ou cancelados.")
        }
        
        if (orcamentoOriginal.ownerId != usuarioId) {
             throw DomainSecurityException("Apenas o dono do orçamento pode criar uma nova revisão.")
        }

        // Cria a nova revisão (v2) apontando para a v1 como parentId
        val novaRevisao = Orcamento(
            tenantId = orcamentoOriginal.tenantId,
            ownerId = orcamentoOriginal.ownerId,
            parentId = orcamentoOriginal.id,
            titulo = orcamentoOriginal.titulo + " (Revisão)",
            bdi = orcamentoOriginal.bdi
        )

        // Copia os módulos e a EAP
        orcamentoOriginal.modulos.forEach { moduloAntigo ->
            val novoModulo = OrcamentoModulo(
                tipoModulo = moduloAntigo.tipoModulo,
                nome = moduloAntigo.nome
            )
            novaRevisao.adicionarModulo(novoModulo)

            moduloAntigo.eapItens.forEach { eapAntigo ->
                val novoEap = EapItem(
                    codigoItem = eapAntigo.codigoItem,
                    descricao = eapAntigo.descricao,
                    marca = eapAntigo.marca,
                    unidade = eapAntigo.unidade,
                    quantidade = eapAntigo.quantidade,
                    valorMo = eapAntigo.valorMo,
                    valorMat = eapAntigo.valorMat,
                    valorSrv = eapAntigo.valorSrv,
                    observacoes = eapAntigo.observacoes
                )
                novoModulo.adicionarEapItem(novoEap)

                // Aqui precisaríamos de uma lógica recursiva para copiar sub-itens e a CPU.
                // Por simplicidade no exemplo, assumimos 1 nível.
            }
        }

        // Copia os ambientes
        orcamentoOriginal.ambientes.forEach { amb ->
            novaRevisao.adicionarAmbiente(AmbienteProjeto(
                nomeAmbiente = amb.nomeAmbiente,
                largura = amb.largura,
                comprimento = amb.comprimento,
                peDireito = amb.peDireito
            ))
        }

        return orcamentoRepository.save(novaRevisao)
    }
}
