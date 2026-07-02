package com.example.api.orcamentos.presentation

import com.example.api.auth.domain.User
import com.example.api.orcamentos.domain.ComposicaoPreco
import com.example.api.orcamentos.domain.TipoInsumo
import com.example.api.orcamentos.infrastructure.ComposicaoPrecoRepository
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.presentation.dto.CpuRequest
import com.example.api.orcamentos.presentation.dto.CpuResponse
import com.example.api.orcamentos.presentation.mapper.toResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class CpuController(
    private val eapItemRepository: EapItemRepository,
    private val cpuRepository: ComposicaoPrecoRepository
) {

    @PostMapping("/eap-itens/{eapItemId}/cpu")
    @Transactional
    fun adicionarComposicao(
        @PathVariable eapItemId: UUID,
        @RequestBody request: CpuRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<CpuResponse> {
        val eapItem = eapItemRepository.findById(eapItemId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (eapItem.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val cpu = ComposicaoPreco(
            tipoInsumo = TipoInsumo.valueOf(request.tipoInsumo),
            descricao = request.descricao,
            unidade = request.unidade,
            coeficiente = request.coeficiente,
            custoUnitarioInsumo = request.custoUnitarioInsumo
        )
        eapItem.adicionarComposicao(cpu)
        val salvo = eapItemRepository.save(eapItem)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo.composicoes.last().toResponse())
    }

    @GetMapping("/eap-itens/{eapItemId}/cpu")
    fun listarComposicoes(
        @PathVariable eapItemId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<List<CpuResponse>> {
        val eapItem = eapItemRepository.findById(eapItemId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (eapItem.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val cpus = eapItem.composicoes.map { it.toResponse() }
        return ResponseEntity.ok(cpus)
    }

    @PutMapping("/cpu/{id}")
    @Transactional
    fun atualizarComposicao(
        @PathVariable id: UUID,
        @RequestBody request: CpuRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<CpuResponse> {
        val cpu = cpuRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (cpu.eapItem?.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        cpu.atualizarValores(
            novoCoeficiente = request.coeficiente,
            novoCustoUnitario = request.custoUnitarioInsumo
        )
        
        val salvo = cpuRepository.save(cpu)
        return ResponseEntity.ok(salvo.toResponse())
    }
}
