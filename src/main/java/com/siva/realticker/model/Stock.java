package com.siva.realticker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    private String ticker;
    private String companyName;
    private double currentPrice;
    private double dailyChangePercent;
    private long volume;
}