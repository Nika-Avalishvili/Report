package com.example.report.model;

import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "documents")
public class Document {

    @Id
    private Long id;

    private LocalDate uploadDate;
    private LocalDate effectiveDate;

    private Long employeeId;
    private Long benefitId;
    private BigDecimal amount;
}
