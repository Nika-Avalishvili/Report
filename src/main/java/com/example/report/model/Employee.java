package com.example.report.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Employee {

    @Id
    private Long id;

    private String firstName;
    private String lastName;
    private String department;
    private String positions;
    private String email;

    private Boolean isActive;
    private Boolean isPensionsPayer;

}
