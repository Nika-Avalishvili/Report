package com.example.report.rabbitMQ;

import com.example.report.model.Document;
import com.example.report.model.DocumentDTO;
import com.example.report.model.DocumentMapper;
import com.example.report.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@RequiredArgsConstructor
@Component
@Slf4j
public class DocumentConsumer {

    private final DocumentMapper documentMapper;
    private final DocumentRepository documentRepository;

    @Bean
    public Consumer<DocumentDTO> documentInput() {
        return documentDTO -> {
            Document document = documentMapper.dtoToEntity(documentDTO);
            documentRepository.save(document);
            log.info("MESSAGE RECEIVED: Document ({}) added in DB!", documentDTO);
        };
    }
}
