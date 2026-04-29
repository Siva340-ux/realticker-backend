package com.siva.realticker.service;

import com.siva.realticker.model.HistoricalPrice;
import com.siva.realticker.model.Stock;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MockStockDataService {

    @Cacheable("topStocks")
    public List<Stock> getTop10Stocks() {
        List<Stock> stocks = new ArrayList<>();

        stocks.add(new Stock("AAPL", "Apple Inc", 185.40, 1.2, 78000000));
        stocks.add(new Stock("MSFT", "Microsoft Corp", 412.30, 0.8, 45000000));
        stocks.add(new Stock("GOOGL", "Alphabet Inc", 172.15, -0.4, 28000000));
        stocks.add(new Stock("AMZN", "Amazon.com Inc", 181.90, 1.6, 39000000));
        stocks.add(new Stock("NVDA", "NVIDIA Corp", 905.20, 2.9, 52000000));
        stocks.add(new Stock("META", "Meta Platforms", 498.75, 1.1, 25000000));
        stocks.add(new Stock("TSLA", "Tesla Inc", 168.55, -1.7, 61000000));
        stocks.add(new Stock("NFLX", "Netflix Inc", 627.10, 0.9, 12000000));
        stocks.add(new Stock("AMD", "Advanced Micro Devices", 173.88, 2.1, 33000000));
        stocks.add(new Stock("INTC", "Intel Corp", 37.42, -0.6, 41000000));

        return stocks;
    }

    @Cacheable(value = "stockHistory", key = "#ticker")
    public List<HistoricalPrice> getStockHistory(String ticker) {
        List<HistoricalPrice> history = new ArrayList<>();
        Random random = new Random(ticker.hashCode());

        double basePrice = switch (ticker.toUpperCase()) {
            case "AAPL" -> 185.40;
            case "MSFT" -> 412.30;
            case "GOOGL" -> 172.15;
            case "AMZN" -> 181.90;
            case "NVDA" -> 905.20;
            case "META" -> 498.75;
            case "TSLA" -> 168.55;
            case "NFLX" -> 627.10;
            case "AMD" -> 173.88;
            case "INTC" -> 37.42;
            default -> 100.00;
        };

        LocalDate startDate = LocalDate.now().minusMonths(6);
        double price = basePrice * 0.85;

        for (int i = 0; i < 180; i++) {
            price += (random.nextDouble() - 0.5) * 4;
            if (price < 10) {
                price = 10;
            }

            history.add(HistoricalPrice.builder()
                    .date(startDate.plusDays(i).toString())
                    .price(Math.round(price * 100.0) / 100.0)
                    .build());
        }

        return history;
    }

    @Cacheable(value = "stockBySymbol", key = "#ticker")
    public Stock getStockByTicker(String ticker) {
        return getTop10Stocks().stream()
                .filter(stock -> stock.getTicker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElse(null);
    }
}