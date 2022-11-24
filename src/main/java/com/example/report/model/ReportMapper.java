package com.example.report.model;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReportMapper {

    public ReportDTO entityToDto(Report report){
        return ReportDTO.builder()
                .id(report.getId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .build();
    }

    public List<ReportDTO> entityToDto(List<Report> reports){
        return reports.stream().map(this::entityToDto).collect(Collectors.toList());
    }

    public Report dtoToEntity(ReportDTO reportDTO){
        return Report.builder()
                .id(reportDTO.getId())
                .startDate(reportDTO.getStartDate())
                .endDate(reportDTO.getEndDate())
                .build();
    }

    public List<Report> dtoToEntity(List<ReportDTO> reportDTOs){
        return reportDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
    }

}
