package com.example.api.cpus.application

import com.example.api.cpus.api.dto.CpuDetailDto
import com.example.api.cpus.api.dto.CpuInsumoDetailDto
import com.example.api.cpus.api.dto.CpuInsumoUpsertDto
import com.example.api.cpus.api.dto.CpuSummaryDto
import com.example.api.cpus.api.dto.CpuUpsertDto
import com.example.api.cpus.api.dto.FuncaoSalarialDto
import com.example.api.cpus.api.dto.FuncaoSalarialUpsertDto
import com.example.api.cpus.domain.CpuInsumo
import com.example.api.cpus.domain.CpuPropria
import com.example.api.cpus.domain.FuncaoSalarial
import com.example.api.cpus.domain.TipoContratacao
import com.example.api.cpus.domain.TipoInsumoCpu
import com.example.api.cpus.infrastructure.CpuPropriaRepository
import com.example.api.cpus.infrastructure.FuncaoSalarialRepository
import com.example.api.orcamentos.domain.DomainSecurityException
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GerenciarCpuUseCase(
    private val cpuRepository: CpuPropriaRepository,
    private val funcaoRepository: FuncaoSalarialRepository
) {

    // ---------- Funções salariais ----------

    @Transactional(readOnly = true)
    fun listarFuncoes(userId: UUID): List<FuncaoSalarialDto> =
        funcaoRepository.findAllByOwnerIdOrderByNomeAsc(userId).map { it.toDto() }

    @Transactional
    fun criarFuncao(userId: UUID, dto: FuncaoSalarialUpsertDto): FuncaoSalarialDto {
        if (funcaoRepository.existsByOwnerIdAndNomeIgnoreCase(userId, dto.nome)) {
            throw IllegalArgumentException("Já existe uma função com o nome '${dto.nome}'.")
        }
        val funcao = FuncaoSalarial(
            ownerId = userId,
            nome = dto.nome,
            valorHora = dto.valorHora,
            tipoContratacao = parseTipoContratacao(dto.tipoContratacao),
            encargosPct = dto.encargosPct
        )
        return funcaoRepository.save(funcao).toDto()
    }

    @Transactional
    fun atualizarFuncao(userId: UUID, funcaoId: UUID, dto: FuncaoSalarialUpsertDto): FuncaoSalarialDto {
        val funcao = carregarFuncaoDoDono(userId, funcaoId)
        funcao.atualizar(dto.nome, dto.valorHora, parseTipoContratacao(dto.tipoContratacao), dto.encargosPct)
        return funcaoRepository.save(funcao).toDto()
    }

    /** Aplica os encargos em lote por tipo (ex.: horista 88,28% / mensalista 49,82% — planilha 004). */
    @Transactional
    fun aplicarEncargos(userId: UUID, horistaPct: java.math.BigDecimal, mensalistaPct: java.math.BigDecimal): List<FuncaoSalarialDto> {
        val funcoes = funcaoRepository.findAllByOwnerIdOrderByNomeAsc(userId)
        funcoes.forEach { funcao ->
            funcao.aplicarEncargos(
                if (funcao.tipoContratacao == TipoContratacao.HORISTA) horistaPct else mensalistaPct
            )
        }
        return funcaoRepository.saveAll(funcoes).map { it.toDto() }
    }

    @Transactional
    fun excluirFuncao(userId: UUID, funcaoId: UUID) {
        val funcao = carregarFuncaoDoDono(userId, funcaoId)
        funcaoRepository.delete(funcao)
    }

    // ---------- CPUs próprias ----------

    @Transactional(readOnly = true)
    fun listarCpus(userId: UUID, search: String?, pageable: Pageable): Page<CpuSummaryDto> {
        val page = if (search.isNullOrBlank()) cpuRepository.findAllByOwnerIdOrderByCodigoAsc(userId, pageable)
        else cpuRepository.search(userId, search, pageable)
        return page.map { it.toSummaryDto() }
    }

    @Transactional(readOnly = true)
    fun detalharCpu(userId: UUID, cpuId: UUID): CpuDetailDto =
        carregarCpuDoDono(userId, cpuId).toDetailDto()

    @Transactional
    fun criarCpu(userId: UUID, dto: CpuUpsertDto): CpuDetailDto {
        if (cpuRepository.existsByOwnerIdAndCodigoIgnoreCase(userId, dto.codigo)) {
            throw IllegalArgumentException("Já existe uma CPU com o código '${dto.codigo}'.")
        }
        val cpu = CpuPropria(
            ownerId = userId,
            codigo = dto.codigo,
            descricao = dto.descricao,
            unidade = dto.unidade,
            isAuxiliar = dto.isAuxiliar
        )
        aplicarInsumos(userId, cpu, dto.insumos)
        return cpuRepository.save(cpu).toDetailDto()
    }

    @Transactional
    fun atualizarCpu(userId: UUID, cpuId: UUID, dto: CpuUpsertDto): CpuDetailDto {
        val cpu = carregarCpuDoDono(userId, cpuId)
        if (!cpu.codigo.equals(dto.codigo, ignoreCase = true) &&
            cpuRepository.existsByOwnerIdAndCodigoIgnoreCase(userId, dto.codigo)
        ) {
            throw IllegalArgumentException("Já existe uma CPU com o código '${dto.codigo}'.")
        }
        cpu.codigo = dto.codigo
        cpu.descricao = dto.descricao
        cpu.unidade = dto.unidade
        cpu.limparInsumos()
        aplicarInsumos(userId, cpu, dto.insumos)
        return cpuRepository.save(cpu).toDetailDto()
    }

    @Transactional
    fun excluirCpu(userId: UUID, cpuId: UUID) {
        val cpu = carregarCpuDoDono(userId, cpuId)
        try {
            cpuRepository.delete(cpu)
            cpuRepository.flush()
        } catch (ex: org.springframework.dao.DataIntegrityViolationException) {
            throw IllegalArgumentException("Esta composição auxiliar é usada por outras CPUs e não pode ser excluída.")
        }
    }

    // ---------- Internos ----------

    private fun aplicarInsumos(userId: UUID, cpu: CpuPropria, insumos: List<CpuInsumoUpsertDto>) {
        insumos.forEach { dto ->
            val funcao = dto.funcaoSalarialId?.let { carregarFuncaoDoDono(userId, it) }
            val referencia = dto.cpuReferenciaId?.let { carregarCpuDoDono(userId, it) }
            val insumo = CpuInsumo(
                tipoInsumo = parseTipoInsumo(dto.tipoInsumo),
                descricao = dto.descricao,
                unidade = dto.unidade,
                coeficiente = dto.coeficiente,
                custoUnitario = dto.custoUnitario,
                funcaoSalarial = funcao,
                cpuReferencia = referencia
            )
            runCatching { cpu.adicionarInsumo(insumo) }
                .getOrElse { throw IllegalArgumentException(it.message) }
        }
    }

    private fun carregarCpuDoDono(userId: UUID, cpuId: UUID): CpuPropria {
        val cpu = cpuRepository.findById(cpuId)
            .orElseThrow { EntityNotFoundException("CPU não encontrada") }
        if (cpu.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar esta CPU.")
        }
        return cpu
    }

    private fun carregarFuncaoDoDono(userId: UUID, funcaoId: UUID): FuncaoSalarial {
        val funcao = funcaoRepository.findById(funcaoId)
            .orElseThrow { EntityNotFoundException("Função salarial não encontrada") }
        if (funcao.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar esta função salarial.")
        }
        return funcao
    }

    private fun parseTipoInsumo(valor: String): TipoInsumoCpu =
        runCatching { TipoInsumoCpu.valueOf(valor.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Tipo de insumo desconhecido: $valor") }

    private fun parseTipoContratacao(valor: String): TipoContratacao =
        runCatching { TipoContratacao.valueOf(valor.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Tipo de contratação desconhecido: $valor (use HORISTA ou MENSALISTA)") }

    private fun FuncaoSalarial.toDto() = FuncaoSalarialDto(
        id = id, nome = nome, valorHora = valorHora,
        tipoContratacao = tipoContratacao.name, encargosPct = encargosPct,
        valorHoraComEncargos = valorHoraComEncargos
    )

    private fun CpuPropria.toSummaryDto() = CpuSummaryDto(
        id = id, codigo = codigo, descricao = descricao, unidade = unidade,
        isAuxiliar = isAuxiliar, valorUnitario = valorUnitario
    )

    private fun CpuPropria.toDetailDto() = CpuDetailDto(
        id = id, codigo = codigo, descricao = descricao, unidade = unidade, isAuxiliar = isAuxiliar,
        valorMo = valorMo, valorMat = valorMat, valorSrv = valorSrv, valorUnitario = valorUnitario,
        insumos = insumos.map { insumo ->
            val efetivo = custoUnitarioEfetivo(insumo)
            CpuInsumoDetailDto(
                id = insumo.id,
                tipoInsumo = insumo.tipoInsumo.name,
                descricao = insumo.descricao,
                unidade = insumo.unidade,
                coeficiente = insumo.coeficiente,
                custoUnitarioEfetivo = efetivo,
                custoTotal = insumo.coeficiente.multiply(efetivo),
                funcaoSalarialId = insumo.funcaoSalarial?.id,
                funcaoSalarialNome = insumo.funcaoSalarial?.nome,
                cpuReferenciaId = insumo.cpuReferencia?.id,
                cpuReferenciaCodigo = insumo.cpuReferencia?.codigo
            )
        }
    )
}
