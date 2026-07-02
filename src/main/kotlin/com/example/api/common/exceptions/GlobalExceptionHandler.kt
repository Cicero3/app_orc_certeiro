package com.example.api.common.exceptions

import com.example.api.common.dto.ErrorResponse
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import com.example.api.orcamentos.domain.DomainSecurityException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request at ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "BAD_REQUEST",
                    message = ex.message ?: "Invalid request",
                    path = request.requestURI
                )
            )
        )
    }

    @ExceptionHandler(DomainSecurityException::class)
    fun handleDomainSecurity(
        ex: DomainSecurityException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Domain Security Violation at ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "DOMAIN_SECURITY_VIOLATION",
                    message = ex.message ?: "Ação bloqueada pelas regras de negócio",
                    path = request.requestURI
                )
            )
        )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized at ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "UNAUTHORIZED",
                    message = ex.message ?: "Não autorizado",
                    path = request.requestURI
                )
            )
        )
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(
        ex: EntityNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Not found at ${request.requestURI}: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "NOT_FOUND",
                    message = ex.message ?: "Resource not found",
                    path = request.requestURI
                )
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        logger.warn("Validation failed at ${request.requestURI}: $message")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "VALIDATION_ERROR",
                    message = message,
                    path = request.requestURI
                )
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error at ${request.requestURI}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = ErrorResponse.ErrorDetail(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    path = request.requestURI
                )
            )
        )
    }
}