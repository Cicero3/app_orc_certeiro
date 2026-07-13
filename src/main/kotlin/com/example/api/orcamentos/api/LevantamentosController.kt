package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.LevantamentoCreateDto
import com.example.api.orcamentos.api.dto.LevantamentoDto
import com.example.api.orcamentos.application.LevantamentoUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Levantamentos", description = "Levantamentos de quantitativo das calculadoras, com vínculo à EAP")
@RestController
@RequestMapping("/api/v1/orcamentos/{id}/levantamentos")
class LevantamentosController(
    private val levantamentoUseCase: LevantamentoUseCase
) {

    @GetMapping
    fun listar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<List<LevantamentoDto>>> =
        ResponseEntity.ok(ApiResponse(data = levantamentoUseCase.listar(user.id, id)))

    /** Salva o levantamento e, se eapItemId for informado, aplica o resultado como quantidade do item. */
    @PostMapping
    fun salvar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: LevantamentoCreateDto
    ): ResponseEntity<ApiResponse<LevantamentoDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = levantamentoUseCase.salvar(user.id, id, dto)))

    @DeleteMapping("/{levantamentoId}")
    fun excluir(
        @PathVariable id: UUID,
        @PathVariable levantamentoId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        levantamentoUseCase.excluir(user.id, id, levantamentoId)
        return ResponseEntity.noContent().build()
    }
}
