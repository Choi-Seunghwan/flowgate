package com.chuz.flowgate.payment.repository;

import com.chuz.flowgate.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findBySagaId(String sagaId);

	Optional<Payment> findByReservationId(Long reservationId);
}
