package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String NET_AMOUNT = "netAmount";
    private static final String PENSIONS_FUND = "pensionsFund";
    private static final String PERSONAL_INCOME_TAX = "personalIncomeTax";
    private static final String GROSS_AMOUNT = "grossAmount";
    private static final String ACCRUAL = "Accrual";
    private static final String GROSS = "Gross";
    private static final String NET = "Net";
    private static final String DEDUCTION = "Deduction";

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final ReportRepository reportRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ReportEntryMapper reportEntryMapper;
    private final ReportMapper reportMapper;

    private Map<String, BigDecimal> calculator(BigDecimal amount, Employee employee, Benefit benefit) {
        Map<String, BigDecimal> calculatedNumbers = new HashMap<>();

        Boolean isActive = employee.getIsActive();
        Boolean isPensionsPayer = employee.getIsPensionsPayer();
        String benefitType = benefit.getBenefitTypeName();
        String calculationMethod = benefit.getCalculationMethodName();

        if (!isActive) {
            calculatedNumbers.put(NET_AMOUNT, BigDecimal.valueOf(0));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.valueOf(0));
            calculatedNumbers.put(GROSS_AMOUNT, BigDecimal.valueOf(0));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.784)));
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply(BigDecimal.valueOf(0.02)));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.196)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply((BigDecimal.valueOf(0.02).divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP))));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP));
            return calculatedNumbers;
        } else if (isPensionsPayer && benefitType.equals(DEDUCTION)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply(BigDecimal.valueOf(-0.02)));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(-0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.8)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
            return calculatedNumbers;
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.8), RoundingMode.HALF_UP));
            return calculatedNumbers;
        } else {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(-1)));
            return calculatedNumbers;
        }
    }

    private ReportEntry documentEntryToReportEntry(Document document, Report report) {
        Employee employee = employeeRepository.getReferenceById(document.getEmployeeId());
        Benefit benefit = benefitRepository.getReferenceById(document.getBenefitId());
        Map<String, BigDecimal> calculatedNumbers = calculator(document.getAmount(), employee, benefit);
        return ReportEntry.builder()
                .employee(employee)
                .benefit(benefit)
                .document(document)
                .netAmount(calculatedNumbers.get(NET_AMOUNT))
                .pensionsFund(calculatedNumbers.get(PENSIONS_FUND))
                .personalIncomeTax(calculatedNumbers.get(PERSONAL_INCOME_TAX))
                .grossAmount(calculatedNumbers.get(GROSS_AMOUNT))
                .report(report)
                .build();
    }

    public List<ReportEntryDTO> generateReportEntries(LocalDate from, LocalDate to) {
        Report report = Report.builder()
                .startDate(from)
                .endDate(to)
                .build();
        Report savedReport = reportRepository.save(report);

        List<Document> documentEntries = documentRepository.findByEffectiveDateBetween(from, to);
        List<ReportEntry> reportEntries = documentEntries.stream()
                .map(document -> documentEntryToReportEntry(document, savedReport))
                .collect(Collectors.toList());
        List<ReportEntry> savedReportEntries = reportEntryRepository.saveAll(reportEntries);
        return reportEntryMapper.entityToDto(savedReportEntries);
    }

    public List<ReportDTO> getAllReports() {
        return reportMapper.entityToDto(reportRepository.findAll());
    }

    public List<ReportEntryDTO> getReportEntriesByReportId(Long reportId) {
        return reportEntryMapper.entityToDto(reportEntryRepository.findAllByReportId(reportId));
    }


    private void writeReportDataInExcelRow(Sheet sheet, Integer rowNumber, Report report, CellStyle numberStyle, CellStyle dateStyle){
        Row row = sheet.createRow(rowNumber);

        Cell cell = row.createCell(0);
        cell.setCellValue(report.getId());
        cell.setCellStyle(numberStyle);

        cell = row.createCell(1);
        cell.setCellValue(report.getStartDate());
        cell.setCellStyle(dateStyle);

        cell = row.createCell(2);
        cell.setCellValue(report.getEndDate());
        cell.setCellStyle(dateStyle);
    }

    private void applyCommonSheetStyle(Sheet sheet){
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.createFreezePane(0,1);
        sheet.setDisplayGridlines(false);
        sheet.setZoom(130);
    }

    private CellStyle headerCellStyle(Workbook workbook, Sheet sheet){
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(font);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        headerStyle.setWrapText(true);

        Row header = sheet.createRow(0);
        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Report ID");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Start date");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(2);
        headerCell.setCellValue("End date");
        headerCell.setCellStyle(headerStyle);
        return headerStyle;
    }

    private CellStyle cellNumberStyle(Workbook workbook){
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setWrapText(true);
        numberStyle.setBorderBottom(BorderStyle.THIN);
        numberStyle.setDataFormat((short) 1);
        numberStyle.setAlignment(HorizontalAlignment.LEFT);
        numberStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        return numberStyle;
    }

    private CellStyle cellDateStyle(Workbook workbook){
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setWrapText(true);
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setDataFormat((short) 14);
        dateStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        return dateStyle;
    }

    public List<ReportDTO> extractAllReports() throws IOException {
        List<Report> reportList = reportRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("List of reports");
        applyCommonSheetStyle(sheet);

        CellStyle headerStyle = headerCellStyle(workbook, sheet);
        CellStyle numberStyle = cellNumberStyle(workbook);
        CellStyle dateStyle = cellDateStyle(workbook);

        for (int i = 0; i < reportList.size(); i++) {
            Report report = reportList.get(i);
            writeReportDataInExcelRow(sheet,i+1,report, numberStyle, dateStyle);
        }


        File currDir = new File(".");
        String path = currDir.getAbsolutePath();
        String fileLocation = path.substring(0, path.length() - 1) + "List_of_Reports.xlsx";

        FileOutputStream outputStream = new FileOutputStream(fileLocation);
        workbook.write(outputStream);
        workbook.close();
        return reportMapper.entityToDto(reportList);
    }


}

