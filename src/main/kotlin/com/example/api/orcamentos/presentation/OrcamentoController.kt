package com.example.api.orcamentos.presentation

import com.example.api.auth.domain.User
import com.example.api.orcamentos.domain.Orcamento
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import com.example.api.orcamentos.presentation.dto.CriarOrcamentoRequest
import com.example.api.orcamentos.presentation.dto.OrcamentoResponse
import com.example.api.orcamentos.presentation.mapper.toResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orcamentos")
class OrcamentoController(
    private val orcamentoRepository: OrcamentoRepository
) {

    @GetMapping("/{id}")
    fun getOrcamento(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<OrcamentoResponse> {
        val orcamento = orcamentoRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        // Em um cenário real com RLS, o banco filtraria o tenant_id, mas vamos fazer um check básico aqui:
        if (orcamento.ownerId != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(orcamento.toResponse())
    }

    @PostMapping
    fun criarOrcamento(
        @RequestBody request: CriarOrcamentoRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<OrcamentoResponse> {
        val novoOrcamento = Orcamento(
            tenantId = user.id,
            ownerId = user.id,
            titulo = request.titulo,
            bdi = request.bdi
        )
        val salvo = orcamentoRepository.save(novoOrcamento)
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo.toResponse())
    }
}
