package com.sivemor.platform

import com.sivemor.platform.config.CorsProperties
import com.sivemor.platform.config.JwtProperties
import com.sivemor.platform.config.AppMailProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
@EnableConfigurationProperties(
    JwtProperties::class,
    CorsProperties::class,
    AppMailProperties::class
)
class PlatformApplication {
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<PlatformApplication>(*args)
}
