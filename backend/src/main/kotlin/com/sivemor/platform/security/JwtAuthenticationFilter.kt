package com.sivemor.platform.security

import com.sivemor.platform.domain.UserRepository
import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (header?.startsWith("Bearer ") == true &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val token = header.removePrefix("Bearer ").trim()
            val claims = runCatching { jwtService.parseAccessToken(token) }.getOrNull()

            if (claims != null) {
                authenticateRequest(request, claims)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticateRequest(request: HttpServletRequest, claims: Claims) {
        val userId = (claims["uid"] as? Number)?.toLong() ?: return
        val user = userRepository.findById(userId).orElse(null) ?: return

        if (!user.active || user.archived) {
            return
        }

        val principal = AppUserPrincipal.from(user)
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities
        )
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication
    }
}
