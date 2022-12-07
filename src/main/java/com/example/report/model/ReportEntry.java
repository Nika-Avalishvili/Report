package com.example.report.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class ReportEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "employeeId")
    private Employee employee;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "benefitId")
    private Benefit benefit;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "documentId")
    private Document document;

    private BigDecimal netAmount;
    private BigDecimal pensionsFund;
    private BigDecimal personalIncomeTax;
    private BigDecimal grossAmount;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "reportId")
    private Report report;
}
