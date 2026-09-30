package com.claimsagentteam.claim;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.claimsagentteam.policy.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final Clock clock;

    public ClaimService(ClaimRepository claimRepository, PolicyRepository policyRepository, Clock clock) {
        this.claimRepository = claimRepository;
        this.policyRepository = policyRepository;
        this.clock = clock;
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
                ClaimStatus.REPORTED);

        claimRepository.save(claim);
        return new CreateClaimResponse(claimNumber.toString(), ClaimStatus.REPORTED);
    }
}
