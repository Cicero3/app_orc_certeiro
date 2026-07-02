package com.example.api.orcamentos.domain

/**
 * Exception lançada quando há uma violação de segurança do domínio (ex: tentar alterar um orçamento imutável).
 */
class DomainSecurityException(message: String) : RuntimeException(message)
