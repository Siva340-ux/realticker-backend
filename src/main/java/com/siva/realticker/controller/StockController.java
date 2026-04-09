package com.siva.realticker.controller;

import com.siva.realticker.dto.*;
import com.siva.realticker.model.HistoricalPrice;
import com.siva.realticker.model.Stock;
import com.siva.realticker.service.HuggingFaceService;
import com.siva.realticker.service.MockStockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StockController {

    private final MockStockDataService mockStockDataService;
    private final HuggingFaceService huggingFaceService;

    @GetMapping("/top10")
    public ApiResponse<List<Stock>> getTop10Stocks() {
        return ApiResponse.<List<Stock>>builder()
                .success(true)
                .message("Top 10 stocks fetched successfully")
                .data(mockStockDataService.getTop10Stocks())
                .build();
    }

    @GetMapping("/{ticker}/history")
    public ApiResponse<StockHistoryResponse> getStockHistory(@PathVariable String ticker) {
        Stock stock = mockStockDataService.getStockByTicker(ticker);

        if (stock == null) {
            return ApiResponse.<StockHistoryResponse>builder()
                    .success(false)
                    .message("Stock not found for ticker: " + ticker)
                    .data(null)
                    .build();
        }

        StockHistoryResponse response = StockHistoryResponse.builder()
                .ticker(stock.getTicker())
                .companyName(stock.getCompanyName())
                .history(mockStockDataService.getStockHistory(ticker))
                .build();

        return ApiResponse.<StockHistoryResponse>builder()
                .success(true)
                .message("Stock history fetched successfully")
                .data(response)
                .build();
    }

    @PostMapping("/{ticker}/analyze")
    public ApiResponse<AnalyzeResponse> analyzeStock(@PathVariable String ticker,
                                                     @RequestBody(required = false) AnalyzeRequest request) {
        Stock stock = mockStockDataService.getStockByTicker(ticker);

        if (stock == null) {
            return ApiResponse.<AnalyzeResponse>builder()
                    .success(false)
                    .message("Stock not found for ticker: " + ticker)
                    .data(null)
                    .build();
        }

        List<HistoricalPrice> history = mockStockDataService.getStockHistory(ticker);
        AnalysisResult analysis = huggingFaceService.analyzeStockHistory(ticker, history);

        AnalyzeResponse response = AnalyzeResponse.builder()
                .ticker(stock.getTicker())
                .companyName(stock.getCompanyName())
                .analysis(analysis)
                .build();

        return ApiResponse.<AnalyzeResponse>builder()
                .success(true)
                .message("Analysis completed successfully")
                .data(response)
                .build();
    }
}