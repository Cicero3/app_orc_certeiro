package com.example.api.orcamentos.presentation

import com.example.api.auth.domain.User
import com.example.api.orcamentos.domain.AmbienteProjeto
import com.example.api.orcamentos.infrastructure.AmbienteProjetoRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import com.example.api.orcamentos.presentation.dto.AmbienteRequest
import com.example.api.orcamentos.presentation.dto.AmbienteResponse
import com.example.api.orcamentos.presentation.mapper.toResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class AmbientesController(
    private val orcamentoRepository: OrcamentoRepository,
    private val ambienteRepository: AmbienteProjetoRepository
) {

    @PostMapping("/orcamentos/{orcamentoId}/ambientes")
    @Transactional
    fun adicionarAmbiente(
        @PathVariable orcamentoId: UUID,
        @RequestBody request: AmbienteRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<AmbienteResponse> {
        val orcamento = orcamentoRepository.findById(orcamentoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (orcamento.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val ambiente = AmbienteProjeto(
            nomeAmbiente = request.nomeAmbiente,
            largura = request.largura,
            comprimento = request.comprimento,
            peDireito = request.peDireito
        )
        orcamento.adicionarAmbiente(ambiente)
        val salvo = orcamentoRepository.save(orcamento)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo.ambientes.last().toResponse())
    }

    @PutMapping("/ambientes/{id}")
    @Transactional
    fun atualizarAmbiente(
        @PathVariable id: UUID,
        @RequestBody request: AmbienteRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<AmbienteResponse> {
        val ambiente = ambienteRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (ambiente.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        ambiente.atualizarDimensoes(
            novaLargura = request.largura,
            novoComprimento = request.comprimento,
            novoPeDireito = request.peDireito
        )
        ambiente.nomeAmbiente = request.nomeAmbiente
        
        val salvo = ambienteRepository.save(ambiente)
        return ResponseEntity.ok(salvo.toResponse())
    }
}
