package com.example.api.orcamentos.presentation

import com.example.api.auth.domain.User
import com.example.api.orcamentos.domain.EapItem
import com.example.api.orcamentos.domain.OrcamentoModulo
import com.example.api.orcamentos.domain.TipoModulo
import com.example.api.orcamentos.infrastructure.EapItemRepository
import com.example.api.orcamentos.infrastructure.OrcamentoModuloRepository
import com.example.api.orcamentos.infrastructure.OrcamentoRepository
import com.example.api.orcamentos.presentation.dto.AdicionarModuloRequest
import com.example.api.orcamentos.presentation.dto.AtualizarEapItemRequest
import com.example.api.orcamentos.presentation.dto.EapItemRequest
import com.example.api.orcamentos.presentation.dto.EapItemResponse
import com.example.api.orcamentos.presentation.dto.ImportarCatalogoRequest
import com.example.api.orcamentos.application.CopiarItemCatalogoUseCase
import com.example.api.orcamentos.presentation.dto.OrcamentoModuloResponse
import com.example.api.orcamentos.presentation.mapper.toResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class EapController(
    private val orcamentoRepository: OrcamentoRepository,
    private val orcamentoModuloRepository: OrcamentoModuloRepository,
    private val eapItemRepository: EapItemRepository,
    private val copiarItemCatalogoUseCase: CopiarItemCatalogoUseCase
) {

    @PostMapping("/orcamentos/{orcamentoId}/modulos")
    @Transactional
    fun adicionarModulo(
        @PathVariable orcamentoId: UUID,
        @RequestBody request: AdicionarModuloRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<OrcamentoModuloResponse> {
        val orcamento = orcamentoRepository.findById(orcamentoId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (orcamento.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val modulo = OrcamentoModulo(
            tipoModulo = TipoModulo.valueOf(request.tipoModulo),
            nome = request.nome
        )
        orcamento.adicionarModulo(modulo)
        val salvo = orcamentoRepository.save(orcamento) // Salva em cascata
        
        // Pega o último módulo adicionado (já com ID)
        val moduloSalvo = salvo.modulos.last()
        return ResponseEntity.status(HttpStatus.CREATED).body(moduloSalvo.toResponse())
    }

    @PostMapping("/modulos/{moduloId}/eap-itens")
    @Transactional
    fun adicionarEapItemNoModulo(
        @PathVariable moduloId: UUID,
        @RequestBody request: EapItemRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<EapItemResponse> {
        val modulo = orcamentoModuloRepository.findById(moduloId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (modulo.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val eapItem = EapItem(
            codigoItem = request.codigoItem,
            descricao = request.descricao,
            marca = request.marca,
            unidade = request.unidade,
            quantidade = request.quantidade,
            valorMo = request.valorMo,
            valorMat = request.valorMat,
            valorSrv = request.valorSrv,
            observacoes = request.observacoes
        )
        modulo.adicionarEapItem(eapItem)
        val salvo = orcamentoModuloRepository.save(modulo) // Cascade
        
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo.eapItens.last().toResponse())
    }

    @PostMapping("/eap-itens/{parentId}/sub-itens")
    @Transactional
    fun adicionarSubItemNaEap(
        @PathVariable parentId: UUID,
        @RequestBody request: EapItemRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<EapItemResponse> {
        val parent = eapItemRepository.findById(parentId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (parent.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val subItem = EapItem(
            codigoItem = request.codigoItem,
            descricao = request.descricao,
            marca = request.marca,
            unidade = request.unidade,
            quantidade = request.quantidade,
            valorMo = request.valorMo,
            valorMat = request.valorMat,
            valorSrv = request.valorSrv,
            observacoes = request.observacoes
        )
        parent.adicionarSubItem(subItem)
        val salvo = eapItemRepository.save(parent)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo.subItens.last().toResponse())
    }

    @PutMapping("/eap-itens/{id}")
    @Transactional
    fun atualizarEapItem(
        @PathVariable id: UUID,
        @RequestBody request: AtualizarEapItemRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<EapItemResponse> {
        val item = eapItemRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (item.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        // Chama o guard clause do domínio (validará se é negativo e se o status permite edição)
        item.atualizarValores(
            novaQuantidade = request.quantidade,
            mo = request.valorMo,
            mat = request.valorMat,
            srv = request.valorSrv
        )
        
        val salvo = eapItemRepository.save(item)
        return ResponseEntity.ok(salvo.toResponse())
    }

    @PostMapping("/modulos/{moduloId}/importar-catalogo")
    fun importarParaModulo(
        @PathVariable moduloId: UUID,
        @RequestBody request: ImportarCatalogoRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<EapItemResponse> {
        val modulo = orcamentoModuloRepository.findById(moduloId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (modulo.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val novoItem = copiarItemCatalogoUseCase.executar(
            catalogoItemId = request.catalogoItemId,
            quantidade = request.quantidade,
            moduloId = moduloId
        )
        return ResponseEntity.ok(novoItem.toResponse())
    }

    @PostMapping("/eap-itens/{parentId}/importar-catalogo")
    fun importarParaItem(
        @PathVariable parentId: UUID,
        @RequestBody request: ImportarCatalogoRequest,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<EapItemResponse> {
        val parent = eapItemRepository.findById(parentId).orElse(null)
            ?: return ResponseEntity.notFound().build()
            
        if (parent.orcamento?.ownerId != user.id) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

        val novoItem = copiarItemCatalogoUseCase.executar(
            catalogoItemId = request.catalogoItemId,
            quantidade = request.quantidade,
            parentId = parentId
        )
        return ResponseEntity.ok(novoItem.toResponse())
    }
}
