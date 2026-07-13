package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.BdiUpdateDto
import com.example.api.orcamentos.api.dto.EapItemCreateDto
import com.example.api.orcamentos.api.dto.EapItemFromCatalogoDto
import com.example.api.orcamentos.api.dto.EapItemFromCpuDto
import com.example.api.orcamentos.api.dto.EapItemUpdateDto
import com.example.api.orcamentos.api.dto.ModuloCreateDto
import com.example.api.orcamentos.api.dto.OrcamentoCreateDto
import com.example.api.orcamentos.api.dto.OrcamentoDetailDto
import com.example.api.orcamentos.api.dto.OrcamentoSummaryDto
import com.example.api.orcamentos.api.dto.StatusActionDto
import com.example.api.orcamentos.api.dto.DimensionamentoDto
import com.example.api.orcamentos.application.AlterarStatusOrcamentoUseCase
import com.example.api.orcamentos.application.DimensionamentoUseCase
import com.example.api.orcamentos.application.GerenciarEapUseCase
import com.example.api.orcamentos.application.GerenciarOrcamentoUseCase
import java.math.BigDecimal
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Orçamentos", description = "Gerenciamento de orçamentos, EAP e planilha orçamentária")
@RestController
@RequestMapping("/api/v1/orcamentos")
class OrcamentosController(
    private val gerenciarOrcamentoUseCase: GerenciarOrcamentoUseCase,
    private val gerenciarEapUseCase: GerenciarEapUseCase,
    private val alterarStatusOrcamentoUseCase: AlterarStatusOrcamentoUseCase,
    private val dimensionamentoUseCase: DimensionamentoUseCase
) {

    // ---------- Orçamento ----------

    @PostMapping
    fun criar(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: OrcamentoCreateDto
    ): ResponseEntity<ApiResponse<OrcamentoSummaryDto>> {
        val result = gerenciarOrcamentoUseCase.criarOrcamento(user.id, dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    @GetMapping
    fun listarMeus(
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<List<OrcamentoSummaryDto>>> {
        val result = gerenciarOrcamentoUseCase.listarMeusOrcamentos(user.id)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    /** Planilha orçamentária completa: módulos, itens, MO/MAT/SERV, % de participação e preço com BDI. */
    @GetMapping("/{id}")
    fun detalhar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    @PatchMapping("/{id}/bdi")
    fun atualizarBdi(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: BdiUpdateDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        val result = gerenciarOrcamentoUseCase.atualizarBdi(user.id, id, dto.bdi)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    /** Transições da máquina de estados: SOLICITAR_APROVACAO, APROVAR, REJEITAR, ENVIAR_CLIENTE, ACEITAR_CLIENTE, RECUSAR_CLIENTE, CANCELAR. */
    @PostMapping("/{id}/status")
    fun alterarStatus(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: StatusActionDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        alterarStatusOrcamentoUseCase.execute(id, user.id, user.role == "ADMIN", dto.acao)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    /** Dimensionamento de equipes e prazos a partir dos coeficientes de MO da CPU (aba DIM EQP). */
    @GetMapping("/{id}/dimensionamento")
    fun dimensionamento(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(defaultValue = "8") jornada: BigDecimal
    ): ResponseEntity<ApiResponse<DimensionamentoDto>> =
        ResponseEntity.ok(ApiResponse(data = dimensionamentoUseCase.calcular(user.id, id, jornada)))

    @DeleteMapping("/{id}")
    fun excluir(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        gerenciarOrcamentoUseCase.excluirOrcamento(user.id, id)
        return ResponseEntity.noContent().build()
    }

    // ---------- Módulos ----------

    @PostMapping("/{id}/modulos")
    fun adicionarModulo(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: ModuloCreateDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        gerenciarEapUseCase.adicionarModulo(user.id, id, dto)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    @DeleteMapping("/{id}/modulos/{moduloId}")
    fun removerModulo(
        @PathVariable id: UUID,
        @PathVariable moduloId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        gerenciarEapUseCase.removerModulo(user.id, id, moduloId)
        return ResponseEntity.noContent().build()
    }

    // ---------- Itens da EAP ----------

    @PostMapping("/{id}/itens")
    fun adicionarItem(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: EapItemCreateDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        gerenciarEapUseCase.adicionarItem(user.id, id, dto)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    /** Copia um item do catálogo SINAPI para a EAP, trazendo os insumos como CPU. */
    @PostMapping("/{id}/itens/catalogo")
    fun copiarDoCatalogo(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: EapItemFromCatalogoDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        gerenciarEapUseCase.copiarDoCatalogo(user.id, id, dto)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    /** Copia uma CPU própria do usuário para a EAP, com snapshot dos insumos. */
    @PostMapping("/{id}/itens/cpu-propria")
    fun copiarDaCpuPropria(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: EapItemFromCpuDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        gerenciarEapUseCase.copiarDaCpuPropria(user.id, id, dto)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    @PutMapping("/{id}/itens/{itemId}")
    fun atualizarItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: EapItemUpdateDto
    ): ResponseEntity<ApiResponse<OrcamentoDetailDto>> {
        gerenciarEapUseCase.atualizarItem(user.id, id, itemId, dto)
        val result = gerenciarOrcamentoUseCase.detalharOrcamento(user.id, id)
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    fun removerItem(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        gerenciarEapUseCase.removerItem(user.id, id, itemId)
        return ResponseEntity.noContent().build()
    }
}
