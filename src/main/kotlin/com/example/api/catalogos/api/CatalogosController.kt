package com.example.api.catalogos.api

import com.example.api.catalogos.api.dto.CatalogoInsumoDto
import com.example.api.catalogos.api.dto.CatalogoItemDetailDto
import com.example.api.catalogos.api.dto.CatalogoItemSummaryDto
import com.example.api.catalogos.infrastructure.CatalogoItemRepository
import com.example.api.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "Catálogo SINAPI", description = "Consultas à base de dados do SINAPI")
@RestController
@RequestMapping("/api/v1/catalogos/itens")
class CatalogosController(
    private val catalogoItemRepository: CatalogoItemRepository
) {
    @GetMapping
    fun searchItens(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<Page<CatalogoItemSummaryDto>>> {
        val pageable = PageRequest.of(page, size, Sort.by("descricao").ascending())

        val resultPage = if (search.isNullOrBlank()) {
            catalogoItemRepository.findAll(pageable)
        } else {
            val term = "%$search%"
            catalogoItemRepository.findByDescricaoContainingIgnoreCaseOrCodigoContainingIgnoreCase(term, term, pageable)
        }

        val dtoPage = resultPage.map { item ->
            CatalogoItemSummaryDto(
                id = item.id,
                codigo = item.codigo,
                descricao = item.descricao,
                unidade = item.unidade,
                valorTotal = item.valorMo + item.valorMat + item.valorSrv
            )
        }

        return ResponseEntity.ok(ApiResponse(data = dtoPage))
    }

    @GetMapping("/{id}")
    fun getItemDetails(@PathVariable id: UUID): ResponseEntity<ApiResponse<CatalogoItemDetailDto>> {
        val item = catalogoItemRepository.findByIdWithInsumos(id)
            ?: throw EntityNotFoundException("Item do catálogo não encontrado: $id")

        val dto = CatalogoItemDetailDto(
            id = item.id,
            codigo = item.codigo,
            descricao = item.descricao,
            unidade = item.unidade,
            valorMo = item.valorMo,
            valorMat = item.valorMat,
            valorSrv = item.valorSrv,
            insumos = item.insumos.map { insumo ->
                CatalogoInsumoDto(
                    id = insumo.id,
                    tipoInsumo = insumo.tipoInsumo,
                    codigo = insumo.codigo,
                    descricao = insumo.descricao,
                    unidade = insumo.unidade,
                    coeficiente = insumo.coeficiente,
                    custoUnitario = insumo.custoUnitario,
                    custoTotal = insumo.coeficiente * insumo.custoUnitario
                )
            }
        )

        return ResponseEntity.ok(ApiResponse(data = dto))
    }
}
