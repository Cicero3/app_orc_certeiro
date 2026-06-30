package com.example.api.auth.security

import com.example.api.auth.infrastructure.TokenDenylist
import com.example.api.auth.infrastructure.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val tokenDenylist: TokenDenylist
) : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = resolveToken(request)
            val jti = if (token != null && jwtTokenProvider.validateToken(token)) {
                jwtTokenProvider.extractJti(token)
            } else null

            // Token válido e não revogado (logout) -> autentica.
            if (token != null && jti != null && !tokenDenylist.isDenylisted(jti)) {
                val userId = jwtTokenProvider.extractUserId(token)
                val user = userRepository.findById(userId).orElse(null)

                // Conta anonimizada/excluída (LGPD) não autentica, mesmo com token válido.
                if (user != null && user.deletedAt == null) {
                    val principal = UserPrincipal(user.id, user.email, user.role)
                    val authentication = UsernamePasswordAuthenticationToken(
                        principal, null, principal.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to set authentication: ${e.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) {
            bearer.substring(7)
        } else null
    }
}