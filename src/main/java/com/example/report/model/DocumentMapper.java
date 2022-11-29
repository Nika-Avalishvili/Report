package com.example.report.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DocumentMapper {

    public DocumentDTO entityToDto(Document document){
        return DocumentDTO.builder()
                .id(document.getId())
                .effectiveDate(document.getEffectiveDate())
                .uploadDate(document.getUploadDate())
                .employeeId(document.getEmployeeId())
                .benefitId(document.getBenefitId())
                .amount(document.getAmount())
                .build();
    }

    public List<DocumentDTO> entityToDto(List<Document> documents){
        return documents.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public Document dtoToEntity(DocumentDTO documentDTO){
        return Document.builder()
                .id(documentDTO.getId())
                .effectiveDate(documentDTO.getEffectiveDate())
                .uploadDate(documentDTO.getUploadDate())
                .employeeId(documentDTO.getEmployeeId())
                .benefitId(documentDTO.getBenefitId())
                .amount(documentDTO.getAmount())
                .build();
    }

    public List<Document> dtoToEntity(List<DocumentDTO> documentDTOS){
        return documentDTOS.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }

}
