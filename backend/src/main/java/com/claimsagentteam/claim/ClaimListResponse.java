package com.claimsagentteam.claim;

public record ClaimListResponse(java.util.List<ClaimListItem> items, int page, int pageSize, long totalItems, long totalPages) {
}
