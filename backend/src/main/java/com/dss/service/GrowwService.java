package com.dss.service;

import com.dss.client.GrowwApiClient;
import com.dss.dto.groww.GrowwOrderRequest;
import com.dss.dto.groww.GrowwOrderResponse;
import com.dss.dto.groww.GrowwQuoteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Business-layer wrapper around {@link GrowwApiClient}.
 * Provides validation, default values, and logging on top of the raw HTTP calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrowwService {

    private final GrowwApiClient growwApiClient;

    /**
     * Get a live quote for a symbol on the specified exchange.
     */
    public GrowwQuoteResponse getQuote(String exchange, String tradingSymbol) {
        validateNotBlank(tradingSymbol, "tradingSymbol");
        if (exchange == null || exchange.isBlank()) {
            exchange = "NSE";
        }
        log.info("Fetching Groww quote: {}:{}", exchange, tradingSymbol);
        return growwApiClient.getQuote(exchange, tradingSymbol);
    }

    /**
     * Place an order after basic input validation.
     */
    public GrowwOrderResponse placeOrder(GrowwOrderRequest request) {
        validateNotBlank(request.getTradingSymbol(), "tradingSymbol");
        validateNotBlank(request.getTransactionType(), "transactionType");
        validateNotBlank(request.getOrderType(), "orderType");

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        if (request.getExchange() == null || request.getExchange().isBlank()) {
            request.setExchange("NSE");
        }
        if (request.getProductType() == null || request.getProductType().isBlank()) {
            request.setProductType("CNC");
        }

        // LIMIT & SL orders need a price
        String ot = request.getOrderType().toUpperCase();
        if (("LIMIT".equals(ot) || "SL".equals(ot)) && request.getPrice() == null) {
            throw new IllegalArgumentException("price is required for " + ot + " orders");
        }
        if (("SL".equals(ot) || "SL-M".equals(ot)) && request.getTriggerPrice() == null) {
            throw new IllegalArgumentException("triggerPrice is required for " + ot + " orders");
        }

        log.info("Placing Groww order: {} {} {} qty={} price={}",
                request.getExchange(), request.getTransactionType(),
                request.getTradingSymbol(), request.getQuantity(), request.getPrice());

        return growwApiClient.placeOrder(request);
    }

    private void validateNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
