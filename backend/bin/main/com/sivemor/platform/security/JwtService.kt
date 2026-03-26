package com.sivemor.platform.security

import com.sivemor.platform.config.JwtProperties
import com.sivemor.platform.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
    private val clock: Clock
) {
    private val random = SecureRandom()

    fun generateAccessToken(user: User): String {
        val now = Instant.now(clock)
        val expiration = now.plusSeconds(jwtProperties.accessTokenMinutes * 60)

        return Jwts.builder()
            .subject(user.username)
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("uid", user.id)
            .claim("role", user.role.name)
            .claim("typ", "access")
            .signWith(secretKey())
            .compact()
    }

    fun generateRefreshToken(): String {
        val bytes = ByteArray(48)
        random.nextBytes(bytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun parseAccessToken(token: String): Claims = Jwts.parser()
        .verifyWith(secretKey())
        .build()
        .parseSignedClaims(token)
        .payload
        .also {
            if (it["typ"] != "access") {
                throw IllegalArgumentException("Invalid token type")
            }
        }

    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun secretKey() = Keys.hmacShaKeyFor(
        runCatching { Decoders.BASE64.decode(jwtProperties.secret) }
            .getOrElse { jwtProperties.secret.toByteArray() }
    )
}
