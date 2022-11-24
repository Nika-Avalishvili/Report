package com.example.report.repository;

import com.example.report.model.ReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportEntryRepository extends JpaRepository<ReportEntry, Long> {
}
