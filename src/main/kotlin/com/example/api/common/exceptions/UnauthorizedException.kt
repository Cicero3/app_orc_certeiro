package com.example.api.common.exceptions

/** Falha de autenticação (ex.: refresh token inválido/expirado/revogado). Mapeada para 401. */
class UnauthorizedException(message: String) : RuntimeException(message)
