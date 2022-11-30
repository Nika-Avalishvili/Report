package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

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
            calculatedNumbers.put("netAmount", BigDecimal.valueOf(0));
            calculatedNumbers.put("pensionsFund", BigDecimal.valueOf(0));
            calculatedNumbers.put("personalIncomeTax", BigDecimal.valueOf(0));
            calculatedNumbers.put("grossAmount", BigDecimal.valueOf(0));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals("Accrual") && calculationMethod.equals("Gross")) {
            calculatedNumbers.put("netAmount", amount.multiply(BigDecimal.valueOf(0.784)));
            calculatedNumbers.put("pensionsFund", amount.multiply(BigDecimal.valueOf(0.02)));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.98*0.2)));
            calculatedNumbers.put("grossAmount", amount);
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals("Accrual") && calculationMethod.equals("Net")) {
            calculatedNumbers.put("netAmount", amount);
            calculatedNumbers.put("pensionsFund", amount.multiply(BigDecimal.valueOf(0.02/0.784)));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put("grossAmount", amount.multiply(BigDecimal.valueOf(1/0.784)));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals("Deduction")) {
            calculatedNumbers.put("netAmount", amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put("pensionsFund", amount.multiply(BigDecimal.valueOf(-0.02)));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(-0.2)));
            calculatedNumbers.put("grossAmount", amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals("Accrual") && calculationMethod.equals("Gross")) {
            calculatedNumbers.put("netAmount", amount.multiply(BigDecimal.valueOf(0.8)));
            calculatedNumbers.put("pensionsFund", BigDecimal.valueOf(0));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put("grossAmount", amount);
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals("Accrual") && calculationMethod.equals("Net")) {
            calculatedNumbers.put("netAmount", amount);
            calculatedNumbers.put("pensionsFund", BigDecimal.valueOf(0));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put("grossAmount", amount.multiply(BigDecimal.valueOf(1/0.8)));
            return calculatedNumbers;
        } else {
            calculatedNumbers.put("netAmount", amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put("pensionsFund", BigDecimal.valueOf(0));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put("grossAmount", amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        }

    }

    public List<ReportEntryDTO> generateReportEntries(LocalDate from, LocalDate to) {
        Report report = Report.builder()
                .startDate(from)
                .endDate(to)
                .build();
        Report savedReport = reportRepository.save(report);

        List<Document> documentEntries = documentRepository.findByEffectiveDateBetween(from, to);
        List<ReportEntry> reportEntries = new ArrayList<>();

        for (int i = 0; i< documentEntries.size(); i++){

            Employee employee = employeeRepository.getReferenceById(documentEntries.get(i).getEmployeeId());
            Benefit benefit = benefitRepository.getReferenceById(documentEntries.get(i).getBenefitId());
            Map<String, BigDecimal> calculatedNumbers = calculator(documentEntries.get(i).getAmount(),
                                                                                                employee,
                                                                                                benefit);

            ReportEntry reportEntry = ReportEntry.builder()
                    .employee(employee)
                    .benefit(benefit)
                    .document(documentEntries.get(i))
                    .netAmount(calculatedNumbers.get("netAmount"))
                    .pensionsFund(calculatedNumbers.get("pensionsFund"))
                    .personalIncomeTax(calculatedNumbers.get("personalIncomeTax"))
                    .grossAmount(calculatedNumbers.get("grossAmount"))
                    .report(savedReport)
                    .build();

            reportEntryRepository.save(reportEntry);
            reportEntries.add(reportEntry);
        }
        return reportEntryMapper.entityToDto(reportEntries);
    }

    public List<ReportDTO> getAllReports(){
        return reportMapper.entityToDto(reportRepository.findAll());
    }

    public List<ReportEntryDTO> getReportEntriesByReportId(Long reportId){
        return reportEntryMapper.entityToDto(reportEntryRepository.findAllByReportId(reportId));
    }
}
