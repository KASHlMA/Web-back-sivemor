package com.sivemor.platform.service

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.config.AppMailProperties
import com.sivemor.platform.support.TestEntityFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender

class UserPasswordServicesTest {

    private val mailSender = mockk<JavaMailSender>()

    @Test
    fun `blocks password emails when smtp host is only a local test inbox`() {
        val mailer = SmtpUserCredentialMailer(
            mailSender = mailSender,
            mailProperties = AppMailProperties(enabled = true, allowTestInbox = false),
            smtpHost = "mailpit"
        )

        assertThrows(BadRequestException::class.java) {
            mailer.sendNewPassword(TestEntityFactory.user(id = 8L, username = "tech"), "Temp1234!")
        }
    }

    @Test
    fun `sends password email when smtp host is configured for real delivery`() {
        every { mailSender.send(any<SimpleMailMessage>()) } just runs
        val mailer = SmtpUserCredentialMailer(
            mailSender = mailSender,
            mailProperties = AppMailProperties(enabled = true, allowTestInbox = false),
            smtpHost = "smtp.office365.com"
        )

        mailer.sendNewPassword(TestEntityFactory.user(id = 8L, username = "tech"), "Temp1234!")

        verify(exactly = 1) {
            mailSender.send(withArg<SimpleMailMessage> {
                check(it.from == "no-reply@sivemor.local")
                check(it.to?.contentEquals(arrayOf("tech@example.com")) == true)
                check(it.text?.contains("Temp1234!") == true)
            })
        }
    }
}
