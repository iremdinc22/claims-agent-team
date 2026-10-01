package com.claimsagentteam.claim;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Opt-in real PostgreSQL tests, isolated from demonstration data in a random schema. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "TASK002_POSTGRES_TESTS", matches = "true")
class ClaimPostgresIntegrationTest {
    static final String SCHEMA = "task002_test_" + UUID.randomUUID().toString().replace("-", "");
    static final String URL = System.getenv().getOrDefault("TASK002_TEST_DATABASE_URL",
            "jdbc:postgresql://localhost:55432/claims_agent_team");
    static final String USER = System.getenv().getOrDefault("TASK002_TEST_DATABASE_USER", "claims_agent");
    static final String PASSWORD = System.getenv().getOrDefault("TASK002_TEST_DATABASE_PASSWORD", "claims_agent");
    static final Instant NOW = Instant.parse("2026-10-01T10:00:00.123456Z");
    @Autowired ClaimService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @TestConfiguration static class FixedClock {
        @Bean @Primary Clock testClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().execute("CREATE SCHEMA " + SCHEMA);
        }
        registry.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @AfterAll static void cleanup() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            connection.createStatement().execute("DROP SCHEMA " + SCHEMA + " CASCADE");
        }
    }

    @BeforeEach void emptyClaims() { jdbc.update("DELETE FROM claims"); }

    UUID seed(String owner, LocalDate date, Instant time, ClaimStatus status, int number) {
        UUID id = new UUID(0, number);
        jdbc.update("INSERT INTO claims VALUES (?, ?, ?, ?, ?, ?, ?, ?)", id, "MOTOR-POLICY-001",
                "COLLISION", date, "Fixture " + number, status.name(), owner, Timestamp.from(time));
        return id;
    }

    @ParameterizedTest @ValueSource(ints = {0, 1, 10, 11, 20, 21})
    void ownershipPaginationSortingAndAllStatuses(int count) {
        String owner = DemoCurrentUserProvider.DEMO_USER_ID;
        List<UUID> expected = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            // Same incident date crosses page boundaries; creation time breaks ties.
            expected.add(seed(owner, LocalDate.of(2025, 1, 15), NOW.minusSeconds(i),
                    ClaimStatus.values()[i % 4], i));
        }
        UUID other = seed("other-user", LocalDate.of(2026, 1, 1), NOW, ClaimStatus.APPROVED, 100);
        List<String> actual = new ArrayList<>();
        for (int page = 1; page <= Math.max(1, (count + 9) / 10); page++) {
            var result = service.listClaims(page);
            assertThat(result.totalItems()).isEqualTo(count);
            assertThat(result.totalPages()).isEqualTo((count + 9) / 10);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.items()).hasSize(Math.min(10, Math.max(0, count - (page - 1) * 10)));
            result.items().forEach(item -> {
                actual.add(item.claimNumber());
                assertThat(service.getClaim(UUID.fromString(item.claimNumber())).status()).isEqualTo(item.status());
            });
        }
        assertThat(actual).containsExactlyElementsOf(expected.stream().map(UUID::toString).toList());
        assertThat(service.listClaims(Integer.MAX_VALUE).items()).isEmpty();
        assertThatThrownBy(() -> service.getClaim(other)).isInstanceOf(ClaimNotFoundException.class);
        assertThatThrownBy(() -> service.getClaim(UUID.randomUUID())).isInstanceOf(ClaimNotFoundException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM claims", Long.class)).isEqualTo(count + 1L);
        for (int i = 1; i <= count; i++) {
            assertThat(jdbc.queryForObject("SELECT status FROM claims WHERE claim_number = ?", String.class,
                    new UUID(0, i))).isEqualTo(ClaimStatus.values()[i % 4].name());
        }
    }

    @Test void incidentDateHasPriorityAndUuidBreaksExactTies() {
        var older = seed("prototype-demo-user", LocalDate.of(2024, 1, 1), NOW, ClaimStatus.APPROVED, 1);
        var second = seed("prototype-demo-user", LocalDate.of(2025, 1, 1), NOW.minusSeconds(100), ClaimStatus.REJECTED, 3);
        var first = seed("prototype-demo-user", LocalDate.of(2025, 1, 1), NOW.minusSeconds(100), ClaimStatus.PENDING, 2);
        assertThat(service.listClaims(1).items()).extracting(ClaimListItem::claimNumber)
                .containsExactly(first.toString(), second.toString(), older.toString());
    }

    @Test void browserIdentityCannotOverrideCreateListOrDetail() throws Exception {
        String request = """
                {"policyNumber":"MOTOR-POLICY-001","incidentType":"COLLISION","incidentDate":"2025-01-15",
                "description":"Regression","createdBy":"other-user","createdAt":"2000-01-01T00:00:00Z",
                "userId":"other-user","status":"APPROVED"}
                """;
        var response = mvc.perform(post("/api/claims").header("X-User-ID", "other-user")
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REPORTED"))
                .andExpect(jsonPath("$.createdBy").doesNotExist()).andReturn();
        var json = mapper.readTree(response.getResponse().getContentAsString());
        assertThat(json.size()).isEqualTo(2);
        UUID id = UUID.fromString(json.get("claimNumber").asText());
        var metadata = jdbc.queryForMap("SELECT created_by, created_at FROM claims WHERE claim_number = ?", id);
        assertThat(metadata.get("created_by")).isEqualTo("prototype-demo-user");
        assertThat(((Timestamp) metadata.get("created_at")).toInstant()).isEqualTo(NOW);
        // SQL reinitialization must preserve actual new-claim metadata, not assign migration time.
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
        }
        assertThat(jdbc.queryForObject("SELECT created_at FROM claims WHERE claim_number = ?",
                Timestamp.class, id).toInstant()).isEqualTo(NOW);
        assertThat(jdbc.queryForObject("SELECT created_by FROM claims WHERE claim_number = ?",
                String.class, id)).isEqualTo("prototype-demo-user");
        mvc.perform(get("/api/claims").header("X-User-ID", "other-user"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        mvc.perform(get("/api/claims/" + id).header("X-User-ID", "other-user"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Regression"));
        var other = seed("other-user", LocalDate.of(2025, 1, 1), NOW, ClaimStatus.APPROVED, 100);
        String nonOwned = mvc.perform(get("/api/claims/" + other).header("X-User-ID", "other-user"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(get("/api/claims/" + UUID.randomUUID()))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(nonOwned).isEqualTo(missing);
    }

    @Test void legacyUpgradePreservesDataAndIsIdempotent() throws Exception {
        // A separate temporary schema exercises the original TASK-001 structure.
        String legacy = SCHEMA + "_legacy";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            try {
                connection.createStatement().execute("CREATE SCHEMA " + legacy);
                connection.createStatement().execute("SET search_path TO " + legacy);
                String original = new ClassPathResource("schema.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                original = original.substring(original.indexOf("CREATE TABLE"), original.indexOf("ALTER TABLE"))
                        .replace("    created_by text,\n    created_at timestamptz,\n", "")
                        .replace("CONSTRAINT claims_status_allowed CHECK (status IN ('REPORTED', 'PENDING', 'APPROVED', 'REJECTED'))",
                                "CONSTRAINT claims_status_reported CHECK (status = 'REPORTED')");
                connection.createStatement().execute(original);
                connection.createStatement().execute("INSERT INTO policies VALUES ('MOTOR-POLICY-001')");
                connection.createStatement().execute("INSERT INTO claims VALUES ('00000000-0000-0000-0000-000000000001', 'MOTOR-POLICY-001', 'THEFT', '2020-01-01', 'Legacy description', 'REPORTED')");
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
                var rows = connection.createStatement().executeQuery("SELECT * FROM claims");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("created_by")).isEqualTo("prototype-demo-user");
                Instant migrationTime = rows.getTimestamp("created_at").toInstant();
                assertThat(migrationTime).isBetween(Instant.now().minusSeconds(60), Instant.now().plusSeconds(1));
                assertThat(rows.getString("description")).isEqualTo("Legacy description");
                assertThat(rows.getString("status")).isEqualTo("REPORTED");
                connection.createStatement().execute("INSERT INTO claims VALUES ('00000000-0000-0000-0000-000000000002', 'MOTOR-POLICY-001', 'OTHER', '2025-01-01', 'Other', 'APPROVED', 'other-user', '2025-01-01T10:00:00Z')");
                // Simulate partial metadata to verify each COALESCE preserves existing values.
                connection.createStatement().execute("ALTER TABLE claims ALTER COLUMN created_at DROP NOT NULL");
                connection.createStatement().execute("UPDATE claims SET created_at = NULL WHERE created_by = 'other-user'");
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
                var preserved = connection.createStatement().executeQuery("SELECT * FROM claims ORDER BY claim_number");
                preserved.next();
                assertThat(preserved.getTimestamp("created_at").toInstant()).isEqualTo(migrationTime);
                preserved.next();
                assertThat(preserved.getString("created_by")).isEqualTo("other-user");
                Instant otherTime = preserved.getTimestamp("created_at").toInstant();
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
                var restart = connection.createStatement().executeQuery("SELECT * FROM claims ORDER BY claim_number");
                restart.next();
                assertThat(restart.getTimestamp("created_at").toInstant()).isEqualTo(migrationTime);
                restart.next();
                assertThat(restart.getTimestamp("created_at").toInstant()).isEqualTo(otherTime);
                assertThatThrownBy(() -> connection.createStatement().execute("INSERT INTO claims (claim_number,policy_number,incident_type,incident_date,description,status) VALUES ('00000000-0000-0000-0000-000000000003','MOTOR-POLICY-001','OTHER','2025-01-01','Missing metadata','REPORTED')"))
                        .isInstanceOf(SQLException.class);
                for (String status : new String[]{"PENDING", "APPROVED", "REJECTED"}) {
                    connection.createStatement().execute("UPDATE claims SET status = '" + status + "'");
                }
                assertThatThrownBy(() -> connection.createStatement().execute("UPDATE claims SET status = 'UNKNOWN'"))
                        .isInstanceOf(SQLException.class);
            } finally {
                connection.createStatement().execute("DROP SCHEMA IF EXISTS " + legacy + " CASCADE");
            }
        }
    }
}
