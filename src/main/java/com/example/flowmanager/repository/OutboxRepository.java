package com.example.flowmanager.repository;

import com.example.flowmanager.model.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, java.util.UUID> {
    List<OutboxMessage> findByStatusOrderByCreatedAtAsc(String status);
}