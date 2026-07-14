package com.example.api.orcamentos.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.CronogramaDto
import com.example.api.orcamentos.api.dto.CronogramaSalvarDto
import com.example.api.orcamentos.application.CronogramaUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Cronograma", description = "Cronograma físico-financeiro: previsto × real por período e curva S")
@RestController
@RequestMapping("/api/v1/orcamentos/{id}/cronograma")
class CronogramaController(
    private val cronogramaUseCase: CronogramaUseCase
) {

    @GetMapping
    fun obter(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(required = false) periodos: Int?
    ): ResponseEntity<ApiResponse<CronogramaDto>> =
        ResponseEntity.ok(ApiResponse(data = cronogramaUseCase.obter(user.id, id, periodos)))

    /** Upsert em lote das células (módulo × período) com % previsto e % real. */
    @PutMapping
    fun salvar(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(required = false) periodos: Int?,
        @Valid @RequestBody dto: CronogramaSalvarDto
    ): ResponseEntity<ApiResponse<CronogramaDto>> =
        ResponseEntity.ok(ApiResponse(data = cronogramaUseCase.salvar(user.id, id, dto, periodos)))
}
