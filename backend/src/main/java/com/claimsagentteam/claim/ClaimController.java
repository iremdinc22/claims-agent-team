package com.claimsagentteam.claim;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateClaimResponse createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return claimService.createClaim(request);
    }

    @GetMapping
    public ClaimListResponse listClaims(@RequestParam MultiValueMap<String, String> parameters) {
        for (String key : parameters.keySet()) {
            if (!key.equals("page") && !key.equals("status")) throw new InvalidClaimQueryException(key);
        }
        int page = 1;
        if (parameters.containsKey("page")) {
            var values = parameters.get("page");
            if (values.size() != 1 || !values.getFirst().matches("[0-9]+")) {
                throw new InvalidClaimQueryException("page");
            }
            try { page = Integer.parseInt(values.getFirst()); }
            catch (NumberFormatException exception) { throw new InvalidClaimQueryException("page"); }
            if (page < 1) throw new InvalidClaimQueryException("page");
        }
        if (!parameters.containsKey("status")) return claimService.listClaims(page);
        var values = parameters.get("status");
        if (values.size() != 1) throw new InvalidClaimQueryException("status");
        ClaimStatus status;
        try { status = ClaimStatus.valueOf(values.getFirst()); }
        catch (IllegalArgumentException exception) { throw new InvalidClaimQueryException("status"); }
        return claimService.listClaims(page, status);
    }

    @GetMapping("/{claimNumber}")
    public ClaimDetailResponse getClaim(@PathVariable String claimNumber,
            @RequestParam MultiValueMap<String, String> parameters) {
        if (!parameters.isEmpty()) throw new InvalidClaimQueryException(parameters.keySet().iterator().next());
        UUID number;
        try {
            number = UUID.fromString(claimNumber);
            if (!number.toString().equalsIgnoreCase(claimNumber)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) { throw new InvalidClaimQueryException("claimNumber"); }
        return claimService.getClaim(number);
    }
}
