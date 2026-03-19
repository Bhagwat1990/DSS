package com.dss.dto;

import lombok.*;

/**
 * Request for AI-powered suggestion. 
 * Can optionally pass a custom prompt/context.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdvisorRequest {
    private String symbol;          // Stock symbol
    private String additionalContext; // Optional: e.g., "News sentiment is Positive"
}
