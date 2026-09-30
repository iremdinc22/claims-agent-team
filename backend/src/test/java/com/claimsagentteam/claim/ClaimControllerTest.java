package com.claimsagentteam.claim;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.claimsagentteam.common.ApiExceptionHandler;
import com.claimsagentteam.common.JsonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClaimController.class)
@Import({ApiExceptionHandler.class, JsonConfiguration.class})
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClaimService claimService;

    @Test
    void returnsCreatedClaim() throws Exception {
        when(claimService.createClaim(any())).thenReturn(new CreateClaimResponse(
                "550e8400-e29b-41d4-a716-446655440000",
                ClaimStatus.REPORTED));

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.claimNumber").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void ignoresUnknownJsonProperties() throws Exception {
        when(claimService.createClaim(any())).thenReturn(new CreateClaimResponse(
                "550e8400-e29b-41d4-a716-446655440000",
                ClaimStatus.REPORTED));

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace(
                                "\"description\": \"Rear-end collision at a junction.\"",
                                "\"description\": \"Rear-end collision at a junction.\", \"ignored\": true")))
                .andExpect(status().isCreated());
    }

    @Test
    void mapsMissingRequiredFieldsToValidationError() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber").value("Policy number is required"))
                .andExpect(jsonPath("$.fieldErrors.incidentType").value("Incident type is required"))
                .andExpect(jsonPath("$.fieldErrors.incidentDate").value("Incident date is required"))
                .andExpect(jsonPath("$.fieldErrors.description").value("Description is required"));
    }

    @Test
    void mapsBlankRequiredTextFieldsToValidationError() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()
                                .replace("MOTOR-POLICY-001", "   ")
                                .replace("Rear-end collision at a junction.", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber").value("Policy number is required"))
                .andExpect(jsonPath("$.fieldErrors.description").value("Description is required"));
    }

    @Test
    void mapsUnsupportedIncidentTypeToValidationError() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("COLLISION", "FIRE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.incidentType")
                        .value("Incident type must be one of COLLISION, THEFT, GLASS_DAMAGE, or OTHER"));
    }

    @Test
    void mapsInvalidIncidentDateFormatToValidationError() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("2025-01-15", "15/01/2025")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.incidentDate")
                        .value("Incident date must use YYYY-MM-DD format"));
    }

    @Test
    void rejectsIncompatibleJsonValue() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"MOTOR-POLICY-001\"", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber").value("Invalid value"));
    }

    @Test
    void mapsFutureIncidentDateToValidationError() throws Exception {
        when(claimService.createClaim(any())).thenThrow(new FutureIncidentDateException());

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.incidentDate")
                        .value("Incident date cannot be in the future"));
    }

    @Test
    void mapsMissingPolicyToUnprocessableEntity() throws Exception {
        when(claimService.createClaim(any())).thenThrow(new PolicyNotFoundException());

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors.policyNumber").value("Policy does not exist"));
    }

    @Test
    void mapsUnexpectedFailureWithoutExposingDetails() throws Exception {
        when(claimService.createClaim(any())).thenThrow(new IllegalStateException("persistence unavailable"));

        mockMvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("The request could not be completed"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void doesNotMapUnrelatedMissingResourcesAsInternalClaimErrors() throws Exception {
        mockMvc.perform(get("/missing-resource"))
                .andExpect(status().isNotFound());
    }

    private String validRequest() {
        return """
                {
                  "policyNumber": "MOTOR-POLICY-001",
                  "incidentType": "COLLISION",
                  "incidentDate": "2025-01-15",
                  "description": "Rear-end collision at a junction."
                }
                """;
    }
}
