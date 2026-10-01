package com.claimsagentteam.claim;

import org.springframework.stereotype.Component;

@Component
public class DemoCurrentUserProvider implements CurrentUserProvider {
    public static final String DEMO_USER_ID = "prototype-demo-user";

    @Override
    public String currentUserId() {
        return DEMO_USER_ID;
    }
}
