package com.example.report.consumers;

import com.example.report.model.Employee;
import com.example.report.model.EmployeeDTO;
import com.example.report.model.EmployeeMapper;
import com.example.report.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@RequiredArgsConstructor
@Component
@Slf4j
public class EmployeeConsumer {

    private final EmployeeMapper employeeMapper;
    private final EmployeeRepository employeeRepository;


    @Bean
    public Consumer<EmployeeDTO> employeeInput() {
        return employeeDTO -> {
            Employee employee = employeeMapper.dtoToEntity(employeeDTO);
            employeeRepository.save(employee);
            log.info("MESSAGE RECEIVED: Employee ({}) added in DB!", employeeDTO);
        };
    }
}
