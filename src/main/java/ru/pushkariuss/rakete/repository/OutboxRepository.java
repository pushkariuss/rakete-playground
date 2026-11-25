package ru.pushkariuss.rakete.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.pushkariuss.rakete.model.Order;
import ru.pushkariuss.rakete.model.Outbox;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    @Query(value = "SELECT * FROM outbox " +
            "WHERE status = 'NEW' " +
            "ORDER BY created_at ASC, id " +
            "LIMIT :limit " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> getNewOutboxRecords(@Param("limit") int limit);
}
