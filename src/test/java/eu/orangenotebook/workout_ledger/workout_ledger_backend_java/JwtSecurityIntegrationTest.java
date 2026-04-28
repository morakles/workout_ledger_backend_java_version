package eu.orangenotebook.workout_ledger.workout_ledger_backend_java;

import eu.orangenotebook.workout_ledger.workout_ledger_backend_java.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    Environment environment;

    @Test
    @DisplayName("public endpoint without token returns 200")
    void publicEndpointNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("protected endpoint without token returns 401")
    void protectedEndpointNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/protectedstatus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected endpoint with valid token returns 200")
    void protectedEndpointValidToken() throws Exception {
        String token = jwtService.generateToken("user@email.com", List.of("ROLE_USER"));

        mockMvc.perform(get("/api/v1/protectedstatus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Protected status OK"));
    }

    @Test
    @DisplayName("protected endpoint with malformed token returns 401 JSON")
    void protectedEndpointMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/protectedstatus")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("protected endpoint with tampered token returns 401")
    void protectedEndpointWrongSignature() throws Exception {
        String valid = jwtService.generateToken("user@email.com", List.of("ROLE_USER"));
        String tampered = valid + "x"; // break signature

        mockMvc.perform(get("/api/v1/protectedstatus")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("protected endpoint with expired token returns 401")
    void protectedEndpointExpiredToken() throws Exception {
        String secret = environment.getProperty("jwt.secret");
        byte[] key = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String expired = Jwts.builder()
                .setSubject("user@email.com")
                .claim("roles", List.of("ROLE_USER"))
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 120000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 60000))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/protectedstatus")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }
}
