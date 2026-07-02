package com.example.api.orcamentos.api

import com.example.api.auth.domain.User
import com.example.api.common.dto.ApiResponse
import com.example.api.orcamentos.api.dto.OrcamentoCreateDto
import com.example.api.orcamentos.api.dto.OrcamentoSummaryDto
import com.example.api.orcamentos.application.GerenciarOrcamentoUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Orçamentos", description = "Gerenciamento de orçamentos")
@RestController
@RequestMapping("/api/v1/orcamentos")
class OrcamentosController(
    private val gerenciarOrcamentoUseCase: GerenciarOrcamentoUseCase
) {

    @PostMapping
    fun criar(
        @AuthenticationPrincipal user: User,
        @RequestBody dto: OrcamentoCreateDto
    ): ResponseEntity<ApiResponse<OrcamentoSummaryDto>> {
        val result = gerenciarOrcamentoUseCase.criarOrcamento(user, dto)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse(data = result))
    }

    @GetMapping
    fun listarMeus(
        @AuthenticationPrincipal user: User
    ): ResponseEntity<ApiResponse<List<OrcamentoSummaryDto>>> {
        val result = gerenciarOrcamentoUseCase.listarMeusOrcamentos(user.id)
        return ResponseEntity.ok(ApiResponse(data = result))
    }
}
