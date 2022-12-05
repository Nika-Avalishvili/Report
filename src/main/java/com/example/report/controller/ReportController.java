package com.example.report.controller;

import com.example.report.model.ReportDTO;
import com.example.report.model.ReportEntryDTO;
import com.example.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public List<ReportEntryDTO> generateReportEntries(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.generateReportEntries(startDate, endDate);
    }

    @GetMapping()
    public List<ReportDTO> getAllReports() {
        return reportService.getAllReports();
    }

    @GetMapping("/extractReports")
    public ResponseEntity<ByteArrayResource> extractAllReports() throws IOException {
        Workbook workbook = reportService.extractAllReports();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String localDateTime = LocalDateTime.now().format(myFormatObj);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=List_of_Reports_%s.xlsx", localDateTime));
        workbook.write(stream);
        workbook.close();

        return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()),
                header, HttpStatus.CREATED);
    }

    @GetMapping("/extractReportEntries")
    public ResponseEntity<ByteArrayResource> extractReportEntriesByReportId(@RequestParam Long reportId) throws Exception {
        Workbook workbook = reportService.extractReportEntriesByReportId(reportId);
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String localDateTime = LocalDateTime.now().format(myFormatObj);

        org.apache.commons.io.output.ByteArrayOutputStream stream = new org.apache.commons.io.output.ByteArrayOutputStream();
        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "force-download"));
        header.set(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=Payroll_Register_%s.xlsx", localDateTime));
        workbook.write(stream);
        workbook.close();

        return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()),
                header, HttpStatus.CREATED);
    }

    @GetMapping("/{reportId}")
    public List<ReportEntryDTO> getReportEntriesByReportId(@PathVariable Long reportId) {
        return reportService.getReportEntriesByReportId(reportId);
    }
}

