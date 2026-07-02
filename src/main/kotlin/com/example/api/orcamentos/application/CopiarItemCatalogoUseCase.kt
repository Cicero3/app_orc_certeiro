package com.example.api.orcamentos.application

import com.example.api.catalogos.infrastructure.CatalogoItemRepository
import com.example.api.orcamentos.domain.ComposicaoPreco
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.TipoInsumo
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.OrcamentoModuloRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class CopiarItemCatalogoUseCase(
    private val catalogoItemRepository: CatalogoItemRepository,
    private val moduloRepository: OrcamentoModuloRepository,
    private val eapItemRepository: EapItemRepository
) {

    @Transactional
    fun executar(
        catalogoItemId: UUID,
        quantidade: BigDecimal,
        moduloId: UUID? = null,
        parentId: UUID? = null
    ): EapItem {
        require(moduloId != null || parentId != null) { "É necessário informar o moduloId ou parentId de destino." }

        // Busca o item no catálogo (com seus insumos)
        val catalogoItem = catalogoItemRepository.findByIdWithInsumos(catalogoItemId)
            ?: throw EntityNotFoundException("Item de catálogo não encontrado.")

        // Cria o EapItem base
        val novoEapItem = EapItem(
            codigoItem = catalogoItem.codigo,
            descricao = catalogoItem.descricao,
            unidade = catalogoItem.unidade,
            quantidade = quantidade,
            valorMo = catalogoItem.valorMo,
            valorMat = catalogoItem.valorMat,
            valorSrv = catalogoItem.valorSrv
        )

        // Associa ao orçamento (Modulo ou Parent EAP)
        if (moduloId != null) {
            val modulo = moduloRepository.findById(moduloId)
                .orElseThrow { EntityNotFoundException("Módulo não encontrado") }
            modulo.adicionarEapItem(novoEapItem)
        } else if (parentId != null) {
            val parent = eapItemRepository.findById(parentId)
                .orElseThrow { EntityNotFoundException("Item EAP pai não encontrado") }
            parent.adicionarSubItem(novoEapItem)
        }

        // Copia os insumos como ComposicaoPreco (CPU)
        catalogoItem.insumos.forEach { insumo ->
            val composicao = ComposicaoPreco(
                tipoInsumo = TipoInsumo.valueOf(insumo.tipoInsumo),
                descricao = insumo.descricao,
                unidade = insumo.unidade,
                coeficiente = insumo.coeficiente,
                custoUnitarioInsumo = insumo.custoUnitario
            )
            novoEapItem.adicionarComposicao(composicao)
        }

        return eapItemRepository.save(novoEapItem)
    }
}
