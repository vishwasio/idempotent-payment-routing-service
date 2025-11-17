package com.payment.route.repository;

import com.payment.route.model.DeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {
    Optional<DeadLetter> findByEventId(long eventId);
}
