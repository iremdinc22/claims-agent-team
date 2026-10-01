package com.claimsagentteam.claim;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.claimsagentteam.policy.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final Clock clock;
    private final CurrentUserProvider currentUserProvider;

    public ClaimService(ClaimRepository claimRepository, PolicyRepository policyRepository, Clock clock, CurrentUserProvider currentUserProvider) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.clock = clock;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public CreateClaimResponse createClaim(CreateClaimRequest request) {
        if (request.incidentDate().isAfter(LocalDate.now(clock))) {
            throw new FutureIncidentDateException();
        }

        if (!policyRepository.existsById(request.policyNumber())) {
            throw new PolicyNotFoundException();
        }

        UUID claimNumber = UUID.randomUUID();
        ClaimEntity claim = new ClaimEntity(
                claimNumber,
                request.policyNumber(),
                request.incidentType(),
                request.incidentDate(),
                request.description(),
                ClaimStatus.REPORTED,
                currentUserId(),
                clock.instant());

        claimRepository.save(claim);
        return new CreateClaimResponse(claimNumber.toString(), ClaimStatus.REPORTED);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ClaimListResponse listClaims(int page) {
        return listClaims(page, null);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ClaimListResponse listClaims(int page, ClaimStatus status) {
        if (page < 1) throw new InvalidClaimQueryException("page");
        String owner = currentUserId();
        long offset = ((long) page - 1) * 10;
        long total = status == null ? claimRepository.countByCreatedBy(owner)
                : claimRepository.countByCreatedByAndStatus(owner, status);
        var items = offset >= total ? java.util.List.<ClaimListItem>of()
                : (status == null ? claimRepository.findOwnedPage(owner, offset)
                        : claimRepository.findOwnedStatusPage(owner, status.name(), offset)).stream()
                .map(claim -> new ClaimListItem(claim.getClaimNumber().toString(), claim.getPolicyNumber(),
                        claim.getIncidentType(), claim.getIncidentDate(), claim.getStatus())).toList();
        return new ClaimListResponse(items, page, 10, total, total / 10 + (total % 10 == 0 ? 0 : 1));
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse getClaim(UUID claimNumber) {
        ClaimEntity claim = claimRepository.findByClaimNumberAndCreatedBy(claimNumber, currentUserId())
                .orElseThrow(ClaimNotFoundException::new);
        return new ClaimDetailResponse(claim.getClaimNumber().toString(), claim.getPolicyNumber(),
                claim.getIncidentType(), claim.getIncidentDate(), claim.getDescription(), claim.getStatus());
    }

    private String currentUserId() {
        String owner = currentUserProvider.currentUserId();
        if (owner == null || owner.isBlank()) throw new IllegalStateException("Current user unavailable");
        return owner;
    }
}
