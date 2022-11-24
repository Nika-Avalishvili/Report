package com.example.report.service;

import com.example.report.model.Benefit;
import com.example.report.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

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
            calculatedNumbers.put("grossAmount", amount.divide(BigDecimal.valueOf(0.784)));
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
            calculatedNumbers.put("grossAmount", amount.divide(BigDecimal.valueOf(0.8)));
            return calculatedNumbers;
        } else {
            calculatedNumbers.put("netAmount", amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put("pensionsFund", BigDecimal.valueOf(0));
            calculatedNumbers.put("personalIncomeTax", amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put("grossAmount", amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        }

    }


//    public ReportDTO createReport(LocalDate from, LocalDate to) {
//        List<DocumentWithEmployeeDTOAndBenefitDTO> documentEntries = List.of(documentClient.getDocumentEntriesByDates(from, to));
//
//        List<ReportEntry> reportEntries = new ArrayList<>();
//
//        for (int i = 0; i < documentEntries.size(); i++) {
//            Map<String, BigDecimal> calculatedNumbers = calculator(documentEntries.get(i).getAmount(),
//                    documentEntries.get(i).getEmployeeDTO(),
//                    documentEntries.get(i).getBenefitDTO().getBenefitTypeDTO(),
//                    documentEntries.get(i).getBenefitDTO().getCalculationMethodDTO());
//
//            ReportEntry reportEntry = ReportEntry.builder()
//                    .accrualDate(documentEntries.get(i).getEffectiveDate())
//                    .employeeDTO(documentEntries.get(i).getEmployeeDTO())
//                    .benefitDTO(documentEntries.get(i).getBenefitDTO())
//                    .netAmount(calculatedNumbers.get("netAmount"))
//                    .pensionsFund(calculatedNumbers.get("pensionsFund"))
//                    .personalIncomeTax(calculatedNumbers.get("personalIncomeTax"))
//                    .grossAmount(calculatedNumbers.get("grossAmount"))
//                    .build();
//            reportEntries.add(reportEntry);
//        }
//
//        return ReportDTO.builder()
//                .reportEntries(reportEntries)
//                .build();
//
//    }
}
