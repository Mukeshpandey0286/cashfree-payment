package com.rental.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;
import java.math.BigDecimal;

public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";

    @NotBlank(message = "Customer email is required")
    @Email(message = "Invalid email format")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "Return URL is required")
    private String returnUrl;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    // Constructors
    public PaymentRequest() {
    }

    public PaymentRequest(String orderId, BigDecimal amount, String currency,
            String customerEmail, String customerPhone, String customerId, String returnUrl) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.customerId = customerId;
        this.returnUrl = returnUrl;
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}
