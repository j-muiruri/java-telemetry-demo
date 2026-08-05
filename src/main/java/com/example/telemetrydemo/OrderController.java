package com.example.telemetrydemo;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    // Obtain Tracer and Counter from the global OpenTelemetry instance
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("com.example.telemetrydemo");
    private final LongCounter orderCounter = GlobalOpenTelemetry.getMeter("com.example.telemetrydemo")
            .counterBuilder("orders_created_total")
            .setDescription("Total count of processed orders")
            .setUnit("1")
            .build();

    @PostMapping
    public Map<String, Object> createOrder(@RequestParam String item, @RequestParam double amount) throws InterruptedException {
        log.info("Received request to process order for item: {}", item);

        // Record a custom metric increment
        orderCounter.add(1);

        // Add custom attributes to the active trace span automatically managed by OTel Agent
        Span currentSpan = Span.current();
        currentSpan.setAttribute("order.item", item);
        currentSpan.setAttribute("order.amount", amount);

        // Simulate nested business logic inside a custom sub-span
        processPayment(amount);

        return Map.of("status", "SUCCESS", "item", item, "amount", amount);
    }

    private void processPayment(double amount) throws InterruptedException {
        // Start an explicit child span for payment processing execution path
        Span childSpan = tracer.spanBuilder("processPayment").startSpan();
        try (var scope = childSpan.makeCurrent()) {
            log.info("Processing payment of ${}...", amount);
            
            // Artificial delay
            Thread.sleep(new Random().nextInt(100) + 50);

            if (amount > 1000) {
                log.warn("High-value transaction detected: ${}", amount);
                childSpan.setAttribute("payment.flag", "HIGH_VALUE");
            }
        } finally {
            childSpan.end();
        }
    }
}
