package com.example.report.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BenefitMapper {

    public BenefitDTOForRabbitMQ entityToDto(Benefit benefit){
        return BenefitDTOForRabbitMQ.builder()
                .id(benefit.getId())
                .name(benefit.getName())
                .benefitTypeName(benefit.getBenefitTypeName())
                .calculationMethodName(benefit.getCalculationMethodName())
                .build();
    }

    public List<BenefitDTOForRabbitMQ> entityToDto(List<Benefit> benefits){
        return benefits.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public Benefit dtoToEntity(BenefitDTOForRabbitMQ benefitDTOForRabbitMQ){
        return Benefit.builder()
                .id(benefitDTOForRabbitMQ.getId())
                .name(benefitDTOForRabbitMQ.getName())
                .benefitTypeName(benefitDTOForRabbitMQ.getBenefitTypeName())
                .calculationMethodName(benefitDTOForRabbitMQ.getCalculationMethodName())
                .build();
    }

    public List<Benefit> dtoToEntity(List<BenefitDTOForRabbitMQ> benefitDTOForRabbitMQS){
        return benefitDTOForRabbitMQS.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }
}
