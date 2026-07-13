package com.example.api.orcamentos.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Projeção "Planilha Orçamentária" (equivalente à aba PLAN ORÇA da planilha de referência):
 * árvore de módulos e itens com decomposição MO/MAT/SERV, subtotais por módulo,
 * percentual de participação (curva ABC) e preço com BDI.
 * Todos os valores são calculados a partir da EAP — nunca digitados em dois lugares.
 */
data class OrcamentoDetailDto(
    val id: UUID,
    val titulo: String,
    val status: String,
    val bdi: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
    val modulos: List<ModuloPlanilhaDto>,
    val totais: PlanilhaTotaisDto
)

data class ModuloPlanilhaDto(
    val id: UUID,
    val tipoModulo: String,
    val nome: String,
    val itens: List<EapItemDetailDto>,
    /** Somatórios dos itens folha do módulo (MO/MAT/SERV × quantidade). */
    val totalMo: BigDecimal,
    val totalMat: BigDecimal,
    val totalSrv: BigDecimal,
    val totalCusto: BigDecimal,
    /** totalCusto × (1 + BDI). */
    val totalPreco: BigDecimal,
    /** Participação do módulo no custo direto total (0..1, 4 casas). */
    val percentual: BigDecimal
)

data class EapItemDetailDto(
    val id: UUID,
    val codigoItem: String,
    val descricao: String,
    val marca: String?,
    val unidade: String?,
    val quantidade: BigDecimal,
    val valorMo: BigDecimal,
    val valorMat: BigDecimal,
    val valorSrv: BigDecimal,
    /** MO + MAT + SERV (por unidade). */
    val valorUnitario: BigDecimal,
    /** valorUnitario × quantidade. */
    val custoTotal: BigDecimal,
    /** custoTotal × (1 + BDI). */
    val precoTotal: BigDecimal,
    val observacoes: String?,
    val composicoes: List<ComposicaoDetailDto>,
    val subItens: List<EapItemDetailDto>
)

data class ComposicaoDetailDto(
    val id: UUID,
    val tipoInsumo: String,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    val custoUnitarioInsumo: BigDecimal,
    /** coeficiente × custo unitário. */
    val custoTotalInsumo: BigDecimal
)

data class PlanilhaTotaisDto(
    val totalMo: BigDecimal,
    val totalMat: BigDecimal,
    val totalSrv: BigDecimal,
    /** Soma dos itens folha (sem BDI). */
    val custoDireto: BigDecimal,
    /** custoDireto × (1 + BDI). */
    val precoComBdi: BigDecimal
)
