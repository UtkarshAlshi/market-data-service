package com.tradewise.marketdataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradewise.marketdataservice.dto.finnhub.FinnhubMessage;
import com.tradewise.marketdataservice.dto.finnhub.FinnhubTrade;
import com.tradewise.marketdataservice.dto.response.StockPriceUpdate;
import jakarta.annotation.PostConstruct;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
public class MarketDataBroker {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataBroker.class);

    @Value("${tradewise.app.finnhub.apikey}")
    private String finnhubApiKey;

    @Value("${tradewise.app.finnhub.websocket.url}")
    private String finnhubWsUrl;

    private static final List<String> SYMBOLS_TO_SUBSCRIBE = Arrays.asList(
            "AAPL", "MSFT", "GOOGL", "AMZN", "IBM", "BINANCE:BTCUSDT", "BINANCE:ETHUSDT"
    );

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, StockPriceUpdate> kafkaTemplate;
    private WebSocketClient webSocketClient;

    public MarketDataBroker(ObjectMapper objectMapper,
                            KafkaTemplate<String, StockPriceUpdate> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void connect() {
        try {
            URI uri = new URI(finnhubWsUrl + "?token=" + finnhubApiKey);

            this.webSocketClient = new WebSocketClient(uri) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("Connected to Finnhub WebSocket.");
                    for (String symbol : SYMBOLS_TO_SUBSCRIBE) {
                        String subscribeMessage = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
                        logger.info("Subscribing to " + symbol);
                        this.send(subscribeMessage);
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        FinnhubMessage finnhubMessage = objectMapper.readValue(message, FinnhubMessage.class);

                        if ("trade".equalsIgnoreCase(finnhubMessage.getType()) && finnhubMessage.getData() != null) {
                            for (FinnhubTrade trade : finnhubMessage.getData()) {
                                if (trade.getSymbol() != null && trade.getPrice() != null) {
                                    broadcastPrice(trade.getSymbol(), trade.getPrice());
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        // logger.debug("Received non-trade message: " + message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("Disconnected from Finnhub WebSocket: " + reason);
                    // TODO: Implement reconnection logic
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("Finnhub WebSocket error", ex);
                }
            };

            logger.info("Connecting to Finnhub...");
            this.webSocketClient.connectBlocking();

        } catch (Exception e) {
            logger.error("Failed to connect to WebSocket", e);
        }
    }

    private void broadcastPrice(String symbol, BigDecimal price) {
        StockPriceUpdate update = new StockPriceUpdate(symbol, price);

        logger.info("Publishing price update to Kafka: {} at Price: ${}", symbol, price);
        kafkaTemplate.send("price-updates", symbol, update);
        logger.debug("Kafka send completed for: {}", symbol);
    }
}
