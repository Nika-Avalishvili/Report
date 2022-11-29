package com.example.report.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BenefitMapper {

    public BenefitDTOForMQ entityToDto(Benefit benefit){
        return BenefitDTOForMQ.builder()
                .id(benefit.getId())
                .name(benefit.getName())
                .benefitTypeName(benefit.getBenefitTypeName())
                .calculationMethodName(benefit.getCalculationMethodName())
                .build();
    }

    public List<BenefitDTOForMQ> entityToDto(List<Benefit> benefits){
        return benefits.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public Benefit dtoToEntity(BenefitDTOForMQ benefitDTOForMQ){
        return Benefit.builder()
                .id(benefitDTOForMQ.getId())
                .name(benefitDTOForMQ.getName())
                .benefitTypeName(benefitDTOForMQ.getBenefitTypeName())
                .calculationMethodName(benefitDTOForMQ.getCalculationMethodName())
                .build();
    }

    public List<Benefit> dtoToEntity(List<BenefitDTOForMQ> benefitDTOForMQS){
        return benefitDTOForMQS.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }
}
