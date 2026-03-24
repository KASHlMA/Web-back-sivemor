package com.sivemor.platform.auth

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.RefreshTokenRepository
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.security.JwtService
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.support.TestEntityFactory.refreshToken
import com.sivemor.platform.support.TestEntityFactory.user
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.Runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AuthServiceTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var jwtService: JwtService

    @MockK
    private lateinit var auditService: AuditService

    private lateinit var authService: AuthService
    private val clock: Clock = Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        authService = AuthService(
            userRepository,
            refreshTokenRepository,
            passwordEncoder,
            jwtService,
            auditService,
            clock
        )
        every { auditService.log(any(), any(), any(), any(), any()) } just Runs
    }

    @Test
    fun `login issues a new session and writes an audit log`() {
        val actor = user(id = 10L, username = "admin")
        val savedRefreshToken = slot<com.sivemor.platform.domain.RefreshToken>()

        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("admin") } returns actor
        every { passwordEncoder.matches("Admin123!", actor.passwordHash) } returns true
        every { refreshTokenRepository.findAllActiveByUserId(actor.id!!) } returns emptyList()
        every { jwtService.generateRefreshToken() } returns "raw-refresh-token"
        every { jwtService.hashToken("raw-refresh-token") } returns "hashed-refresh-token"
        every { jwtService.generateAccessToken(actor) } returns "access-token"
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

        val response = authService.login(LoginRequest("admin", "Admin123!"))

        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("raw-refresh-token")
        assertThat(response.user.username).isEqualTo("admin")
        assertThat(savedRefreshToken.captured.tokenHash).isEqualTo("hashed-refresh-token")
        assertThat(savedRefreshToken.captured.expiresAt).isEqualTo(Instant.now(clock).plusSeconds(60 * 60 * 24 * 14))
        verify {
            auditService.log(
                actor = actor,
                action = "AUTH_LOGIN",
                entityName = "USER",
                entityId = actor.id.toString(),
                details = match<Map<String, String>> { it["role"] == actor.role.name }
            )
        }
    }

    @Test
    fun `login rejects inactive users as invalid credentials`() {
        val inactiveUser = user(id = 11L, active = false)
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("user11") } returns inactiveUser

        assertThatThrownBy {
            authService.login(LoginRequest("user11", "password"))
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Invalid credentials")
    }

    @Test
    fun `refresh revokes an expired token and rejects the request`() {
        val token = refreshToken(
            user = user(id = 22L),
            tokenHash = "stored-hash",
            expiresAt = Instant.now(clock).minusSeconds(60)
        )

        every { jwtService.hashToken("expired-token") } returns "stored-hash"
        every {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse("stored-hash")
        } returns token

        assertThatThrownBy {
            authService.refresh(RefreshRequest("expired-token"))
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Refresh token has expired")

        assertThat(token.revokedAt).isEqualTo(Instant.now(clock))
    }

    @Test
    fun `refresh rotates a valid refresh token and issues a new session`() {
        val actor = user(id = 12L, username = "tech")
        val storedToken = refreshToken(
            user = actor,
            tokenHash = "stored-hash",
            expiresAt = Instant.now(clock).plusSeconds(3600)
        )
        val savedRefreshToken = slot<com.sivemor.platform.domain.RefreshToken>()

        every { jwtService.hashToken("refresh-token") } returns "stored-hash"
        every {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse("stored-hash")
        } returns storedToken
        every { refreshTokenRepository.findAllActiveByUserId(actor.id!!) } returns listOf(storedToken)
        every { jwtService.generateRefreshToken() } returns "new-refresh-token"
        every { jwtService.hashToken("new-refresh-token") } returns "new-refresh-hash"
        every { jwtService.generateAccessToken(actor) } returns "new-access-token"
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

        val response = authService.refresh(RefreshRequest("refresh-token"))

        assertThat(storedToken.revokedAt).isEqualTo(Instant.now(clock))
        assertThat(response.accessToken).isEqualTo("new-access-token")
        assertThat(response.refreshToken).isEqualTo("new-refresh-token")
        assertThat(savedRefreshToken.captured.tokenHash).isEqualTo("new-refresh-hash")
    }

    @Test
    fun `refresh rejects tokens for unavailable users`() {
        val archivedUser = user(id = 13L, archived = true)
        val storedToken = refreshToken(
            user = archivedUser,
            tokenHash = "stored-hash",
            expiresAt = Instant.now(clock).plusSeconds(3600)
        )

        every { jwtService.hashToken("refresh-token") } returns "stored-hash"
        every {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse("stored-hash")
        } returns storedToken

        assertThatThrownBy {
            authService.refresh(RefreshRequest("refresh-token"))
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("User is not available")
    }

    @Test
    fun `logout is idempotent when the token does not exist`() {
        every { jwtService.hashToken("missing-token") } returns "missing-hash"
        every {
            refreshTokenRepository.findByTokenHashAndRevokedAtIsNullAndArchivedFalse("missing-hash")
        } returns null

        val response = authService.logout(LogoutRequest("missing-token"))

        assertThat(response.success).isTrue()
        verify(exactly = 0) { auditService.log(any(), any(), any(), any(), any()) }
    }
}
