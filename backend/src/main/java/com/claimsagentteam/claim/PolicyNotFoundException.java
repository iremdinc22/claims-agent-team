package com.claimsagentteam.claim;

public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException() {
        super("Policy does not exist");
    }
}
