package com.example.api.cpus.api

import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.cpus.api.dto.AplicarEncargosDto
import com.example.api.cpus.api.dto.CpuDetailDto
import com.example.api.cpus.api.dto.CpuSummaryDto
import com.example.api.cpus.api.dto.CpuUpsertDto
import com.example.api.cpus.api.dto.FuncaoSalarialDto
import com.example.api.cpus.api.dto.FuncaoSalarialUpsertDto
import com.example.api.cpus.application.GerenciarCpuUseCase
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "CPUs Próprias", description = "Composições de preço unitário do usuário e tabela salarial por função")
@RestController
@RequestMapping("/api/v1")
class CpusController(
    private val gerenciarCpuUseCase: GerenciarCpuUseCase
) {

    // ---------- CPUs ----------

    @GetMapping("/cpus")
    fun listarCpus(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<Page<CpuSummaryDto>>> {
        val result = gerenciarCpuUseCase.listarCpus(user.id, search, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse(data = result))
    }

    @GetMapping("/cpus/{id}")
    fun detalharCpu(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<CpuDetailDto>> =
        ResponseEntity.ok(ApiResponse(data = gerenciarCpuUseCase.detalharCpu(user.id, id)))

    @PostMapping("/cpus")
    fun criarCpu(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: CpuUpsertDto
    ): ResponseEntity<ApiResponse<CpuDetailDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = gerenciarCpuUseCase.criarCpu(user.id, dto)))

    @PutMapping("/cpus/{id}")
    fun atualizarCpu(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: CpuUpsertDto
    ): ResponseEntity<ApiResponse<CpuDetailDto>> =
        ResponseEntity.ok(ApiResponse(data = gerenciarCpuUseCase.atualizarCpu(user.id, id, dto)))

    @DeleteMapping("/cpus/{id}")
    fun excluirCpu(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        gerenciarCpuUseCase.excluirCpu(user.id, id)
        return ResponseEntity.noContent().build()
    }

    // ---------- Funções salariais ----------

    @GetMapping("/funcoes-salariais")
    fun listarFuncoes(
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<List<FuncaoSalarialDto>>> =
        ResponseEntity.ok(ApiResponse(data = gerenciarCpuUseCase.listarFuncoes(user.id)))

    @PostMapping("/funcoes-salariais")
    fun criarFuncao(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: FuncaoSalarialUpsertDto
    ): ResponseEntity<ApiResponse<FuncaoSalarialDto>> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse(data = gerenciarCpuUseCase.criarFuncao(user.id, dto)))

    @PutMapping("/funcoes-salariais/{id}")
    fun atualizarFuncao(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: FuncaoSalarialUpsertDto
    ): ResponseEntity<ApiResponse<FuncaoSalarialDto>> =
        ResponseEntity.ok(ApiResponse(data = gerenciarCpuUseCase.atualizarFuncao(user.id, id, dto)))

    /** Aplica encargos sociais em lote por tipo de contratação (planilha 004). */
    @PostMapping("/funcoes-salariais/aplicar-encargos")
    fun aplicarEncargos(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody dto: AplicarEncargosDto
    ): ResponseEntity<ApiResponse<List<FuncaoSalarialDto>>> =
        ResponseEntity.ok(ApiResponse(data = gerenciarCpuUseCase.aplicarEncargos(user.id, dto.horistaPct, dto.mensalistaPct)))

    @DeleteMapping("/funcoes-salariais/{id}")
    fun excluirFuncao(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<Void> {
        gerenciarCpuUseCase.excluirFuncao(user.id, id)
        return ResponseEntity.noContent().build()
    }
}
