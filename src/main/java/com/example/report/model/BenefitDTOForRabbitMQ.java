package com.example.report.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BenefitDTOForRabbitMQ {
    private Long id;
    private String name;
    private String benefitTypeName;
    private String calculationMethodName;
}
