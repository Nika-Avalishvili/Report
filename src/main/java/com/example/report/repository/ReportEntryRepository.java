package com.example.report.repository;

import com.example.report.model.ReportEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportEntryRepository extends JpaRepository<ReportEntry, Long> {
    List<ReportEntry> findAllByReportId(Long reportId);

    List<ReportEntry> findAllByEmployeeIdAndReportId(Long employeeId, Long reportId);
}
