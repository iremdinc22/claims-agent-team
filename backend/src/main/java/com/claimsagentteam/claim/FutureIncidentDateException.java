package com.claimsagentteam.claim;

public class FutureIncidentDateException extends RuntimeException {

    public FutureIncidentDateException() {
        super("Incident date cannot be in the future");
    }
}
