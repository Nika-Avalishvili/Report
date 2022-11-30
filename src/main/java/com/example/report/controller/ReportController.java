package com.example.report.controller;

import com.example.report.model.ReportDTO;
import com.example.report.model.ReportEntryDTO;
import com.example.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate/{startDate}/{endDate}")
    public List<ReportEntryDTO> generateReportEntries(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
        return reportService.generateReportEntries(startDate, endDate);
    }

    @GetMapping()
    public List<ReportDTO> getAllReports(){
        return reportService.getAllReports();
    }

    @GetMapping("/{reportId}")
    public List<ReportEntryDTO> getReportEntriesByReportId(@PathVariable Long reportId){
        return reportService.getReportEntriesByReportId(reportId);
    }



}

