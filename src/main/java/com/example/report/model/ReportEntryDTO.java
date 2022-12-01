package com.example.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ReportEntryDTO {

    private Long id;

    private EmployeeDTO employeeDTO;
    private BenefitDTOForMQ benefitDTOForMQ;
    private DocumentDTO documentDTO;

    private BigDecimal netAmount;
    private BigDecimal pensionsFund;
    private BigDecimal personalIncomeTax;
    private BigDecimal grossAmount;

    private ReportDTO reportDTO;
}
