package com.example.api.common.dto

import org.springframework.data.domain.Page

object PageMeta {
    fun of(page: Page<*>): Map<String, Any> = mapOf(
        "page" to page.number,
        "size" to page.size,
        "totalElements" to page.totalElements,
        "totalPages" to page.totalPages
    )
}
