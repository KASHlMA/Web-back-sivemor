package com.sivemor.platform.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class DatabaseMigrationConfig {
    @Bean(initMethod = "migrate")
    fun flyway(dataSource: DataSource): Flyway = Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("classpath:db/migration")
        .load()

    @Bean
    fun entityManagerFactoryDependsOnFlyway(): BeanFactoryPostProcessor = BeanFactoryPostProcessor { beanFactory ->
        val beanName = "entityManagerFactory"
        if (beanFactory.containsBeanDefinition(beanName)) {
            val beanDefinition = beanFactory.getBeanDefinition(beanName)
            val dependsOn = beanDefinition.dependsOn?.toMutableSet() ?: mutableSetOf()
            dependsOn += "flyway"
            beanDefinition.setDependsOn(*dependsOn.toTypedArray())
        }
    }
}
