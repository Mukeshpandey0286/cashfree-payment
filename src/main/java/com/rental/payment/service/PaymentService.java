package com.rental.payment.service;

import com.rental.payment.dto.PaymentRequest;
import com.rental.payment.dto.PaymentResponse;
import com.rental.payment.entity.PaymentEntity;
import com.rental.payment.entity.PaymentStatus;
import com.rental.payment.repository.PaymentRepository;
import com.rental.payment.config.CashfreeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CashfreeConfig cashfreeConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public PaymentResponse createPaymentOrder(PaymentRequest request) {
        try {
            // Check if order already exists
            Optional<PaymentEntity> existingPayment = paymentRepository.findByOrderId(request.getOrderId());
            if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
                return new PaymentResponse(false, "Order already processed successfully");
            }

            // Create payment entity
            PaymentEntity payment = new PaymentEntity();
            payment.setOrderId(request.getOrderId());
            payment.setAmount(request.getAmount());
            payment.setCurrency(request.getCurrency());
            payment.setCustomerEmail(request.getCustomerEmail());
            payment.setCustomerPhone(request.getCustomerPhone());

            // Create order with Cashfree
            Map<String, String> cashfreeResult = createCashfreeOrder(request);
            if (cashfreeResult != null && cashfreeResult.containsKey("cf_order_id")) {
                payment.setCfOrderId(cashfreeResult.get("cf_order_id"));
                paymentRepository.save(payment);

                PaymentResponse response = new PaymentResponse(true, "Payment order created successfully");
                response.setOrderId(request.getOrderId());
                response.setCfOrderId(cashfreeResult.get("cf_order_id"));
                response.setPaymentSessionId(cashfreeResult.get("payment_session_id"));
                response.setAmount(request.getAmount());
                response.setStatus(PaymentStatus.PENDING);
                return response;
            } else {
                return new PaymentResponse(false, "Failed to create payment order");
            }

        } catch (Exception e) {
            logger.error("Error creating payment order: ", e);
            return new PaymentResponse(false, "Internal server error");
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(String orderId) {
        try {
            Optional<PaymentEntity> paymentOpt = paymentRepository.findByOrderId(orderId);
            if (!paymentOpt.isPresent()) {
                return new PaymentResponse(false, "Payment order not found");
            }

            PaymentEntity payment = paymentOpt.get();

            // Get payment status from Cashfree
            JsonNode paymentDetails = getPaymentStatus(payment.getCfOrderId());
            if (paymentDetails != null) {
                updatePaymentFromCashfreeResponse(payment, paymentDetails);
                paymentRepository.save(payment);

                PaymentResponse response = new PaymentResponse();
                response.setSuccess(payment.getStatus() == PaymentStatus.SUCCESS);
                response.setMessage(getStatusMessage(payment.getStatus()));
                response.setOrderId(payment.getOrderId());
                response.setCfOrderId(payment.getCfOrderId());
                response.setAmount(payment.getAmount());
                response.setStatus(payment.getStatus());
                response.setPaymentMethod(payment.getPaymentMethod());
                response.setGatewayTransactionId(payment.getGatewayTransactionId());

                return response;
            } else {
                return new PaymentResponse(false, "Failed to verify payment with gateway");
            }

        } catch (Exception e) {
            logger.error("Error verifying payment: ", e);
            return new PaymentResponse(false, "Internal server error");
        }
    }

    @Transactional
    public PaymentResponse handleWebhook(String payload, String signature) {
        try {
            // Verify webhook signature
            if (!verifyWebhookSignature(payload, signature)) {
                logger.warn("Invalid webhook signature");
                return new PaymentResponse(false, "Invalid signature");
            }

            JsonNode webhookData = objectMapper.readTree(payload);
            String cfOrderId = webhookData.path("data").path("order").path("cf_order_id").asText();

            Optional<PaymentEntity> paymentOpt = paymentRepository.findByCfOrderId(cfOrderId);
            if (!paymentOpt.isPresent()) {
                logger.warn("Payment not found for webhook: {}", cfOrderId);
                return new PaymentResponse(false, "Payment not found");
            }

            PaymentEntity payment = paymentOpt.get();
            updatePaymentFromWebhook(payment, webhookData);
            paymentRepository.save(payment);

            logger.info("Webhook processed successfully for order: {}", payment.getOrderId());
            return new PaymentResponse(true, "Webhook processed successfully");

        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return new PaymentResponse(false, "Webhook processing failed");
        }
    }

    private Map<String, String> createCashfreeOrder(PaymentRequest request) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(cashfreeConfig.getBaseUrl() + "/orders");

            // Set headers
            post.setHeader("Content-Type", "application/json");
            post.setHeader("x-client-id", cashfreeConfig.getAppId());
            post.setHeader("x-client-secret", cashfreeConfig.getSecretKey());
            post.setHeader("x-api-version", "2022-09-01");

            // Create request body
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("order_id", request.getOrderId());
            orderData.put("order_amount", request.getAmount());
            orderData.put("order_currency", request.getCurrency());

            Map<String, String> customer = new HashMap<>();
            customer.put("customer_id", request.getCustomerId());
            customer.put("customer_email", request.getCustomerEmail());
            if (request.getCustomerPhone() != null && !request.getCustomerPhone().trim().isEmpty()) {
                customer.put("customer_phone", request.getCustomerPhone());
            }
            orderData.put("customer_details", customer);

            orderData.put("order_meta", Map.of("return_url", request.getReturnUrl()));

            String jsonBody = objectMapper.writeValueAsString(orderData);
            logger.info("Cashfree request: {}", jsonBody);
            post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                logger.info("Cashfree response: {}", responseBody);

                if (response.getCode() == 200) {
                    JsonNode responseJson = objectMapper.readTree(responseBody);
                    Map<String, String> result = new HashMap<>();
                    result.put("cf_order_id", responseJson.path("cf_order_id").asText());
                    result.put("payment_session_id", responseJson.path("payment_session_id").asText());
                    return result;
                } else {
                    logger.error("Cashfree order creation failed: {}", responseBody);
                    return null;
                }
            }

        } catch (Exception e) {
            logger.error("Error creating Cashfree order: ", e);
            return null;
        }
    }

    private JsonNode getPaymentStatus(String cfOrderId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(cashfreeConfig.getBaseUrl() + "/orders/" + cfOrderId + "/payments");

            // Set headers
            get.setHeader("x-client-id", cashfreeConfig.getAppId());
            get.setHeader("x-client-secret", cashfreeConfig.getSecretKey());
            get.setHeader("x-api-version", "2022-09-01");

            try (CloseableHttpResponse response = client.execute(get)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getCode() == 200) {
                    return objectMapper.readTree(responseBody);
                } else {
                    logger.error("Failed to get payment status: {}", responseBody);
                    return null;
                }
            }

        } catch (Exception e) {
            logger.error("Error getting payment status: ", e);
            return null;
        }
    }

    private void updatePaymentFromCashfreeResponse(PaymentEntity payment, JsonNode response) {
        try {
            if (response.isArray() && response.size() > 0) {
                JsonNode paymentData = response.get(0);

                String status = paymentData.path("payment_status").asText();
                payment.setStatus(mapCashfreeStatus(status));
                payment.setPaymentMethod(paymentData.path("payment_method").asText());
                payment.setGatewayTransactionId(paymentData.path("cf_payment_id").asText());

                if (payment.getStatus() == PaymentStatus.FAILED) {
                    payment.setFailureReason(paymentData.path("payment_message").asText());
                }

                payment.setRawResponse(response.toString());
            }
        } catch (Exception e) {
            logger.error("Error updating payment from Cashfree response: ", e);
        }
    }

    private void updatePaymentFromWebhook(PaymentEntity payment, JsonNode webhookData) {
        try {
            JsonNode paymentData = webhookData.path("data").path("payment");

            String status = paymentData.path("payment_status").asText();
            payment.setStatus(mapCashfreeStatus(status));
            payment.setPaymentMethod(paymentData.path("payment_method").asText());
            payment.setGatewayTransactionId(paymentData.path("cf_payment_id").asText());

            if (payment.getStatus() == PaymentStatus.FAILED) {
                payment.setFailureReason(paymentData.path("payment_message").asText());
            }

            payment.setRawResponse(webhookData.toString());
        } catch (Exception e) {
            logger.error("Error updating payment from webhook: ", e);
        }
    }

    private PaymentStatus mapCashfreeStatus(String cashfreeStatus) {
        switch (cashfreeStatus.toUpperCase()) {
            case "SUCCESS":
                return PaymentStatus.SUCCESS;
            case "FAILED":
                return PaymentStatus.FAILED;
            case "CANCELLED":
                return PaymentStatus.CANCELLED;
            case "PENDING":
            default:
                return PaymentStatus.PENDING;
        }
    }

    private String getStatusMessage(PaymentStatus status) {
        switch (status) {
            case SUCCESS:
                return "Payment completed successfully";
            case FAILED:
                return "Payment failed";
            case CANCELLED:
                return "Payment cancelled";
            case PENDING:
            default:
                return "Payment is pending";
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    cashfreeConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature: ", e);
            return false;
        }
    }
}