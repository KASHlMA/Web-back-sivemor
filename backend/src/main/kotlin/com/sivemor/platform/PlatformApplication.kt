package com.sivemor.platform

import com.sivemor.platform.config.CorsProperties
import com.sivemor.platform.config.JwtProperties
import com.sivemor.platform.config.AppMailProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Clock

@SpringBootApplication
@EntityScan("com.sivemor.platform.domain")
@EnableJpaRepositories("com.sivemor.platform.domain")
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
