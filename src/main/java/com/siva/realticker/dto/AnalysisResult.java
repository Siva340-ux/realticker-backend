package com.siva.realticker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    private String trend;           // "Upward", "Downward", "Sideways"
    private String riskLevel;       // "Low", "Medium", "High"
    private String suggestedAction; // "Long-term investment", "Short-term watch", "Avoid"
    private String reasoning;       // Detailed explanation
}