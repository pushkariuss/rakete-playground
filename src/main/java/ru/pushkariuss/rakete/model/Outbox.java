package ru.pushkariuss.rakete.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity(name = "outbox")
public class Outbox {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "status", insertable = false)
    @Enumerated(EnumType.STRING)
    @Generated(event = EventType.INSERT)
    private OutboxStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private Instant createdAt;

}
