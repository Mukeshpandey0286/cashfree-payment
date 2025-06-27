package com.rental.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.rental.payment.entity.PaymentStatus;

public class PaymentResponse {
    private boolean success;
    private String message;
    private String orderId;
    private String cfOrderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String gatewayTransactionId;
    private LocalDateTime timestamp;
    private String paymentSessionId; // Added for Cashfree payment session

    // Constructors
    public PaymentResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCfOrderId() {
        return cfOrderId;
    }

    public void setCfOrderId(String cfOrderId) {
        this.cfOrderId = cfOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPaymentSessionId() {
        return paymentSessionId;
    }

    public void setPaymentSessionId(String paymentSessionId) {
        this.paymentSessionId = paymentSessionId;
    }
}