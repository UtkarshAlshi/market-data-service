package com.tradewise.marketdataservice.controller;

import com.tradewise.marketdataservice.service.MarketDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ta4j.core.BarSeries; // <-- ADD
import org.springframework.web.bind.annotation.RequestParam; // <-- ADD
import java.time.LocalDate; // <-- ADD

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    /**
     * GET /api/market-data/{symbol}
     * Fetches the latest price for a given stock symbol (e.g., AAPL).
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<String> getMarketData(
            @PathVariable String symbol,
            @RequestHeader("X-User-Email") String userEmail) { // <-- ADD

        Optional<BigDecimal> price = marketDataService.getLatestPrice(symbol.toUpperCase());

        if (price.isPresent()) {
            return ResponseEntity.ok(price.get().toString());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // --- ADD THIS NEW ENDPOINT ---
    @GetMapping("/history/internal")
    public ResponseEntity<BarSeries> getHistoricalData(
            @RequestParam String symbol,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestHeader("X-User-Email") String userEmail) {
        
        // We don't use the email, but it confirms auth
        BarSeries series = marketDataService.getHistoricalData(symbol, startDate, endDate);
        return ResponseEntity.ok(series);
    }
}
