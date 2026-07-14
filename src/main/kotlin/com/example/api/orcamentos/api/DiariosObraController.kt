package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.DiarioObraDto
import com.example.api.orcamentos.api.dto.DiarioObraUpsertDto
import com.example.api.orcamentos.application.DiarioObraUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Diário de Obra", description = "RDO: clima, efetivo, equipamentos, atividades e ocorrências por dia")
@RestController
@RequestMapping("/api/v1/orcamentos/{id}/diarios")
class DiariosObraController(
    private val diarioObraUseCase: DiarioObraUseCase
) {

    @GetMapping
    fun listar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<List<DiarioObraDto>>> =
        ResponseEntity.ok(ApiResponse(data = diarioObraUseCase.listar(user.id, id)))

    /** Cria ou atualiza o RDO da data informada (upsert por orçamento+data). */
    @PostMapping
    fun salvar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: DiarioObraUpsertDto
    ): ResponseEntity<ApiResponse<DiarioObraDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = diarioObraUseCase.salvar(user.id, id, dto)))

    @DeleteMapping("/{diarioId}")
    fun excluir(
        @PathVariable id: UUID,
        @PathVariable diarioId: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        diarioObraUseCase.excluir(user.id, id, diarioId)
        return ResponseEntity.noContent().build()
    }
}
