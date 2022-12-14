package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import org.apache.poi.ss.usermodel.Workbook;
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

import static org.assertj.core.api.Assertions.assertThat;
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
                .effectiveDate(LocalDate.of(2022, 4, 3))
                .employeeId(1L)
                .benefitId(1L)
                .amount(BigDecimal.valueOf(500))
                .build();
        Mockito.when(documentRepository.findByEffectiveDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(document));

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

        ReportEntry reportEntry = ReportEntry.builder()
                .report(report)
                .employee(employee)
                .benefit(benefit)
                .netAmount(BigDecimal.valueOf(392))
                .personalIncomeTax(BigDecimal.valueOf(98))
                .pensionsFund(BigDecimal.valueOf(10))
                .grossAmount(BigDecimal.valueOf(500))
                .document(document)
                .build();
        Mockito.when(reportEntryRepository.saveAll(any())).thenReturn(List.of(reportEntry));

        List<ReportEntryDTO> reportEntryDTOS = reportService.generateReportEntries(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31));

        assertThat(reportEntryDTOS.get(0).getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(392));
        assertThat(reportEntryDTOS.get(0).getPersonalIncomeTax()).isEqualByComparingTo(BigDecimal.valueOf(98));
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
        ReportEntry reportEntry = ReportEntry.builder()
                .report(new Report(8L, LocalDate.of(2022, 1, 30), LocalDate.of(2022, 1, 30)))
                .employee(new Employee())
                .benefit(new Benefit())
                .netAmount(BigDecimal.valueOf(6000))
                .personalIncomeTax(BigDecimal.valueOf(1000))
                .pensionsFund(BigDecimal.valueOf(500))
                .grossAmount(BigDecimal.valueOf(7500))
                .document(new Document())
                .build();
        Mockito.when(reportEntryRepository.findAllByReportId(anyLong())).thenReturn(List.of(reportEntry));

        assertThat(reportService.getReportEntriesByReportId(8L).get(0).getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(6000));
    }

    @Test
    void extractAllReports() {
        LocalDate testDate = LocalDate.of(2022, 10, 26);
        Report report1 = new Report(17L, testDate, testDate);
        Report report2 = new Report(19L, testDate, testDate);
        Mockito.when(reportRepository.findAll()).thenReturn(List.of(report1, report2));

        Workbook workbook = reportService.extractAllReports();

        assertThat((long) workbook.getSheetAt(0).getRow(1).getCell(0).getNumericCellValue()).isEqualTo(report1.getId());
        assertThat((long) workbook.getSheetAt(0).getRow(2).getCell(0).getNumericCellValue()).isEqualTo(report2.getId());
    }

    @Test
    void extractReportEntriesByReportId() {
        LocalDate testDate = LocalDate.of(2022, 3, 14);

        Document document = new Document(1L, testDate, testDate, 1L, 1L, BigDecimal.valueOf(1020));
        Employee employee = new Employee(1L, "Nika", "Avalishvili", "Department", "Position", "email", true, true);
        Benefit benefit = new Benefit(1L, "Salary", "Accrual", "Gross");
        Report report = new Report(1L, testDate, testDate);

        ReportEntry reportEntry = ReportEntry.builder()
                .id(1L)
                .document(document)
                .employee(employee)
                .benefit(benefit)
                .report(report)
                .netAmount(BigDecimal.valueOf(509.6))
                .grossAmount(BigDecimal.valueOf(650))
                .personalIncomeTax(BigDecimal.valueOf(127.4))
                .pensionsFund(BigDecimal.valueOf(13))
                .build();
        Mockito.when(reportEntryRepository.findAllByReportId(anyLong())).thenReturn(List.of(reportEntry));
        Workbook workbook = reportService.extractReportEntriesByReportId(1L);

        assertThat(workbook.getSheetAt(0).getRow(1).getCell(8).getNumericCellValue()).isEqualByComparingTo(reportEntry.getGrossAmount().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(1).getCell(11).getNumericCellValue()).isEqualByComparingTo(reportEntry.getPensionsFund().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue()).isEqualTo("Nika");
    }
}
