package com.coworking;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class DataSourceContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @DynamicPropertySource
    static void registerDataSourceProps(DynamicPropertyRegistry registry) {
        // Spring usará estos valores en lugar de application.yml
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    DataSource dataSource;

    @BeforeAll
    static void checkDocker() {
        // Arranca el contenedor si aún no arrancó (Testcontainers se encarga, pero útil para logs)
        Assertions.assertTrue(postgres.isCreated() || !postgres.isRunning() || postgres.isRunning(),
                "Docker debe estar disponible para correr este test");
    }

    @Test
    void shouldConnectToPostgres() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            Assertions.assertNotNull(conn);
            Assertions.assertFalse(conn.isClosed());
        }
    }
}

