package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.AnaliseRiscoDto
import com.example.api.orcamentos.api.dto.AplicarContingenciaDto
import com.example.api.orcamentos.api.dto.RiscoDto
import com.example.api.orcamentos.api.dto.RiscoUpsertDto
import com.example.api.orcamentos.application.AnaliseRiscoUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Riscos", description = "Matriz de risco e contingência por Simulação de Monte Carlo")
@RestController
@RequestMapping("/api/v1/orcamentos/{id}/riscos")
class RiscosController(
    private val analiseRiscoUseCase: AnaliseRiscoUseCase
) {

    /** Riscos classificados (matriz P×I), valor esperado e Simulação de Monte Carlo. */
    @GetMapping("/analise")
    fun analisar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(defaultValue = "10000") iteracoes: Int,
        @RequestParam(required = false) seed: Long?
    ): ResponseEntity<ApiResponse<AnaliseRiscoDto>> =
        ResponseEntity.ok(ApiResponse(data = analiseRiscoUseCase.analisar(user.id, id, iteracoes, seed)))

    @PostMapping
    fun adicionar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: RiscoUpsertDto
    ): ResponseEntity<ApiResponse<RiscoDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = analiseRiscoUseCase.adicionarRisco(user.id, id, dto)))

    @PutMapping("/{riscoId}")
    fun atualizar(
        @PathVariable id: UUID,
        @PathVariable riscoId: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: RiscoUpsertDto
    ): ResponseEntity<ApiResponse<RiscoDto>> =
        ResponseEntity.ok(ApiResponse(data = analiseRiscoUseCase.atualizarRisco(user.id, id, riscoId, dto)))

    @DeleteMapping("/{riscoId}")
    fun remover(
        @PathVariable id: UUID,
        @PathVariable riscoId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        analiseRiscoUseCase.removerRisco(user.id, id, riscoId)
        return ResponseEntity.noContent().build()
    }

    /** Grava o percentil escolhido (P50/P80/P90/P95) como contingência na formação de preço. */
    @PostMapping("/aplicar-contingencia")
    fun aplicarContingencia(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: AplicarContingenciaDto
    ): ResponseEntity<ApiResponse<AnaliseRiscoDto>> =
        ResponseEntity.ok(ApiResponse(data = analiseRiscoUseCase.aplicarContingencia(user.id, id, dto)))
}
