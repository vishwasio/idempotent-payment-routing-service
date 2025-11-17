package com.payment.route.config;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class TestcontainersConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER;

    static {
        POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:15.4")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        POSTGRE_SQL_CONTAINER.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRE_SQL_CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRE_SQL_CONTAINER.getUsername(),
                "spring.datasource.password=" + POSTGRE_SQL_CONTAINER.getPassword(),
                "spring.jpa.hibernate.ddl-auto=validate", // rely on Flyway; change if needed
                "spring.flyway.enabled=true"
        ).applyTo(context.getEnvironment());
    }
}
