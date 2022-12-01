package com.example.report.repository;

import com.example.report.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByEffectiveDateBetween(LocalDate from, LocalDate to);
}
