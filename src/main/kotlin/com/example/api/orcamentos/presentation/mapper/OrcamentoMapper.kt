package com.example.api.orcamentos.presentation.mapper

import com.example.api.orcamentos.domain.*
import com.example.api.orcamentos.presentation.dto.*

fun Orcamento.toResponse() = OrcamentoResponse(
    id = this.id,
    titulo = this.titulo,
    bdi = this.bdi,
    status = this.status.name,
    valorTotal = this.valorTotal,
    modulos = this.modulos.map { it.toResponse() },
    ambientes = this.ambientes.map { it.toResponse() }
)

fun OrcamentoModulo.toResponse() = OrcamentoModuloResponse(
    id = this.id,
    tipoModulo = this.tipoModulo.name,
    nome = this.nome,
    eapItens = this.eapItens.filter { it.parent == null }.map { it.toResponse() } // Apenas itens raiz, subitens recursivos
)

fun EapItem.toResponse(): EapItemResponse = EapItemResponse(
    id = this.id,
    codigoItem = this.codigoItem,
    descricao = this.descricao,
    marca = this.marca,
    unidade = this.unidade,
    quantidade = this.quantidade,
    valorUnitario = this.valorUnitario,
    custoTotal = this.custoTotal,
    subItens = this.subItens.map { it.toResponse() }
)

fun AmbienteProjeto.toResponse() = AmbienteResponse(
    id = this.id,
    nomeAmbiente = this.nomeAmbiente,
    largura = this.largura,
    comprimento = this.comprimento,
    peDireito = this.peDireito,
    areaPisoForro = this.areaPisoForro,
    areaParede = this.areaParede
)

fun ComposicaoPreco.toResponse() = CpuResponse(
    id = this.id,
    tipoInsumo = this.tipoInsumo.name,
    descricao = this.descricao,
    unidade = this.unidade,
    coeficiente = this.coeficiente,
    custoUnitarioInsumo = this.custoUnitarioInsumo,
    custoTotalInsumo = this.custoTotalInsumo
)
