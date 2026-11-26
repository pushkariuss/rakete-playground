package ru.pushkariuss.rakete.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pushkariuss.rakete.model.Order;

import java.util.UUID;

@Repository
public interface OrdersRepository extends JpaRepository<Order, UUID> {

    @Modifying
    @Query("UPDATE orders SET status = 'PROCESSED' WHERE id = :orderId AND status = 'CREATED'")
    void markProcessed(@Param("orderId") UUID orderId);
}
