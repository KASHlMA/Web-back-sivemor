package com.sivemor.platform.security

import com.sivemor.platform.config.JwtProperties
import com.sivemor.platform.domain.Role
import com.sivemor.platform.support.TestEntityFactory.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JwtServiceTest {
    private val clock = Clock.fixed(Instant.parse("2036-03-23T12:00:00Z"), ZoneOffset.UTC)
    private val jwtService = JwtService(
        JwtProperties(
            issuer = "sivemor-tests",
            secret = "this-is-a-long-enough-secret-for-test-only-1234567890",
            accessTokenMinutes = 60
        ),
        clock
    )

    @Test
    fun `generateAccessToken includes expected claims`() {
        val token = jwtService.generateAccessToken(user(id = 42L, username = "admin", role = Role.ADMIN))

        val claims = jwtService.parseAccessToken(token)

        assertThat(claims.subject).isEqualTo("admin")
        assertThat(claims["uid"]).isEqualTo(42)
        assertThat(claims["role"]).isEqualTo("ADMIN")
        assertThat(claims["typ"]).isEqualTo("access")
        assertThat(claims.issuer).isEqualTo("sivemor-tests")
    }

    @Test
    fun `generateRefreshToken returns a URL safe token`() {
        val refreshToken = jwtService.generateRefreshToken()

        assertThat(refreshToken).hasSizeGreaterThanOrEqualTo(64)
        assertThat(refreshToken).matches("^[A-Za-z0-9_-]+$")
    }

    @Test
    fun `hashToken returns the sha256 digest`() {
        assertThat(jwtService.hashToken("hello"))
            .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
    }
}
