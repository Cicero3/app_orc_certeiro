package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.CustoIndiretoDto
import com.example.api.orcamentos.api.dto.CustoIndiretoUpsertDto
import com.example.api.orcamentos.api.dto.CustosIndiretosResumoDto
import com.example.api.orcamentos.api.dto.FormacaoPrecoDto
import com.example.api.orcamentos.api.dto.FormacaoPrecoUpsertDto
import com.example.api.orcamentos.domain.CategoriaCustoIndireto
import com.example.api.orcamentos.domain.CustoIndireto
import com.example.api.orcamentos.domain.DomainSecurityException
import com.example.api.orcamentos.domain.FormacaoPreco
import com.example.api.orcamentos.domain.FormacaoPrecoCalculadora
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.CustoIndiretoRepository
import com.example.api.orcamentos.infrastructure.FormacaoPrecoRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * Custos indiretos + formação de preço de venda de um orçamento
 * (abas "4. CUSTO IND" e "0. P. VENDA" da planilha de referência).
 */
@Service
class PrecificacaoUseCase(
    private val orcamentoRepository: OrcamentoRepository,
    private val custoIndiretoRepository: CustoIndiretoRepository,
    private val formacaoPrecoRepository: FormacaoPrecoRepository
) {

    // ---------- Custos indiretos ----------

    @Transactional(readOnly = true)
    fun listarCustosIndiretos(userId: UUID, orcamentoId: UUID): CustosIndiretosResumoDto {
        carregarOrcamentoDoDono(userId, orcamentoId)
        return montarResumo(custoIndiretoRepository.findAllByOrcamentoIdOrderByCategoriaAscDescricaoAsc(orcamentoId))
    }

    @Transactional
    fun adicionarCustoIndireto(userId: UUID, orcamentoId: UUID, dto: CustoIndiretoUpsertDto): CustoIndiretoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val custo = CustoIndireto(
            orcamentoId = orcamentoId,
            categoria = parseCategoria(dto.categoria),
            descricao = dto.descricao,
            quantidade = dto.quantidade,
            valorUnitario = dto.valorUnitario
        )
        return custoIndiretoRepository.save(custo).toDto()
    }

    @Transactional
    fun atualizarCustoIndireto(userId: UUID, orcamentoId: UUID, custoId: UUID, dto: CustoIndiretoUpsertDto): CustoIndiretoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val custo = carregarCustoDoOrcamento(custoId, orcamentoId)
        custo.atualizar(parseCategoria(dto.categoria), dto.descricao, dto.quantidade, dto.valorUnitario)
        return custoIndiretoRepository.save(custo).toDto()
    }

    @Transactional
    fun removerCustoIndireto(userId: UUID, orcamentoId: UUID, custoId: UUID) {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        custoIndiretoRepository.delete(carregarCustoDoOrcamento(custoId, orcamentoId))
    }

    // ---------- Formação de preço ----------

    @Transactional(readOnly = true)
    fun obterFormacaoPreco(userId: UUID, orcamentoId: UUID): FormacaoPrecoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        val params = formacaoPrecoRepository.findById(orcamentoId).orElse(FormacaoPreco(orcamentoId = orcamentoId))
        return montarFormacaoDto(orcamento, params)
    }

    @Transactional
    fun atualizarFormacaoPreco(userId: UUID, orcamentoId: UUID, dto: FormacaoPrecoUpsertDto): FormacaoPrecoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        checkMutavel(orcamento)
        val params = formacaoPrecoRepository.findById(orcamentoId).orElse(FormacaoPreco(orcamentoId = orcamentoId))
        params.admCentral = dto.admCentral
        params.custoFinanceiro = dto.custoFinanceiro
        params.contingencia = dto.contingencia
        params.comissao = dto.comissao
        params.lucro = dto.lucro
        params.cofins = dto.cofins
        params.pis = dto.pis
        params.icms = dto.icms
        params.iss = dto.iss
        params.irpj = dto.irpj
        params.csll = dto.csll
        formacaoPrecoRepository.save(params)
        return montarFormacaoDto(orcamento, params)
    }

    /**
     * Aplica o BDI calculado (sobre o custo direto) no orçamento, de modo que a
     * planilha orçamentária feche exatamente no preço de venda formado.
     */
    @Transactional
    fun aplicarBdiCalculado(userId: UUID, orcamentoId: UUID): FormacaoPrecoDto {
        val orcamento = carregarOrcamentoDoDono(userId, orcamentoId)
        val params = formacaoPrecoRepository.findById(orcamentoId).orElse(FormacaoPreco(orcamentoId = orcamentoId))
        val resultado = FormacaoPrecoCalculadora.calcular(params, orcamento.custoDireto, totalCustosIndiretos(orcamentoId))
        orcamento.atualizarBdi(resultado.bdiSobreCustoDireto)
        orcamentoRepository.save(orcamento)
        return montarFormacaoDto(orcamento, params)
    }

    // ---------- Internos ----------

    private fun montarFormacaoDto(orcamento: Orcamento, params: FormacaoPreco): FormacaoPrecoDto {
        val resultado = FormacaoPrecoCalculadora.calcular(params, orcamento.custoDireto, totalCustosIndiretos(orcamento.id))
        return FormacaoPrecoDto(
            admCentral = params.admCentral,
            custoFinanceiro = params.custoFinanceiro,
            contingencia = params.contingencia,
            comissao = params.comissao,
            lucro = params.lucro,
            cofins = params.cofins,
            pis = params.pis,
            icms = params.icms,
            iss = params.iss,
            irpj = params.irpj,
            csll = params.csll,
            custoDireto = resultado.custoDireto,
            custoIndireto = resultado.custoIndireto,
            baseCdCi = resultado.baseCdCi,
            admCentralValor = resultado.admCentralValor,
            custoFinanceiroValor = resultado.custoFinanceiroValor,
            contingenciaValor = resultado.contingenciaValor,
            custoTotal = resultado.custoTotal,
            aliquotaTributos = resultado.aliquotaTributos,
            tributosValor = resultado.tributosValor,
            comissaoValor = resultado.comissaoValor,
            lucroValor = resultado.lucroValor,
            precoVenda = resultado.precoVenda,
            bdiSobreCustoTotal = resultado.bdiSobreCustoTotal,
            bdiSobreCustoDireto = resultado.bdiSobreCustoDireto,
            bdiAtualDoOrcamento = orcamento.bdi
        )
    }

    private fun totalCustosIndiretos(orcamentoId: UUID): BigDecimal =
        custoIndiretoRepository.findAllByOrcamentoIdOrderByCategoriaAscDescricaoAsc(orcamentoId)
            .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.total) }

    private fun montarResumo(itens: List<CustoIndireto>): CustosIndiretosResumoDto {
        val dtos = itens.map { it.toDto() }
        return CustosIndiretosResumoDto(
            itens = dtos,
            totalPorCategoria = dtos.groupBy { it.categoria }
                .mapValues { (_, grupo) -> grupo.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.total) } },
            total = dtos.fold(BigDecimal.ZERO) { acc, c -> acc.add(c.total) }
        )
    }

    private fun carregarOrcamentoDoDono(userId: UUID, orcamentoId: UUID): Orcamento {
        val orcamento = orcamentoRepository.findById(orcamentoId)
            .orElseThrow { EntityNotFoundException("Orçamento não encontrado") }
        if (orcamento.ownerId != userId) {
            throw DomainSecurityException("Você não tem permissão para acessar este orçamento.")
        }
        return orcamento
    }

    private fun carregarCustoDoOrcamento(custoId: UUID, orcamentoId: UUID): CustoIndireto {
        val custo = custoIndiretoRepository.findById(custoId)
            .orElseThrow { EntityNotFoundException("Custo indireto não encontrado") }
        if (custo.orcamentoId != orcamentoId) {
            throw DomainSecurityException("Custo indireto não pertence a este orçamento.")
        }
        return custo
    }

    private fun checkMutavel(orcamento: Orcamento) {
        if (orcamento.status.isImutavel) {
            throw DomainSecurityException("Orçamento em status ${orcamento.status} é imutável. Edição bloqueada.")
        }
    }

    private fun parseCategoria(valor: String): CategoriaCustoIndireto =
        runCatching { CategoriaCustoIndireto.valueOf(valor.uppercase()) }
            .getOrElse { throw IllegalArgumentException("Categoria de custo indireto desconhecida: $valor") }

    private fun CustoIndireto.toDto() = CustoIndiretoDto(
        id = id, categoria = categoria.name, descricao = descricao,
        quantidade = quantidade, valorUnitario = valorUnitario, total = total
    )
}
