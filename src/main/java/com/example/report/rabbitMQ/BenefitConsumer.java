package com.example.report.rabbitMQ;

import com.example.report.model.*;
import com.example.report.repository.BenefitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@RequiredArgsConstructor
@Component
public class BenefitConsumer {

    private final BenefitMapper benefitMapper;
    private final BenefitRepository benefitRepository;


    @Bean
    public Consumer<BenefitDTOForRabbitMQ> benefitInput() {
        return benefitDTOForRabbitMQ -> {
            Benefit benefit = benefitMapper.dtoToEntity(benefitDTOForRabbitMQ);
            benefitRepository.save(benefit);
            System.out.println("MESSAGE RECEIVED: Benefit added in DB!");
        };
    }
}
