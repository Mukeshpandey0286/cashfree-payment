package com.rental.payment.controller;

import com.rental.payment.dto.PaymentRequest;
import com.rental.payment.dto.PaymentResponse;
import com.rental.payment.entity.PaymentEntity;
import com.rental.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;

@RestController
@RequestMapping("/api/payments")
@Validated
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createPaymentOrder(@Valid @RequestBody PaymentRequest request) {
        logger.info("Creating payment order for: {}", request.getOrderId());

        PaymentResponse response = paymentService.createPaymentOrder(request);

        if (response.isSuccess()) {
            logger.info("Payment order created successfully: {}", request.getOrderId());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Payment order creation failed: {}", response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verify/{orderId}")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @PathVariable @NotBlank(message = "Order ID is required") String orderId) {
        logger.info("Verifying payment for order: {}", orderId);

        PaymentResponse response = paymentService.verifyPayment(orderId);

        if (response.isSuccess()) {
            logger.info("Payment verification successful: {}", orderId);
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Payment verification failed: {} - {}", orderId, response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) {
        try {
            // Read the raw payload
            StringBuilder payload = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    payload.append(line);
                }
            }

            String signature = request.getHeader("x-webhook-signature");

            logger.info("Processing webhook with signature: {}", signature);

            PaymentResponse response = paymentService.handleWebhook(payload.toString(), signature);

            if (response.isSuccess()) {
                return ResponseEntity.ok("Webhook processed successfully");
            } else {
                return ResponseEntity.badRequest().body("Webhook processing failed");
            }

        } catch (IOException e) {
            logger.error("Error reading webhook payload: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment service is healthy");
    }
}