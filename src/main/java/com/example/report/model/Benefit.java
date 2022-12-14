package com.example.report.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Benefit {
    @Id
    private Long id;
    private String name;
    private String benefitTypeName;
    private String calculationMethodName;
}
