package com.example.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DocumentDTO {

    private Long id;

    private LocalDate uploadDate;
    private LocalDate effectiveDate;

    private Long employeeId;
    private Long benefitId;
    private BigDecimal amount;
}
