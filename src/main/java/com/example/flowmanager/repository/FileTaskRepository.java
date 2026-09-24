package com.example.flowmanager.repository;

import com.example.flowmanager.model.FileTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileTaskRepository extends JpaRepository<FileTask, UUID> {
}