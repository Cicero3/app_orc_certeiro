package com.example.api.cpus.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.UUID

class CpuPropriaTest {

    private val ownerId = UUID.randomUUID()

    private fun servente() = FuncaoSalarial(ownerId = ownerId, nome = "Servente", valorHora = BigDecimal("20.42"))
    private fun pedreiro() = FuncaoSalarial(ownerId = ownerId, nome = "Pedreiro", valorHora = BigDecimal("25.38"))

    /**
     * Reproduz a composição auxiliar "Argamassa 1:2:8" da aba 3. CPU com os valores CORRETOS:
     * a planilha original soma R$ 1.580,29 porque o SUM invade o bloco da composição vizinha;
     * o valor certo dos insumos listados é R$ 749,215/m³.
     */
    @Test
    fun `deve calcular composicao auxiliar de argamassa pelos insumos`() {
        val argamassa = CpuPropria(ownerId = ownerId, codigo = "AUX-01", descricao = "Argamassa 1:2:8", unidade = "M3", isAuxiliar = true)
        argamassa.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MAO_DE_OBRA, descricao = "Servente | preparação", unidade = "HR", coeficiente = BigDecimal("11.1"), funcaoSalarial = servente()))
        argamassa.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Cal hidratada CH-I", unidade = "KG", coeficiente = BigDecimal("171.13"), custoUnitario = BigDecimal("1.40")))
        argamassa.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Areia média", unidade = "M3", coeficiente = BigDecimal("1.14"), custoUnitario = BigDecimal("130")))
        argamassa.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Cimento CPII-32", unidade = "KG", coeficiente = BigDecimal("192.53"), custoUnitario = BigDecimal("0.70")))

        // MO: 11.1 × 20.42 = 226.662
        assertEquals(0, BigDecimal("226.662").compareTo(argamassa.valorMo))
        // MAT: 239.582 + 148.20 + 134.771 = 522.553
        assertEquals(0, BigDecimal("522.553").compareTo(argamassa.valorMat))
        // Total correto: 749.215 (a planilha dizia 1580.2858 por erro de intervalo no SUM)
        assertEquals(0, BigDecimal("749.215").compareTo(argamassa.valorUnitario))
    }

    @Test
    fun `deve usar composicao auxiliar como insumo de outra CPU`() {
        val argamassa = CpuPropria(ownerId = ownerId, codigo = "AUX-01", descricao = "Argamassa 1:2:8", unidade = "M3", isAuxiliar = true)
        argamassa.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Insumos agregados", unidade = "M3", coeficiente = BigDecimal("1"), custoUnitario = BigDecimal("749.215")))

        val alvenaria = CpuPropria(ownerId = ownerId, codigo = "3.1", descricao = "Assentamento de alvenaria", unidade = "M2")
        alvenaria.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MAO_DE_OBRA, descricao = "Pedreiro", unidade = "HR", coeficiente = BigDecimal("1.61"), funcaoSalarial = pedreiro()))
        // 0.008 m³ de argamassa por m² de alvenaria — o custo vem da composição auxiliar
        alvenaria.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Argamassa 1:2:8", unidade = "M3", coeficiente = BigDecimal("0.008"), cpuReferencia = argamassa))

        // MO: 1.61 × 25.38 = 40.8618 | MAT: 0.008 × 749.215 = 5.99372
        assertEquals(0, BigDecimal("40.8618").compareTo(alvenaria.valorMo))
        assertEquals(0, BigDecimal("5.99372").compareTo(alvenaria.valorMat))
        assertEquals(0, BigDecimal("46.85552").compareTo(alvenaria.valorUnitario))
    }

    @Test
    fun `funcao salarial tem prioridade sobre custo digitado no insumo de MO`() {
        val cpu = CpuPropria(ownerId = ownerId, codigo = "X", descricao = "Teste", unidade = "UN")
        cpu.adicionarInsumo(
            CpuInsumo(
                tipoInsumo = TipoInsumoCpu.MAO_DE_OBRA, descricao = "Servente", unidade = "HR",
                coeficiente = BigDecimal("2"), custoUnitario = BigDecimal("999"), funcaoSalarial = servente()
            )
        )
        // 2 × 20.42 (da tabela salarial), não 2 × 999
        assertEquals(0, BigDecimal("40.84").compareTo(cpu.valorMo))
    }

    @Test
    fun `composicao auxiliar nao pode referenciar outra composicao`() {
        val aux1 = CpuPropria(ownerId = ownerId, codigo = "AUX-01", descricao = "Argamassa", unidade = "M3", isAuxiliar = true)
        val aux2 = CpuPropria(ownerId = ownerId, codigo = "AUX-02", descricao = "Concreto", unidade = "M3", isAuxiliar = true)

        assertThrows<IllegalStateException> {
            aux1.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Ref", unidade = "M3", coeficiente = BigDecimal.ONE, cpuReferencia = aux2))
        }
    }

    @Test
    fun `somente composicoes auxiliares podem ser referenciadas`() {
        val comum = CpuPropria(ownerId = ownerId, codigo = "3.1", descricao = "Alvenaria", unidade = "M2", isAuxiliar = false)
        val outra = CpuPropria(ownerId = ownerId, codigo = "3.2", descricao = "Reboco", unidade = "M2")

        assertThrows<IllegalStateException> {
            outra.adicionarInsumo(CpuInsumo(tipoInsumo = TipoInsumoCpu.MATERIAL, descricao = "Ref", unidade = "M2", coeficiente = BigDecimal.ONE, cpuReferencia = comum))
        }
    }
}
