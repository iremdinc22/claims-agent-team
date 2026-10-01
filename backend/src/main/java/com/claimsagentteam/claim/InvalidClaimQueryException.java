package com.claimsagentteam.claim;

public class InvalidClaimQueryException extends RuntimeException {
    private final String field;

    public InvalidClaimQueryException(String field) {
        super("Invalid query parameter");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
