package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String NET_AMOUNT = "netAmount";
    private static final String PENSIONS_FUND = "pensionsFund";
    private static final String PERSONAL_INCOME_TAX = "personalIncomeTax";
    private static final String GROSS_AMOUNT = "grossAmount";
    private static final String ACCRUAL = "Accrual";
    private static final String GROSS = "Gross";
    private static final String NET = "Net";
    private static final String DEDUCTION = "Deduction";

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final ReportRepository reportRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ReportEntryMapper reportEntryMapper;
    private final ReportMapper reportMapper;

    public Map<String, BigDecimal> calculator(BigDecimal amount, Employee employee, Benefit benefit) {
        Map<String, BigDecimal> calculatedNumbers = new HashMap<>();

        Boolean isActive = employee.getIsActive();
        Boolean isPensionsPayer = employee.getIsPensionsPayer();
        String benefitType = benefit.getBenefitTypeName();;
        String calculationMethod = benefit.getCalculationMethodName();
        if (!isActive) {
            calculatedNumbers.put(NET_AMOUNT, BigDecimal.valueOf(0));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.valueOf(0));
            calculatedNumbers.put(GROSS_AMOUNT, BigDecimal.valueOf(0));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.784)));
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply(BigDecimal.valueOf(0.02)));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.196)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply((BigDecimal.valueOf(0.02).divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP))));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(DEDUCTION)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply(BigDecimal.valueOf(-0.02)));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(-0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.8)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.8), RoundingMode.HALF_UP));
            return calculatedNumbers;
        } else {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        }

    }

    public ReportEntry documentEntryToReportEntry(Document document, Report report){
        Employee employee = employeeRepository.getReferenceById(document.getEmployeeId());
        Benefit benefit = benefitRepository.getReferenceById(document.getBenefitId());
        Map<String, BigDecimal> calculatedNumbers = calculator(document.getAmount(), employee, benefit);
        return ReportEntry.builder()
                .employee(employee)
                .benefit(benefit)
                .document(document)
                .netAmount(calculatedNumbers.get(NET_AMOUNT))
                .pensionsFund(calculatedNumbers.get(PENSIONS_FUND))
                .personalIncomeTax(calculatedNumbers.get(PERSONAL_INCOME_TAX))
                .grossAmount(calculatedNumbers.get(GROSS_AMOUNT))
                .report(report)
                .build();
    }

    public List<ReportEntryDTO> generateReportEntries(LocalDate from, LocalDate to) {
        Report report = Report.builder()
                .startDate(from)
                .endDate(to)
                .build();
        Report savedReport = reportRepository.save(report);

        List<Document> documentEntries = documentRepository.findByEffectiveDateBetween(from, to);
        List<ReportEntry> reportEntries = documentEntries.stream()
                .map(document -> documentEntryToReportEntry(document, savedReport))
                .collect(Collectors.toList());
        List<ReportEntry> savedReportEntries = reportEntryRepository.saveAll(reportEntries);
        return reportEntryMapper.entityToDto(savedReportEntries);
    }

    public List<ReportDTO> getAllReports(){
        return reportMapper.entityToDto(reportRepository.findAll());
    }

    public List<ReportEntryDTO> getReportEntriesByReportId(Long reportId){
        return reportEntryMapper.entityToDto(reportEntryRepository.findAllByReportId(reportId));
    }
}
