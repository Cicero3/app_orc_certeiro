package com.example.api.common.dto

import java.time.Instant

data class ErrorResponse(
    val error: ErrorDetail
) {
    data class ErrorDetail(
        val code: String,
        val message: String,
        val timestamp: Instant = Instant.now(),
        val path: String? = null
    )
}