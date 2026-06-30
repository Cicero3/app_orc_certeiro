package com.example.api.auth.api

import com.example.api.auth.api.dto.AuthResponse
import com.example.api.auth.api.dto.LoginRequest
import com.example.api.auth.api.dto.RegisterRequest
import com.example.api.auth.application.IssuedTokens
import com.example.api.auth.application.LoginUserUseCase
import com.example.api.auth.application.RefreshAccessTokenUseCase
import com.example.api.auth.application.RegisterUserUseCase
import com.example.api.auth.infrastructure.RefreshTokenStore
import com.example.api.auth.infrastructure.TokenDenylist
import com.example.api.auth.security.JwtTokenProvider
import com.example.api.auth.security.RefreshTokenCookie
import com.example.api.auth.security.UserPrincipal
import com.example.api.common.dto.ApiResponse
import com.example.api.common.exceptions.UnauthorizedException
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "Registro, login, refresh e logout (JWT)")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    private val refreshTokenStore: RefreshTokenStore,
    private val tokenDenylist: TokenDenylist,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenCookie: RefreshTokenCookie
) {
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = registerUserUseCase.execute(request.email, request.password, request.acceptedTerms)
        return authResponse(issueTokens(user.id, user.email, user.role), HttpStatus.CREATED)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val user = loginUserUseCase.execute(request.email, request.password)
        return authResponse(issueTokens(user.id, user.email, user.role), HttpStatus.OK)
    }

    @PostMapping("/refresh")
    fun refresh(httpRequest: HttpServletRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val rawRefreshToken = refreshTokenCookie.read(httpRequest)
            ?: throw UnauthorizedException("Refresh token ausente")
        return authResponse(refreshAccessTokenUseCase.execute(rawRefreshToken), HttpStatus.OK)
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: UserPrincipal,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        // Revoga todas as sessões (refresh tokens) do usuário.
        refreshTokenStore.revokeAllForUser(principal.id)
        // Invalida imediatamente o access token atual (denylist por jti até expirar).
        resolveAccessToken(httpRequest)?.let { token ->
            jwtTokenProvider.extractJti(token)?.let { jti ->
                tokenDenylist.denylist(jti, jwtTokenProvider.remainingMillis(token))
            }
        }
        // Limpa o cookie de refresh no navegador.
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
            .build()
    }

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return ResponseEntity.ok(
            ApiResponse(
                data = mapOf(
                    "userId" to principal.id.toString(),
                    "email" to principal.email
                )
            )
        )
    }

    private fun issueTokens(userId: java.util.UUID, email: String, role: String): IssuedTokens {
        val accessToken = jwtTokenProvider.generateToken(userId, email, role)
        val refreshToken = refreshTokenStore.issue(userId)
        return IssuedTokens(
            userId = userId.toString(),
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getExpirationMs()
        )
    }

    /** Monta a resposta: access token no corpo + refresh token em cookie httpOnly. */
    private fun authResponse(
        tokens: IssuedTokens,
        status: HttpStatus
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val body = AuthResponse(
            userId = tokens.userId,
            email = tokens.email,
            accessToken = tokens.accessToken,
            expiresIn = tokens.expiresIn
        )
        return ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(tokens.refreshToken).toString())
            .body(ApiResponse(data = body))
    }

    private fun resolveAccessToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }
}
