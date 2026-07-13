package com.example.api.orcamentos.application

import com.example.api.cpus.infrastructure.CpuPropriaRepository
import com.example.api.orcamentos.api.dto.EapItemCreateDto
import com.example.api.orcamentos.api.dto.EapItemFromCatalogoDto
import com.example.api.orcamentos.api.dto.EapItemFromCpuDto
import com.example.api.orcamentos.api.dto.EapItemUpdateDto
import com.example.api.orcamentos.api.dto.ModuloCreateDto
import com.example.api.orcamentos.domain.ComposicaoPreco
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoInsumo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.OrcamentoModuloRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Monta e edita a EAP (módulos e itens) de um orçamento.
 * Toda operação valida dono e imutabilidade por status antes de alterar.
 */
@Service
class GerenciarEapUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val moduloRepository: OrcamentoModuloRepository,
    private val eapItemRepository: EapItemRepository,
    private val copiarItemCatalogoUseCase: CopiarItemCatalogoUseCase,
    private val cpuPropriaRepository: CpuPropriaRepository
) {

    @Transactional
    fun adicionarModulo(userId: UUID, orcamentoId: UUID, dto: ModuloCreateDto): OrcamentoModulo {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        val tipo = dto.tipoModulo?.let { parseTipoModulo(it) } ?: TipoModulo.OUTROS
        val modulo = OrcamentoModulo(tipoModulo = tipo, nome = dto.nome)
        orcamento.adicionarModulo(modulo)
        orcamentoRepository.save(orcamento)
        return modulo
    }

    @Transactional
    fun removerModulo(userId: UUID, orcamentoId: UUID, moduloId: UUID) {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val modulo = moduloRepository.findById(moduloId)
            .orElseThrow { EntityNotFoundException("Módulo não encontrado") }
        if (modulo.orcamento?.id != orcamentoId) {
            throw DomainSecurityException("Módulo não pertence a este orçamento.")
        }
        moduloRepository.delete(modulo)
    }

    @Transactional
    fun adicionarItem(userId: UUID, orcamentoId: UUID, dto: EapItemCreateDto): EapItem {
        require((dto.moduloId != null) xor (dto.parentId != null)) {
            "Informe o moduloId (item de primeiro nível) ou o parentId (subitem), nunca ambos."
        }
        carregarOrcamentoDoDono(userId, orcamentoId)

        val novoItem = EapItem(
            codigoItem = dto.codigoItem,
            descricao = dto.descricao,
            marca = dto.marca,
            unidade = dto.unidade,
            quantidade = dto.quantidade,
            valorMo = dto.valorMo,
            valorMat = dto.valorMat,
            valorSrv = dto.valorSrv,
            observacoes = dto.observacoes
        )

        if (dto.moduloId != null) {
            val modulo = moduloRepository.findById(dto.moduloId)
                .orElseThrow { EntityNotFoundException("Módulo não encontrado") }
            if (modulo.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Módulo não pertence a este orçamento.")
            }
            modulo.adicionarEapItem(novoItem)
        } else {
            val parent = eapItemRepository.findById(dto.parentId!!)
                .orElseThrow { EntityNotFoundException("Item EAP pai não encontrado") }
            if (parent.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Item pai não pertence a este orçamento.")
            }
            parent.adicionarSubItem(novoItem)
        }

        return eapItemRepository.save(novoItem)
    }

    @Transactional
    fun copiarDoCatalogo(userId: UUID, orcamentoId: UUID, dto: EapItemFromCatalogoDto): EapItem {
        require((dto.moduloId != null) xor (dto.parentId != null)) {
            "Informe o moduloId (item de primeiro nível) ou o parentId (subitem), nunca ambos."
        }
        carregarOrcamentoDoDono(userId, orcamentoId)

        // Valida que o destino pertence ao orçamento antes de delegar a cópia
        if (dto.moduloId != null) {
            val modulo = moduloRepository.findById(dto.moduloId)
                .orElseThrow { EntityNotFoundException("Módulo não encontrado") }
            if (modulo.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Módulo não pertence a este orçamento.")
            }
        } else {
            val parent = eapItemRepository.findById(dto.parentId!!)
                .orElseThrow { EntityNotFoundException("Item EAP pai não encontrado") }
            if (parent.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Item pai não pertence a este orçamento.")
            }
        }

        return copiarItemCatalogoUseCase.executar(
            catalogoItemId = dto.catalogoItemId,
            quantidade = dto.quantidade,
            moduloId = dto.moduloId,
            parentId = dto.parentId
        )
    }

    /**
     * Copia uma CPU própria do usuário para a EAP como novo item, com snapshot dos insumos
     * (o custo efetivo é congelado no momento da cópia — o orçamento não muda se a tabela
     * salarial ou a composição auxiliar mudarem depois).
     */
    @Transactional
    fun copiarDaCpuPropria(userId: UUID, orcamentoId: UUID, dto: EapItemFromCpuDto): EapItem {
        require((dto.moduloId != null) xor (dto.parentId != null)) {
            "Informe o moduloId (item de primeiro nível) ou o parentId (subitem), nunca ambos."
        }
        carregarOrcamentoDoDono(userId, orcamentoId)

        val cpu = cpuPropriaRepository.findById(dto.cpuId)
            .orElseThrow { EntityNotFoundException("CPU não encontrada") }
        if (cpu.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para usar esta CPU.")
        }

        val novoItem = EapItem(
            codigoItem = cpu.codigo,
            descricao = cpu.descricao,
            unidade = cpu.unidade,
            quantidade = dto.quantidade,
            valorMo = cpu.valorMo,
            valorMat = cpu.valorMat,
            valorSrv = cpu.valorSrv
        )

        if (dto.moduloId != null) {
            val modulo = moduloRepository.findById(dto.moduloId)
                .orElseThrow { EntityNotFoundException("Módulo não encontrado") }
            if (modulo.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Módulo não pertence a este orçamento.")
            }
            modulo.adicionarEapItem(novoItem)
        } else {
            val parent = eapItemRepository.findById(dto.parentId!!)
                .orElseThrow { EntityNotFoundException("Item EAP pai não encontrado") }
            if (parent.orcamento?.id != orcamentoId) {
                throw DomainSecurityException("Item pai não pertence a este orçamento.")
            }
            parent.adicionarSubItem(novoItem)
        }

        cpu.insumos.forEach { insumo ->
            novoItem.adicionarComposicao(
                ComposicaoPreco(
                    tipoInsumo = TipoInsumo.valueOf(insumo.tipoInsumo.name),
                    descricao = insumo.descricao,
                    unidade = insumo.unidade,
                    coeficiente = insumo.coeficiente,
                    custoUnitarioInsumo = cpu.custoUnitarioEfetivo(insumo)
                )
            )
        }

        return eapItemRepository.save(novoItem)
    }

    @Transactional
    fun atualizarItem(userId: UUID, orcamentoId: UUID, itemId: UUID, dto: EapItemUpdateDto): EapItem {
        carregarOrcamentoDoDono(userId, orcamentoId)
        val item = carregarItemDoOrcamento(itemId, orcamentoId)
        item.atualizarValores(dto.quantidade, dto.valorMo, dto.valorMat, dto.valorSrv)
        return eapItemRepository.save(item)
    }

    @Transactional
    fun removerItem(userId: UUID, orcamentoId: UUID, itemId: UUID) {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val item = carregarItemDoOrcamento(itemId, orcamentoId)
        eapItemRepository.delete(item)
    }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para alterar este orçamento.")
        }
        return orcamento
    }

    private fun carregarItemDoOrcamento(itemId: UUID, orcamentoId: UUID): EapItem {
        val item = eapItemRepository.findById(itemId)
            .orElseThrow { EntityNotFoundException("Item EAP não encontrado") }
        if (item.orcamento?.id != orcamentoId) {
            throw DomainSecurityException("Item não pertence a este orçamento.")
        }
        return item
    }

    private fun checkMutavel(orcamento: Orcamento) {
        if (orcamento.status.isImutavel) {
            throw DomainSecurityException("Orçamento em status ${orcamento.status} é imutável. Edição bloqueada.")
        }
    }

    private fun parseTipoModulo(valor: String): TipoModulo =
        runCatching { TipoModulo.valueOf(valor.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Tipo de módulo desconhecido: $valor") }
}
