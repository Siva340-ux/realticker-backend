package com.siva.realticker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {
    private String ticker;
    private String companyName;
    private AnalysisResult analysis;
}