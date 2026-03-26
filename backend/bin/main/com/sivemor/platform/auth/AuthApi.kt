package com.sivemor.platform.auth

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.RefreshToken
import com.sivemor.platform.domain.RefreshTokenRepository
import com.sivemor.platform.domain.User
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.security.JwtService
import com.sivemor.platform.service.AuditService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String
)

data class LogoutRequest(
    @field:NotBlank val refreshToken: String
)

data class SessionUserResponse(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: SessionUserResponse
)

data class LogoutResponse(
    val success: Boolean
)

@RestController
@Tag(name = "Auth", description = "Authentication and token lifecycle endpoints")
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @Operation(summary = "Authenticate an administrator or technician")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @Operation(summary = "Rotate a refresh token and obtain a new access token")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request))

    @Operation(summary = "Revoke the current refresh token")
    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<LogoutResponse> =
        ResponseEntity.ok(authService.logout(request))
}

@org.springframework.stereotype.Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val auditService: AuditService,
    private val clock: Clock
) {
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsernameIgnoreCaseAndArchivedFalse(request.username.trim())
            ?.takeIf { it.active }
            ?: throw BadRequestException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadRequestException("Invalid credentials")
        }

        return issueSession(user).also {
            auditService.log(
                actor = user,
                action = "AUTH_LOGIN",
                entityName = "USER",
                entityId = user.id.toString(),
                details = mapOf("role" to user.role.name)
            )
        }
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthResponse {
        val hashedToken = jwtService.hashToken(request.refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse(hashedToken)
            ?: throw BadRequestException("Refresh token is invalid")

        if (storedToken.expiresAt.isBefore(Instant.now(clock))) {
            storedToken.revokedAt = Instant.now(clock)
            throw BadRequestException("Refresh token has expired")
        }

        val user = storedToken.user
        if (!user.active || user.archived) {
            throw NotFoundException("User is not available")
        }

        storedToken.revokedAt = Instant.now(clock)
        return issueSession(user)
    }

    @Transactional
    fun logout(request: LogoutRequest): LogoutResponse {
        val hashedToken = jwtService.hashToken(request.refreshToken)
        val storedToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse(hashedToken)
            ?: return LogoutResponse(success = true)

        storedToken.revokedAt = Instant.now(clock)
        auditService.log(
            actor = storedToken.user,
            action = "AUTH_LOGOUT",
            entityName = "USER",
            entityId = storedToken.user.id.toString()
        )
        return LogoutResponse(success = true)
    }

    private fun issueSession(user: User): AuthResponse {
        refreshTokenRepository.findAllActiveByUserId(user.id ?: 0L).forEach {
            if (it.expiresAt.isBefore(Instant.now(clock))) {
                it.revokedAt = Instant.now(clock)
            }
        }

        val rawRefreshToken = jwtService.generateRefreshToken()
        val expiresAt = Instant.now(clock).plusSeconds(60 * 60 * 24 * 14)

        refreshTokenRepository.save(
            RefreshToken().apply {
                this.user = user
                tokenHash = jwtService.hashToken(rawRefreshToken)
                this.expiresAt = expiresAt
            }
        )

        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user),
            refreshToken = rawRefreshToken,
            expiresInSeconds = 60 * 60,
            user = SessionUserResponse(
                id = user.id ?: 0L,
                username = user.username,
                fullName = user.fullName,
                role = user.role.name
            )
        )
    }
}
