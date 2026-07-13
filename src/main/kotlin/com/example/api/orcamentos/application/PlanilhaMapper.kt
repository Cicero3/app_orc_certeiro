package com.example.api.orcamentos.application

import com.example.api.orcamentos.api.dto.ComposicaoDetailDto
import com.example.api.orcamentos.api.dto.EapItemDetailDto
import com.example.api.orcamentos.api.dto.ModuloPlanilhaDto
import com.example.api.orcamentos.api.dto.OrcamentoDetailDto
import com.example.api.orcamentos.api.dto.PlanilhaTotaisDto
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.domain.OrcamentoModulo
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Projeta um Orçamento na visão "Planilha Orçamentária" (aba PLAN ORÇA):
 * decomposição MO/MAT/SERV, subtotais por módulo, percentual de participação
 * e preço com BDI. Todos os números derivam da EAP — fonte única de verdade.
 */
object PlanilhaMapper {

    fun toDetailDto(orcamento: Orcamento): OrcamentoDetailDto {
        val fatorBdi = BigDecimal.ONE.add(orcamento.bdi)
        val custoDireto = orcamento.custoDireto

        val modulos = orcamento.modulos.map { toModuloDto(it, fatorBdi, custoDireto) }

        val totais = PlanilhaTotaisDto(
            totalMo = modulos.sumOf { it.totalMo },
            totalMat = modulos.sumOf { it.totalMat },
            totalSrv = modulos.sumOf { it.totalSrv },
            custoDireto = custoDireto,
            precoComBdi = custoDireto.multiply(fatorBdi)
        )

        return OrcamentoDetailDto(
            id = orcamento.id,
            titulo = orcamento.titulo,
            status = orcamento.status.name,
            bdi = orcamento.bdi,
            createdAt = orcamento.createdAt,
            updatedAt = orcamento.updatedAt,
            modulos = modulos,
            totais = totais
        )
    }

    private fun toModuloDto(
        modulo: OrcamentoModulo,
        fatorBdi: BigDecimal,
        custoDiretoGeral: BigDecimal
    ): ModuloPlanilhaDto {
        val folhas = modulo.eapItens.flatMap { coletarFolhas(it) }
        val totalMo = folhas.sumOf { it.valorMo.multiply(it.quantidade) }
        val totalMat = folhas.sumOf { it.valorMat.multiply(it.quantidade) }
        val totalSrv = folhas.sumOf { it.valorSrv.multiply(it.quantidade) }
        val totalCusto = totalMo.add(totalMat).add(totalSrv)

        val percentual = if (custoDiretoGeral.signum() == 0) BigDecimal.ZERO
        else totalCusto.divide(custoDiretoGeral, 4, RoundingMode.HALF_UP)

        return ModuloPlanilhaDto(
            id = modulo.id,
            tipoModulo = modulo.tipoModulo.name,
            nome = modulo.nome,
            itens = modulo.eapItens.map { toItemDto(it, fatorBdi) },
            totalMo = totalMo,
            totalMat = totalMat,
            totalSrv = totalSrv,
            totalCusto = totalCusto,
            totalPreco = totalCusto.multiply(fatorBdi),
            percentual = percentual
        )
    }

    private fun toItemDto(item: EapItem, fatorBdi: BigDecimal): EapItemDetailDto =
        EapItemDetailDto(
            id = item.id,
            codigoItem = item.codigoItem,
            descricao = item.descricao,
            marca = item.marca,
            unidade = item.unidade,
            quantidade = item.quantidade,
            valorMo = item.valorMo,
            valorMat = item.valorMat,
            valorSrv = item.valorSrv,
            valorUnitario = item.valorUnitario,
            custoTotal = item.custoTotal,
            precoTotal = item.custoTotal.multiply(fatorBdi),
            observacoes = item.observacoes,
            composicoes = item.composicoes.map {
                ComposicaoDetailDto(
                    id = it.id,
                    tipoInsumo = it.tipoInsumo.name,
                    descricao = it.descricao,
                    unidade = it.unidade,
                    coeficiente = it.coeficiente,
                    custoUnitarioInsumo = it.custoUnitarioInsumo,
                    custoTotalInsumo = it.custoTotalInsumo
                )
            },
            subItens = item.subItens.map { toItemDto(it, fatorBdi) }
        )

    private fun coletarFolhas(item: EapItem): List<EapItem> =
        if (item.subItens.isEmpty()) listOf(item)
        else item.subItens.flatMap { coletarFolhas(it) }
}
