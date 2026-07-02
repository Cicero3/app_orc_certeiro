package com.example.api.orcamentos.presentation.dto

import java.math.BigDecimal
import java.util.UUID

// -- DTOs de Leitura (Response) --

data class OrcamentoResponse(
    val id: UUID,
    val titulo: String,
    val bdi: BigDecimal,
    val status: String,
    val valorTotal: BigDecimal,
    val modulos: List<OrcamentoModuloResponse>,
    val ambientes: List<AmbienteResponse>
)

data class OrcamentoModuloResponse(
    val id: UUID,
    val tipoModulo: String,
    val nome: String,
    val eapItens: List<EapItemResponse> // Apenas os itens raiz do módulo
)

data class EapItemResponse(
    val id: UUID,
    val codigoItem: String,
    val descricao: String,
    val marca: String?,
    val unidade: String?,
    val quantidade: BigDecimal,
    val valorUnitario: BigDecimal,
    val custoTotal: BigDecimal,
    val subItens: List<EapItemResponse> // Lista recursiva
)

data class AmbienteResponse(
    val id: UUID,
    val nomeAmbiente: String,
    val largura: BigDecimal,
    val comprimento: BigDecimal,
    val peDireito: BigDecimal,
    val areaPisoForro: BigDecimal,
    val areaParede: BigDecimal
)

data class CpuResponse(
    val id: UUID,
    val tipoInsumo: String,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    val custoUnitarioInsumo: BigDecimal,
    val custoTotalInsumo: BigDecimal
)

// -- DTOs de Escrita (Request) --

data class CriarOrcamentoRequest(
    val titulo: String,
    val bdi: BigDecimal
)

data class AdicionarModuloRequest(
    val tipoModulo: String,
    val nome: String
)

data class EapItemRequest(
    val codigoItem: String,
    val descricao: String,
    val marca: String?,
    val unidade: String?,
    val quantidade: BigDecimal,
    val valorMo: BigDecimal,
    val valorMat: BigDecimal,
    val valorSrv: BigDecimal,
    val observacoes: String?
)

data class AtualizarEapItemRequest(
    val quantidade: BigDecimal,
    val valorMo: BigDecimal,
    val valorMat: BigDecimal,
    val valorSrv: BigDecimal
)

data class AmbienteRequest(
    val nomeAmbiente: String,
    val largura: BigDecimal,
    val comprimento: BigDecimal,
    val peDireito: BigDecimal
)

data class CpuRequest(
    val tipoInsumo: String,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    val custoUnitarioInsumo: BigDecimal
)

data class CriarCpuRequest(
    val tipoInsumo: String,
    val codigo: String,
    val descricao: String,
    val unidade: String,
    val coeficiente: BigDecimal,
    val custoUnitario: BigDecimal
)

data class ImportarCatalogoRequest(
    val catalogoItemId: UUID,
    val quantidade: BigDecimal
)
