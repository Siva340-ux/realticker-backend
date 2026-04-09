package com.siva.realticker.dto;

import com.siva.realticker.model.HistoricalPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockHistoryResponse {
    private String ticker;
    private String companyName;
    private List<HistoricalPrice> history;
}