package com.claimsagentteam.claim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.claimsagentteam.policy.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-01-15T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PolicyRepository policyRepository;

    private ClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, policyRepository, CLOCK, new DemoCurrentUserProvider());
    }

    @Test
    void createsReportedClaimForExistingPolicy() {
        when(policyRepository.existsById("MOTOR-POLICY-001")).thenReturn(true);
        CreateClaimRequest request = requestWithDate(LocalDate.of(2025, 1, 15));

        CreateClaimResponse response = claimService.createClaim(request);

        UUID generatedClaimNumber = UUID.fromString(response.claimNumber());
        assertThat(response.status()).isEqualTo(ClaimStatus.REPORTED);

        ArgumentCaptor<ClaimEntity> claimCaptor = ArgumentCaptor.forClass(ClaimEntity.class);
        verify(claimRepository).save(claimCaptor.capture());
        ClaimEntity savedClaim = claimCaptor.getValue();
        assertThat(savedClaim.getClaimNumber()).isEqualTo(generatedClaimNumber);
        assertThat(savedClaim.getPolicyNumber()).isEqualTo("MOTOR-POLICY-001");
        assertThat(savedClaim.getIncidentType()).isEqualTo(IncidentType.COLLISION);
        assertThat(savedClaim.getIncidentDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(savedClaim.getDescription()).isEqualTo("Rear-end collision at a junction.");
        assertThat(savedClaim.getStatus()).isEqualTo(ClaimStatus.REPORTED);
        assertThat(savedClaim.getCreatedBy()).isEqualTo("prototype-demo-user");
        assertThat(savedClaim.getCreatedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void rejectsFutureIncidentDateBeforePolicyLookupOrPersistence() {
        CreateClaimRequest request = requestWithDate(LocalDate.of(2025, 1, 16));

        assertThatThrownBy(() -> claimService.createClaim(request))
                .isInstanceOf(FutureIncidentDateException.class)
                .hasMessage("Incident date cannot be in the future");

        verify(policyRepository, never()).existsById(any());
        verify(claimRepository, never()).save(any());
    }

    @Test
    void rejectsMissingPolicyWithoutPersistingClaim() {
        when(policyRepository.existsById("MOTOR-POLICY-001")).thenReturn(false);

        assertThatThrownBy(() -> claimService.createClaim(requestWithDate(LocalDate.of(2025, 1, 14))))
                .isInstanceOf(PolicyNotFoundException.class)
                .hasMessage("Policy does not exist");

        verify(claimRepository, never()).save(any());
    }

    @Test
    void missingAndNonOwnedDetailUseOwnerPredicate() {
        UUID id = UUID.randomUUID();
        when(claimRepository.findByClaimNumberAndCreatedBy(id, "prototype-demo-user"))
                .thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> claimService.getClaim(id)).isInstanceOf(ClaimNotFoundException.class);
        verify(claimRepository, never()).findById(any());
    }

    @Test
    void maximumPageDoesNotOverflowOrQueryUnneededContent() {
        when(claimRepository.countByCreatedBy("prototype-demo-user")).thenReturn(21L);
        var response = claimService.listClaims(Integer.MAX_VALUE);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalPages()).isEqualTo(3);
        verify(claimRepository, never()).findOwnedPage(any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void unavailableIdentityFailsClosed() {
        for (String identity : new String[]{null, "", "  "}) {
            ClaimService service = new ClaimService(claimRepository, policyRepository, CLOCK, () -> identity);
            assertThatThrownBy(() -> service.listClaims(1)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service.getClaim(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
        }
        ClaimService service = new ClaimService(claimRepository, policyRepository, CLOCK,
                () -> { throw new IllegalStateException("identity unavailable"); });
        assertThatThrownBy(() -> service.listClaims(1)).isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(claimRepository);
    }

    @Test
    void persistenceFailureDoesNotReturnSuccess() {
        when(policyRepository.existsById("MOTOR-POLICY-001")).thenReturn(true);
        when(claimRepository.save(any())).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> claimService.createClaim(requestWithDate(LocalDate.of(2025, 1, 15))))
                .isInstanceOf(IllegalStateException.class);
    }

    private CreateClaimRequest requestWithDate(LocalDate incidentDate) {
        return new CreateClaimRequest(
                "MOTOR-POLICY-001",
                IncidentType.COLLISION,
                incidentDate,
                "Rear-end collision at a junction.");
    }
}
