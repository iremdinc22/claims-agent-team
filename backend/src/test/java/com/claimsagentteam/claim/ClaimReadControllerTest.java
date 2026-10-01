package com.claimsagentteam.claim;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.claimsagentteam.common.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClaimController.class)
@Import(ApiExceptionHandler.class)
class ClaimReadControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ClaimService service;
    private static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test void exactListContractAndHeaderCannotSelectOwner() throws Exception {
        when(service.listClaims(1)).thenReturn(new ClaimListResponse(List.of(new ClaimListItem(
                ID.toString(), "P", IncidentType.COLLISION, LocalDate.of(2025, 1, 15), ClaimStatus.REPORTED)), 1, 10, 1, 1));
        mvc.perform(get("/api/claims").header("X-User-ID", "other"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                {"items":[{"claimNumber":"550e8400-e29b-41d4-a716-446655440000",
                "policyNumber":"P","incidentType":"COLLISION","incidentDate":"2025-01-15",
                "status":"REPORTED"}],"page":1,"pageSize":10,"totalItems":1,"totalPages":1}
                """, true));
        verify(service).listClaims(1);
    }

    @Test void explicitPage() throws Exception {
        when(service.listClaims(2)).thenReturn(new ClaimListResponse(List.of(), 2, 10, 0, 0));
        mvc.perform(get("/api/claims?page=2")).andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
    }

    @ParameterizedTest @ValueSource(strings = {"page=0", "page=-1", "page=", "page=x", "page=1.5",
            "page=2147483648", "page=1&page=2", "userId=other", "size=10", "sort=status", "status=REPORTED"})
    void rejectsInvalidQuery(String query) throws Exception {
        mvc.perform(get("/api/claims?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1"})
    void invalidUuid(String id) throws Exception {
        mvc.perform(get("/api/claims/" + id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.claimNumber").value("Invalid value"));
    }

    @Test void exactDetailContract() throws Exception {
        when(service.getClaim(ID)).thenReturn(new ClaimDetailResponse(ID.toString(), "P", IncidentType.THEFT,
                LocalDate.of(2025, 1, 15), "Description", ClaimStatus.APPROVED));
        mvc.perform(get("/api/claims/" + ID)).andExpect(status().isOk()).andExpect(content().json("""
                {"claimNumber":"550e8400-e29b-41d4-a716-446655440000","policyNumber":"P",
                "incidentType":"THEFT","incidentDate":"2025-01-15","description":"Description","status":"APPROVED"}
                """, true));
    }

    @Test void missingOrNonOwnedUsesGeneric404() throws Exception {
        when(service.getClaim(ID)).thenThrow(new ClaimNotFoundException());
        mvc.perform(get("/api/claims/" + ID)).andExpect(status().isNotFound()).andExpect(content().json(
                "{\"code\":\"CLAIM_NOT_FOUND\",\"message\":\"Claim not found\",\"fieldErrors\":{}}", true));
    }

    @Test void queryFailureIsSanitized() throws Exception {
        when(service.listClaims(1)).thenThrow(new IllegalStateException("SQL secret"));
        mvc.perform(get("/api/claims")).andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"INTERNAL_ERROR\",\"message\":\"The request could not be completed\",\"fieldErrors\":{}}", true));
    }
}
