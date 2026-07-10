package org.bublapi.dent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class IntegrationTestBase {

   @Container
   static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName(
                                                                                                   "dent_test")
                                                                                           .withUsername("test")
                                                                                           .withPassword("test")
                                                                                           .withReuse(true);

   static {
      postgres.start();
   }

   @DynamicPropertySource
   static void configure(DynamicPropertyRegistry registry) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
      registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
      registry.add("spring.datasource.hikari.connection-timeout", () -> 30000);
      registry.add("spring.datasource.hikari.initialization-fail-timeout", () -> 60000);

      registry.add("spring.liquibase.enabled", () -> true);
   }

   @Autowired
   protected MockMvc mockMvc;

   @Autowired
   protected ObjectMapper objectMapper;
}