package com.rental.payment.repository;

import com.rental.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByOrderId(String orderId);

    Optional<PaymentEntity> findByCfOrderId(String cfOrderId);
}