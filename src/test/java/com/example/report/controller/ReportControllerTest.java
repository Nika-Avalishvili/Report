package com.example.report.controller;

import com.example.report.model.*;
import com.example.report.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    ReportRepository reportRepository;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    ReportEntryRepository reportEntryRepository;
    @Autowired
    BenefitRepository benefitRepository;
    @Autowired
    ReportMapper reportMapper;

    @BeforeEach
    void cleanUp() {
        reportRepository.deleteAll();
        documentRepository.deleteAll();
        employeeRepository.deleteAll();
        reportEntryRepository.deleteAll();
        benefitRepository.deleteAll();
    }


    @Test
    void generateReportEntries() throws Exception {
        Document document = Document.builder()
                .id(1L)
                .uploadDate(LocalDate.of(2022, 11, 30))
                .effectiveDate(LocalDate.of(2022, 11, 30))
                .employeeId(1L)
                .benefitId(1L)
                .amount(BigDecimal.valueOf(500))
                .build();
        documentRepository.save(document);

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
        employeeRepository.save(employee);

        Benefit benefit = Benefit.builder()
                .id(1L)
                .name("Annual Bonus")
                .benefitTypeName("Accrual")
                .calculationMethodName("Gross")
                .build();
        benefitRepository.save(benefit);

        LocalDate from = LocalDate.of(2022, 1, 1);
        LocalDate to = LocalDate.of(2022, 12, 31);

        String responseAsAString = mockMvc.perform(MockMvcRequestBuilders.post("/report/generate?startDate={from}&endDate={to}", from, to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ReportEntryDTO> actualReportEntryDTOsList = objectMapper.readValue(responseAsAString, new TypeReference<>() {
        });

        assertThat(actualReportEntryDTOsList.get(0).getNetAmount()).isEqualByComparingTo(BigDecimal.valueOf(392));
        assertThat(actualReportEntryDTOsList.get(0).getPersonalIncomeTax()).isEqualByComparingTo(BigDecimal.valueOf(98));
    }

    @Test
    void getAllReports() throws Exception {
        LocalDate testDate = LocalDate.of(2022, 10, 26);
        ReportDTO report1 = new ReportDTO(1L, testDate, testDate);
        ReportDTO report2 = new ReportDTO(2L, testDate, testDate);

        List<ReportDTO> expectedReportDTOS = List.of(report1, report2);
        reportRepository.saveAll(reportMapper.dtoToEntity(expectedReportDTOS));

        String responseAsAString = mockMvc.perform(MockMvcRequestBuilders.get("/report"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<ReportDTO> actualReportDTOsList = objectMapper.readValue(responseAsAString, new TypeReference<>() {
        });
        assertThat(expectedReportDTOS).isEqualTo(actualReportDTOsList);
    }

    @Test
    void getReportEntriesByReportId() throws Exception {
        ReportEntry reportEntry = ReportEntry.builder()
                .id(1L)
                .report(new Report(8L, LocalDate.of(2022, 1, 30), LocalDate.of(2022, 1, 30)))
                .employee(new Employee(1L, "Nika", "Avalishvili", "Department", "Position", "Email", true, true))
                .benefit(new Benefit(1L, "Bonus", "Accrual", "Gross"))
                .netAmount(BigDecimal.valueOf(6000))
                .personalIncomeTax(BigDecimal.valueOf(1000))
                .pensionsFund(BigDecimal.valueOf(500))
                .grossAmount(BigDecimal.valueOf(7500))
                .document(new Document(1L, LocalDate.of(2022, 1, 2), LocalDate.of(2022, 2, 2), 1L, 1L, BigDecimal.valueOf(600)))
                .build();

        Long reportId = reportEntryRepository.save(reportEntry).getReport().getId();

        String responseAsAString = mockMvc.perform(MockMvcRequestBuilders.get("/report/{id}", reportId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ReportEntry> actualReportEntryDTOsList = objectMapper.readValue(responseAsAString, new TypeReference<>() {
        });

        assertThat(reportEntry.getNetAmount()).isEqualByComparingTo(actualReportEntryDTOsList.get(0).getNetAmount());
        assertThat(reportEntry.getPensionsFund()).isEqualByComparingTo(actualReportEntryDTOsList.get(0).getPensionsFund());
    }

    @Test
    void extractAllReports() throws Exception {
        LocalDate testDate = LocalDate.of(2022, 8, 15);
        Report report1 = new Report(10L, testDate, testDate);
        Report report2 = new Report(16L, testDate, testDate);
        reportRepository.saveAll(List.of(report1, report2));

        byte[] contentAsString = mockMvc.perform(MockMvcRequestBuilders.get("/report/extractReports"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentAsString);

        Workbook workbook = WorkbookFactory.create(byteArrayInputStream);

        LocalDate actualStartDate = workbook.getSheetAt(0).getRow(1).getCell(1).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actualEndDate = workbook.getSheetAt(0).getRow(1).getCell(1).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        assertThat(actualStartDate).isEqualTo(report1.getStartDate());
        assertThat(actualEndDate).isEqualTo(report2.getEndDate());
    }


    @Test
    void extractReportEntriesByReportId() throws Exception {
        LocalDate testDate = LocalDate.of(2022, 3, 14);
        LocalDate startDate = LocalDate.of(2021, 3, 14);
        LocalDate endDate = LocalDate.of(2023, 3, 14);

        Document document = new Document(1L, testDate, testDate, 1L, 1L, BigDecimal.valueOf(1020));
        Employee employee = new Employee(1L, "Nika", "Avalishvili", "Department", "Position", "email", true, true);
        Benefit benefit = new Benefit(1L, "Salary", "Accrual", "Gross");
        Report report = new Report(1L, startDate, endDate);

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

        employeeRepository.save(employee);
        documentRepository.save(document);
        benefitRepository.save(benefit);
        reportEntryRepository.save(reportEntry);
        reportRepository.save(report);

        Long reportId = reportEntryRepository.save(reportEntry).getReport().getId();

        byte[] contentAsString = mockMvc.perform(MockMvcRequestBuilders.get("/report/extractReportEntries?reportId={reportId}", reportId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentAsString);

        Workbook workbook = WorkbookFactory.create(byteArrayInputStream);

        assertThat(workbook.getSheetAt(0).getRow(1).getCell(8).getNumericCellValue()).isEqualByComparingTo(reportEntry.getGrossAmount().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(1).getCell(11).getNumericCellValue()).isEqualByComparingTo(reportEntry.getPensionsFund().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue()).isEqualTo("Nika");
    }

    @Test
    void extractPaySlipsByEmployeeIdAndReportId() throws Exception {
        LocalDate testDate = LocalDate.of(2022, 3, 14);
        LocalDate startDate = LocalDate.of(2021, 3, 14);
        LocalDate endDate = LocalDate.of(2023, 3, 14);

        Document document = new Document(1L, testDate, testDate, 1L, 1L, BigDecimal.valueOf(1020));
        Employee employee = new Employee(1L, "Nika", "Avalishvili", "Department", "Position", "email", true, true);
        Benefit benefit = new Benefit(1L, "Salary", "Accrual", "Gross");
        Report report = new Report(1L, startDate, endDate);

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


        documentRepository.save(document);
        benefitRepository.save(benefit);
        reportEntryRepository.save(reportEntry);
        reportRepository.save(report);
        Long employeeId = employeeRepository.save(employee).getId();
        Long reportId = reportEntryRepository.save(reportEntry).getReport().getId();

        byte[] contentAsString = mockMvc.perform(MockMvcRequestBuilders.get("/report/extractPaySlip?employeeId={employeeID}&reportId={reportId}", employeeId, reportId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsByteArray();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(contentAsString);

        Workbook workbook = WorkbookFactory.create(byteArrayInputStream);

        assertThat(workbook.getSheetAt(0).getRow(3).getCell(2).getNumericCellValue()).isEqualByComparingTo(reportEntry.getGrossAmount().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(4).getCell(3).getNumericCellValue()).isEqualByComparingTo(reportEntry.getPensionsFund().doubleValue());
        assertThat(workbook.getSheetAt(0).getRow(5).getCell(3).getNumericCellValue()).isEqualByComparingTo(reportEntry.getPersonalIncomeTax().doubleValue());
    }
}
