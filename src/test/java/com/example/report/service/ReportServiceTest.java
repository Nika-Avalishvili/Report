package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    ReportRepository reportRepository;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    EmployeeRepository employeeRepository;
    @Mock
    ReportEntryRepository reportEntryRepository;
    @Mock
    BenefitRepository benefitRepository;
    ReportMapper reportMapper;
    ReportEntryMapper reportEntryMapper;
    EmployeeMapper employeeMapper;
    BenefitMapper benefitMapper;
    DocumentMapper documentMapper;
    ReportService reportService;

    @BeforeEach
    void setUp() {
        reportMapper = new ReportMapper();
        employeeMapper = new EmployeeMapper();
        benefitMapper = new BenefitMapper();
        documentMapper = new DocumentMapper();
        reportEntryMapper = new ReportEntryMapper(employeeMapper, benefitMapper, documentMapper, reportMapper);
        reportService = new ReportService(documentRepository,
                employeeRepository,
                benefitRepository,
                reportRepository,
                reportEntryRepository,
                reportEntryMapper,
                reportMapper);
    }

    @Test
    void generateReportEntries() {
        Document document = Document.builder()
                .id(1L)
                .uploadDate(LocalDate.of(2022, 11, 30))
                .effectiveDate(LocalDate.of(2022, 11, 30))
                .employeeId(1L)
                .benefitId(1L)
                .amount(BigDecimal.valueOf(500))
                .build();
        Mockito.when(documentRepository.findByEffectiveDateBetween(any(), any())).thenReturn(List.of(document));

        Report report = Report.builder()
                .id(1L)
                .startDate(LocalDate.of(2022, 1, 1))
                .endDate(LocalDate.of(2022, 12, 31))
                .build();
        Mockito.when(reportRepository.save(any())).thenReturn(report);

        Employee employee = Employee.builder()
                .id(1L)
                .firstName("Nika")
                .lastName("Avalishvili")
                .department("Business Development")
                .positions("Business Builder")
                .email("avalishvili.nick@gmail.com")
                .isActive(true)
                .isPensionsPayer(true)
                .build();
        Mockito.when(employeeRepository.getReferenceById(anyLong())).thenReturn(employee);

        Benefit benefit = Benefit.builder()
                .id(1L)
                .name("Annual Bonus")
                .benefitTypeName("Accrual")
                .calculationMethodName("Gross")
                .build();
        Mockito.when(benefitRepository.getReferenceById(anyLong())).thenReturn(benefit);

        List<ReportEntryDTO> reportEntryDTOS = reportService.generateReportEntries(LocalDate.of(2022, 1, 1), LocalDate.of(202, 12, 31));

        Assertions.assertEquals(392, reportEntryDTOS.get(0).getNetAmount().intValue());
        Assertions.assertEquals(98, reportEntryDTOS.get(0).getPersonalIncomeTax().intValue());

    }

    @Test
    void getAllReports() {
        LocalDate testDate = LocalDate.of(2022, 10, 26);
        Report report1 = new Report(1L, testDate, testDate);
        Report report2 = new Report(6L, testDate, testDate);

        Mockito.when(reportRepository.findAll()).thenReturn(List.of(report1, report2));

        List<ReportDTO> reportDTOS = reportService.getAllReports();

        Assertions.assertEquals(2, reportDTOS.size());
        Assertions.assertEquals(6L, reportDTOS.get(1).getId());
    }

    @Test
    void getReportEntriesByReportId() {
        ReportEntry reportEntry2 = ReportEntry.builder()
                .report(new Report(8L, LocalDate.of(2022, 1, 30), LocalDate.of(2022, 1, 30)))
                .employee(new Employee())
                .benefit(new Benefit())
                .netAmount(BigDecimal.valueOf(6000))
                .personalIncomeTax(BigDecimal.valueOf(1000))
                .pensionsFund(BigDecimal.valueOf(500))
                .grossAmount(BigDecimal.valueOf(7500))
                .document(new Document())
                .build();

        Mockito.when(reportEntryRepository.findAllByReportId(anyLong())).thenReturn(List.of(reportEntry2));

        Assertions.assertEquals(6000, reportService.getReportEntriesByReportId(8L).get(0).getNetAmount().intValue());
    }
}