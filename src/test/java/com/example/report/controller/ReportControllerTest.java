package com.example.report.controller;

import com.example.report.model.*;
import com.example.report.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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

        String responseAsAString = mockMvc.perform(MockMvcRequestBuilders.post("/report/generate/{from}/{to}", from, to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<ReportEntryDTO> actualReportEntryDTOsList = objectMapper.readValue(responseAsAString, new TypeReference<>() {
        });

        Assertions.assertEquals(392, actualReportEntryDTOsList.get(0).getNetAmount().intValue());
        Assertions.assertEquals(98, actualReportEntryDTOsList.get(0).getPersonalIncomeTax().intValue());
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

        assertThat(reportEntry.getNetAmount().intValue()).isEqualTo(actualReportEntryDTOsList.get(0).getNetAmount().intValue());
        assertThat(reportEntry.getPensionsFund().intValue()).isEqualTo(actualReportEntryDTOsList.get(0).getPensionsFund().intValue());
    }
}
