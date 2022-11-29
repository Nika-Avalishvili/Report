package com.example.report.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmployeeMapper {

    public EmployeeDTO entityToDto(Employee employee) {
        return EmployeeDTO.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .department(employee.getDepartment())
                .positions(employee.getPositions())
                .email(employee.getEmail())
                .isActive(employee.getIsActive())
                .isPensionsPayer(employee.getIsPensionsPayer())
                .build();
    }

    public List<EmployeeDTO> entityToDto(List<Employee> employees) {
        return employees.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public Employee dtoToEntity(EmployeeDTO employeeDto) {
        return Employee.builder()
                .id(employeeDto.getId())
                .firstName(employeeDto.getFirstName())
                .lastName(employeeDto.getLastName())
                .department(employeeDto.getDepartment())
                .positions(employeeDto.getPositions())
                .email(employeeDto.getEmail())
                .isActive(employeeDto.getIsActive())
                .isPensionsPayer(employeeDto.getIsPensionsPayer())
                .build();
    }

    public List<Employee> dtoToEntity(List<EmployeeDTO> employeeDTOs) {
        return employeeDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }
}
