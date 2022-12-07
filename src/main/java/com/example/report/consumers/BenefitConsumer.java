package com.example.report.consumers;

import com.example.report.model.*;
import com.example.report.repository.BenefitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@RequiredArgsConstructor
@Component
@Slf4j
public class BenefitConsumer {

    private final BenefitMapper benefitMapper;
    private final BenefitRepository benefitRepository;

    @Bean
    public Consumer<BenefitDTOForMQ> benefitInput() {
        return benefitDTOForMQ -> {
            Benefit benefit = benefitMapper.dtoToEntity(benefitDTOForMQ);
            benefitRepository.save(benefit);
            log.info("MESSAGE RECEIVED: Benefit ({}) added in DB!", benefitDTOForMQ);
        };
    }
}
