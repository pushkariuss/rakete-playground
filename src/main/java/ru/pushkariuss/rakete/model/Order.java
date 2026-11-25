package ru.pushkariuss.rakete.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private Long userId;

    @Column
    private BigDecimal amount;

    @Column(name = "status", insertable = false)
    @Generated(event = EventType.INSERT)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private Instant createdAt;

}
