package com.example.report.model;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportEntryMapper {

    public final EmployeeMapper employeeMapper;
    public final BenefitMapper benefitMapper;
    public final DocumentMapper documentMapper;
    public final ReportMapper reportMapper;
    public ReportEntryDTO entityToDto(ReportEntry reportEntry){
        return ReportEntryDTO.builder()
                .id(reportEntry.getId())
                .employeeDTO(employeeMapper.entityToDto(reportEntry.getEmployee()))
                .benefitDTOForMQ(benefitMapper.entityToDto(reportEntry.getBenefit()))
                .documentDTO(documentMapper.entityToDto(reportEntry.getDocument()))
                .netAmount(reportEntry.getNetAmount())
                .pensionsFund(reportEntry.getPensionsFund())
                .personalIncomeTax(reportEntry.getPersonalIncomeTax())
                .grossAmount(reportEntry.getGrossAmount())
                .reportDTO(reportMapper.entityToDto(reportEntry.getReport()))
                .build();
    }

    public List<ReportEntryDTO> entityToDto(List<ReportEntry> reportEntries){
        return reportEntries.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public ReportEntry dtoToEntity(ReportEntryDTO reportEntryDTO){
        return ReportEntry.builder()
                .id(reportEntryDTO.getId())
                .employee(employeeMapper.dtoToEntity(reportEntryDTO.getEmployeeDTO()))
                .benefit(benefitMapper.dtoToEntity(reportEntryDTO.getBenefitDTOForMQ()))
                .document(documentMapper.dtoToEntity(reportEntryDTO.getDocumentDTO()))
                .netAmount(reportEntryDTO.getNetAmount())
                .pensionsFund(reportEntryDTO.getPensionsFund())
                .personalIncomeTax(reportEntryDTO.getPersonalIncomeTax())
                .grossAmount(reportEntryDTO.getGrossAmount())
                .report(reportMapper.dtoToEntity(reportEntryDTO.getReportDTO()))
                .build();
    }

    public List<ReportEntry> dtoToEntity(List<ReportEntryDTO> reportEntryDTOs){
        return reportEntryDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }
}
