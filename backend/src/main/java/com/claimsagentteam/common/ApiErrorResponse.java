package com.claimsagentteam.common;

import java.util.Map;

public record ApiErrorResponse(String code, String message, Map<String, String> fieldErrors) {
}
