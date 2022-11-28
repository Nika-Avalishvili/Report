package com.example.report.rabbitMQ;

import com.example.report.model.Document;
import com.example.report.model.DocumentDTO;
import com.example.report.model.DocumentMapper;
import com.example.report.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


@RequiredArgsConstructor
@Component
public class DocumentConsumer {

    private final DocumentMapper documentMapper;
    private final DocumentRepository documentRepository;

    @Bean
    public Consumer<DocumentDTO> documentInput() {
        return documentDTO -> {
            Document document = documentMapper.dtoToEntity(documentDTO);
            documentRepository.save(document);
            System.out.println("MESSAGE RECEIVED: Document added in DB!");
        };
    }
}
