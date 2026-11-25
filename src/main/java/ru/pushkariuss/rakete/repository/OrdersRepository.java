package ru.pushkariuss.rakete.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.pushkariuss.rakete.model.Order;

import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<Order, UUID> {
}
