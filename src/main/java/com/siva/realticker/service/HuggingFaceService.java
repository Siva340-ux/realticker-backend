package com.siva.realticker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.realticker.dto.AnalysisResult;
import com.siva.realticker.model.HistoricalPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HuggingFaceService {

    @Value("${huggingface.api.token:}")
    private String hfToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable(value = "aiAnalysis", key = "#ticker + '-' + #history.hashCode()")
    public AnalysisResult analyzeStockHistory(String ticker, List<HistoricalPrice> history) {
        try {
            if (history == null || history.size() < 2) {
                return AnalysisResult.builder()
                        .trend("Sideways")
                        .riskLevel("Medium")
                        .suggestedAction("Short-term watch")
                        .reasoning("Not enough historical data available to generate a meaningful analysis.")
                        .build();
            }

            AnalysisResult computed = buildDeterministicAnalysis(ticker, history);

            if (hfToken == null || hfToken.isBlank()) {
                log.warn("Hugging Face token missing. Returning computed analysis for {}", ticker);
                return computed;
            }

            try {
                String prompt = buildAnalysisPrompt(ticker, history, computed);
                String rawResponse = callHuggingFace(prompt);
                String aiReasoning = extractGeneratedText(rawResponse);

                if (aiReasoning != null && !aiReasoning.isBlank()) {
                    computed.setReasoning(cleanReasoning(aiReasoning));
                    log.info("Hugging Face success for {} - enhanced reasoning added", ticker);
                }

                return computed;
            } catch (Exception hfEx) {
                log.warn("Hugging Face failed for {}. Using computed analysis. Error: {}", ticker, hfEx.getMessage());
                return computed;
            }

        } catch (Exception e) {
            log.error("Error analyzing stock {}: {}", ticker, e.getMessage(), e);
            return AnalysisResult.builder()
                    .trend("Sideways")
                    .riskLevel("Medium")
                    .suggestedAction("Short-term watch")
                    .reasoning("Unable to analyze this stock at the moment. Please try again later.")
                    .build();
        }
    }

    private AnalysisResult buildDeterministicAnalysis(String ticker, List<HistoricalPrice> history) {
        double startPrice = history.get(0).getPrice();
        double endPrice = history.get(history.size() - 1).getPrice();
        double totalChangePercent = ((endPrice - startPrice) / startPrice) * 100.0;

        double volatility = calculateVolatility(history);

        String trend;
        if (totalChangePercent > 8) {
            trend = "Upward";
        } else if (totalChangePercent < -8) {
            trend = "Downward";
        } else {
            trend = "Sideways";
        }

        String riskLevel;
        if (volatility < 1.5) {
            riskLevel = "Low";
        } else if (volatility < 3.5) {
            riskLevel = "Medium";
        } else {
            riskLevel = "High";
        }

        String suggestedAction;
        if ("Upward".equals(trend) && "Low".equals(riskLevel)) {
            suggestedAction = "Long-term investment";
        } else if ("Upward".equals(trend) && "Medium".equals(riskLevel)) {
            suggestedAction = "Short-term watch";
        } else if ("Sideways".equals(trend)) {
            suggestedAction = "Short-term watch";
        } else {
            suggestedAction = "Avoid with reason";
        }

        String reasoning = String.format(
                "%s moved from $%.2f to $%.2f over the last 6 months, a %.1f%% change. " +
                        "The stock shows a %s trend with %s volatility behavior, so the overall risk is classified as %s. " +
                        "For a beginner investor, the suggested action is %s based on recent price movement and stability.",
                ticker,
                startPrice,
                endPrice,
                totalChangePercent,
                trend.toLowerCase(),
                riskLevel.toLowerCase(),
                riskLevel,
                suggestedAction
        );

        return AnalysisResult.builder()
                .trend(trend)
                .riskLevel(riskLevel)
                .suggestedAction(suggestedAction)
                .reasoning(reasoning)
                .build();
    }

    private String buildAnalysisPrompt(String ticker, List<HistoricalPrice> history, AnalysisResult baseAnalysis) {
        double startPrice = history.get(0).getPrice();
        double endPrice = history.get(history.size() - 1).getPrice();
        double totalChangePercent = ((endPrice - startPrice) / startPrice) * 100.0;
        double volatility = calculateVolatility(history);

        return """
                You are a stock analysis assistant for beginner investors.
                Analyze this stock in simple language.

                Ticker: %s
                Start Price: %.2f
                End Price: %.2f
                6-Month Change: %.2f%%
                Volatility Score: %.2f
                Precomputed Trend: %s
                Precomputed Risk: %s
                Suggested Action: %s

                Write only a short reasoning paragraph in 2 to 3 sentences.
                Do not give headings.
                Do not give bullet points.
                Do not mention that you are an AI.
                Keep it beginner friendly.
                """.formatted(
                ticker,
                startPrice,
                endPrice,
                totalChangePercent,
                volatility,
                baseAnalysis.getTrend(),
                baseAnalysis.getRiskLevel(),
                baseAnalysis.getSuggestedAction()
        );
    }

    private String callHuggingFace(String prompt) {
        String url = "https://router.huggingface.co/models/gpt2";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hfToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = """
            {
              "inputs": "%s",
              "parameters": {
                "max_new_tokens": 100,
                "temperature": 0.7,
                "return_full_text": false
              }
            }
            """.formatted(prompt.replace("\"", "\\\""));

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Invalid Hugging Face response: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private String extractGeneratedText(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);

        if (root.isArray() && root.size() > 0 && root.get(0).has("generated_text")) {
            return root.get(0).get("generated_text").asText();
        }

        if (root.has("generated_text")) {
            return root.get("generated_text").asText();
        }

        if (root.has("error")) {
            throw new RuntimeException("Hugging Face API error: " + root.get("error").asText());
        }

        return null;
    }

    private String cleanReasoning(String text) {
        String cleaned = text.trim()
                .replaceAll("\\s+", " ")
                .replace("\"", "");

        if (cleaned.length() > 320) {
            cleaned = cleaned.substring(0, 320).trim() + "...";
        }

        return cleaned;
    }

    private double calculateVolatility(List<HistoricalPrice> history) {
        if (history.size() < 2) return 0.0;

        double totalAbsDailyChange = 0.0;

        for (int i = 1; i < history.size(); i++) {
            double previous = history.get(i - 1).getPrice();
            double current = history.get(i).getPrice();

            if (previous != 0) {
                double dailyChangePercent = Math.abs((current - previous) / previous) * 100.0;
                totalAbsDailyChange += dailyChangePercent;
            }
        }

        return totalAbsDailyChange / (history.size() - 1);
    }
}