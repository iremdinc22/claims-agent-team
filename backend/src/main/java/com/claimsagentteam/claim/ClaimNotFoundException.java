package com.claimsagentteam.claim;

public class ClaimNotFoundException extends RuntimeException {
    public ClaimNotFoundException() {
        super("Claim not found");
    }
}
