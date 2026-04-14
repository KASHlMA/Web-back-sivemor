package com.sivemor.platform.service

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.config.AppMailProperties
import com.sivemor.platform.domain.User
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import java.security.SecureRandom

interface PasswordGenerator {
    fun generate(length: Int = 14): String
}

@Service
class SecurePasswordGenerator : PasswordGenerator {
    private val secureRandom = SecureRandom()
    private val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val lower = "abcdefghijkmnopqrstuvwxyz"
    private val digits = "23456789"
    private val symbols = "!@#$%&*?"
    private val alphabet = upper + lower + digits + symbols

    override fun generate(length: Int): String {
        require(length >= 12) { "Password length must be at least 12" }

        val required = mutableListOf(
            upper.randomChar(),
            lower.randomChar(),
            digits.randomChar(),
            symbols.randomChar()
        )

        while (required.size < length) {
            required += alphabet.randomChar()
        }

        return required.shuffled().joinToString("")
    }

    private fun String.randomChar(): Char = this[secureRandom.nextInt(length)]
}

interface UserCredentialMailer {
    fun sendNewPassword(user: User, generatedPassword: String)
}

@Service
class SmtpUserCredentialMailer(
    private val mailSender: JavaMailSender,
    private val mailProperties: AppMailProperties
) : UserCredentialMailer {
    override fun sendNewPassword(user: User, generatedPassword: String) {
        if (!mailProperties.enabled) {
            throw BadRequestException("El envio de correos para contrasenas no esta habilitado")
        }

        val message = SimpleMailMessage().apply {
            from = mailProperties.from
            setTo(user.email)
            subject = "Acceso a plataforma SIVEMOR"
            text = buildString {
                appendLine("Hola ${user.fullName},")
                appendLine()
                appendLine("Se genero una nueva contrasena para tu acceso a la plataforma SIVEMOR.")
                appendLine("Usuario: ${user.username}")
                appendLine("Contrasena temporal: $generatedPassword")
                appendLine()
                appendLine("Te recomendamos iniciar sesion cuanto antes.")
            }
        }

        mailSender.send(message)
    }
}
