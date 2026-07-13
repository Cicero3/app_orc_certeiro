package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.CustoIndiretoDto
import com.example.api.orcamentos.api.dto.CustoIndiretoUpsertDto
import com.example.api.orcamentos.api.dto.CustosIndiretosResumoDto
import com.example.api.orcamentos.api.dto.FormacaoPrecoDto
import com.example.api.orcamentos.api.dto.FormacaoPrecoUpsertDto
import com.example.api.orcamentos.application.PrecificacaoUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Precificação", description = "Custos indiretos e formação de preço de venda (markup divisor + BDI)")
@RestController
@RequestMapping("/api/v1/orcamentos/{id}")
class PrecificacaoController(
    private val precificacaoUseCase: PrecificacaoUseCase
) {

    // ---------- Custos indiretos ----------

    @GetMapping("/custos-indiretos")
    fun listarCustosIndiretos(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<CustosIndiretosResumoDto>> =
        ResponseEntity.ok(ApiResponse(data = precificacaoUseCase.listarCustosIndiretos(user.id, id)))

    @PostMapping("/custos-indiretos")
    fun adicionarCustoIndireto(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: CustoIndiretoUpsertDto
    ): ResponseEntity<ApiResponse<CustoIndiretoDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = precificacaoUseCase.adicionarCustoIndireto(user.id, id, dto)))

    @PutMapping("/custos-indiretos/{custoId}")
    fun atualizarCustoIndireto(
        @PathVariable id: UUID,
        @PathVariable custoId: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: CustoIndiretoUpsertDto
    ): ResponseEntity<ApiResponse<CustoIndiretoDto>> =
        ResponseEntity.ok(ApiResponse(data = precificacaoUseCase.atualizarCustoIndireto(user.id, id, custoId, dto)))

    @DeleteMapping("/custos-indiretos/{custoId}")
    fun removerCustoIndireto(
        @PathVariable id: UUID,
        @PathVariable custoId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        precificacaoUseCase.removerCustoIndireto(user.id, id, custoId)
        return ResponseEntity.noContent().build()
    }

    // ---------- Formação de preço ----------

    @GetMapping("/formacao-preco")
    fun obterFormacaoPreco(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<FormacaoPrecoDto>> =
        ResponseEntity.ok(ApiResponse(data = precificacaoUseCase.obterFormacaoPreco(user.id, id)))

    @PutMapping("/formacao-preco")
    fun atualizarFormacaoPreco(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: FormacaoPrecoUpsertDto
    ): ResponseEntity<ApiResponse<FormacaoPrecoDto>> =
        ResponseEntity.ok(ApiResponse(data = precificacaoUseCase.atualizarFormacaoPreco(user.id, id, dto)))

    /** Aplica o BDI resultante (sobre o custo direto) no orçamento. */
    @PostMapping("/formacao-preco/aplicar-bdi")
    fun aplicarBdi(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<FormacaoPrecoDto>> =
        ResponseEntity.ok(ApiResponse(data = precificacaoUseCase.aplicarBdiCalculado(user.id, id)))
}
