package com.example.report.service;

import com.example.report.model.Document;
import com.example.report.model.*;
import com.example.report.repository.*;
import com.lowagie.text.Font;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.awt.Color.BLUE;
import static java.awt.Color.WHITE;


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
    private static final String ARIAL = "Arial";

    private final DocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final ReportRepository reportRepository;
    private final ReportEntryRepository reportEntryRepository;
    private final ReportEntryMapper reportEntryMapper;
    private final ReportMapper reportMapper;

    private Map<String, BigDecimal> calculator(BigDecimal amount, Employee employee, Benefit benefit) {
        Map<String, BigDecimal> calculatedNumbers = new HashMap<>();

        boolean isActive = employee.getIsActive();
        boolean isPensionsPayer = employee.getIsPensionsPayer();
        String benefitType = benefit.getBenefitTypeName();
        String calculationMethod = benefit.getCalculationMethodName();

        if (!isActive) {
            calculatedNumbers.put(NET_AMOUNT, BigDecimal.ZERO);
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.ZERO);
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.ZERO);
            calculatedNumbers.put(GROSS_AMOUNT, BigDecimal.ZERO);
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.784)));
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply(BigDecimal.valueOf(0.02)));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.196)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
        } else if (isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, amount.multiply((BigDecimal.valueOf(0.02).divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP))));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.784), RoundingMode.HALF_UP));
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(GROSS)) {
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(0.8)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.ZERO);
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.2)));
            calculatedNumbers.put(GROSS_AMOUNT, amount);
        } else if (!isPensionsPayer && benefitType.equals(ACCRUAL) && calculationMethod.equals(NET)) {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.ZERO);
            calculatedNumbers.put(PERSONAL_INCOME_TAX, amount.multiply(BigDecimal.valueOf(0.25)));
            calculatedNumbers.put(GROSS_AMOUNT, amount.divide(BigDecimal.valueOf(0.8), RoundingMode.HALF_UP));
        } else {
            calculatedNumbers.put(NET_AMOUNT, amount);
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.ZERO);
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.ZERO);
            calculatedNumbers.put(GROSS_AMOUNT, amount);
        }
        return calculatedNumbers;
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

    private void applyCommonSheetStyleForListOfReportsFile(Sheet sheet) {
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.createFreezePane(0, 1);
        sheet.setDisplayGridlines(false);
        sheet.setZoom(130);
    }

    private void applyHeaderCellStyleForListOfReportsFile(Workbook workbook, Sheet sheet) {
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName(ARIAL);
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
    }

    private void applyCommonSheetStyleForPayrollRegisterFile(Sheet sheet) {
        sheet.createFreezePane(0, 1);
        sheet.setDisplayGridlines(false);
        sheet.setZoom(120);
        sheet.groupColumn(4,6);
        sheet.setColumnHidden(0, true);
        sheet.setColumnWidth(1,2200);
        sheet.setColumnWidth(2,2500);
        sheet.setColumnWidth(3,2500);
        sheet.setColumnWidth(4,4500);
        sheet.setColumnWidth(5,4500);
        sheet.setColumnWidth(6,4500);
    }

    private void applyHeaderCellStyleForPayrollRegisterFile(Workbook workbook, Sheet sheet, List<Benefit> accrualsList, List<Benefit> deductionsList) {
        CellStyle headerStyle = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName(ARIAL);
        font.setFontHeightInPoints((short) 10);
        font.setBold(false);
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
        headerCell.setCellValue("Report entry id");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(1);
        headerCell.setCellValue("Employee id");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(2);
        headerCell.setCellValue("First name");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(3);
        headerCell.setCellValue("Last name");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(4);
        headerCell.setCellValue("Department");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(5);
        headerCell.setCellValue("Position");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(6);
        headerCell.setCellValue("Email");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(7);
        headerCell.setCellValue("Net payable amount");
        headerCell.setCellStyle(headerStyle);

        for (int i = 0; i < accrualsList.size(); i++) {
            headerCell = header.createCell(8 + i);
            headerCell.setCellValue(accrualsList.get(i).getName());
            headerCell.setCellStyle(headerStyle);
        }

        headerCell = header.createCell(8 + accrualsList.size());
        headerCell.setCellValue("Total accruals");
        headerCell.setCellStyle(headerStyle);

        for (int i = 0; i < deductionsList.size(); i++) {
            headerCell = header.createCell(9 + accrualsList.size() + i);
            headerCell.setCellValue(deductionsList.get(i).getName());
            headerCell.setCellStyle(headerStyle);
        }

        headerCell = header.createCell(9 + accrualsList.size() + deductionsList.size());
        headerCell.setCellValue("Personal income tax");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(10 + accrualsList.size() + deductionsList.size());
        headerCell.setCellValue("Pensions fund");
        headerCell.setCellStyle(headerStyle);

        headerCell = header.createCell(11 + accrualsList.size() + deductionsList.size());
        headerCell.setCellValue("Total deductions");
        headerCell.setCellStyle(headerStyle);
    }

    private void applyCommonSheetStyleForPaySlipFile(Sheet sheet) {
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.setDisplayGridlines(false);
        sheet.setZoom(120);
        sheet.setColumnWidth(1, 4500);
        sheet.setColumnWidth(2, 3500);
        sheet.setColumnWidth(3, 3500);
    }

    private void applyHeaderCellStyleForPaySlipFile(Workbook workbook, Sheet sheet, Employee employee, Report report) {
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName(ARIAL);
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

        String firstName = employee.getFirstName();
        String lastName = employee.getLastName();
        String department = employee.getDepartment();
        String position = employee.getPositions();

        String startDate = report.getStartDate().toString();
        String endDate = report.getEndDate().toString();

        Row header1 = sheet.createRow(0);
        Cell headerCell = header1.createCell(0);
        headerCell.setCellValue(String.format("PaySlip details [from %s to %s]", startDate, endDate));

        Row header2 = sheet.createRow(1);
        headerCell = header2.createCell(0);
        headerCell.setCellValue(String.format("%s %s, %s, %s", firstName, lastName, department, position));

        Row header3 = sheet.createRow(2);
        headerCell = header3.createCell(0);
        headerCell.setCellValue("Benefit ID");
        headerCell.setCellStyle(headerStyle);

        headerCell = header3.createCell(1);
        headerCell.setCellValue("Benefit Name");
        headerCell.setCellStyle(headerStyle);

        headerCell = header3.createCell(2);
        headerCell.setCellValue("Accruals");
        headerCell.setCellStyle(headerStyle);

        headerCell = header3.createCell(3);
        headerCell.setCellValue("Deductions");
        headerCell.setCellStyle(headerStyle);
    }

    private void applySummarizingCellStyleForPayrollRegisterFile(Workbook workbook, Map<Employee, Map<String, BigDecimal>> amountsPerEmployee, List<Benefit> accrualsList, List<Benefit> deductionsList) {
        CellStyle lastRowStyle = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName(ARIAL);
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setItalic(true);

        lastRowStyle.setFont(font);
        lastRowStyle.setAlignment(HorizontalAlignment.RIGHT);
        lastRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        lastRowStyle.setBorderBottom(BorderStyle.DOUBLE);
        lastRowStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        lastRowStyle.setWrapText(true);

        Row lastRow = workbook.getSheetAt(0).createRow(amountsPerEmployee.size()+1);
        int lastRowNum = amountsPerEmployee.size()+1;

        Cell cellOfLastRow;

        for (int i = 0; i < 7; i++) {
            cellOfLastRow = lastRow.createCell(i);
            cellOfLastRow.setCellStyle(lastRowStyle);
        }

        for (int i = 7; i <= (11 + accrualsList.size() + deductionsList.size()); i++) {
            String column = String.valueOf((char) (i + 65));
            cellOfLastRow = lastRow.createCell(i);
            cellOfLastRow.setCellFormula(String.format("sum(%s2:%s%s)",column, column, lastRowNum));
            cellOfLastRow.setCellStyle(lastRowStyle);
        }
    }

    private void writeReportDataInListOfReportsFileRows(Sheet sheet, Integer rowNumber, Report report, CellStyle numberStyle, CellStyle dateStyle) {
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

    private void writeReportEntryDataInPayrollRegisterFileRows(Workbook workbook, Sheet sheet, Integer rowNumber, ReportEntry reportEntry, Map.Entry<Employee, Map<String, BigDecimal>> mapEntry, List<Benefit> accrualsList, List<Benefit> deductionsList) {
        Employee employee = mapEntry.getKey();
        Map<String, BigDecimal> mapOfAmounts = mapEntry.getValue();

        CellStyle cellNumberStyle = getCellNumberStyle(workbook);
        CellStyle cellStringStyle = getCellStringStyle(workbook);

        Row row = sheet.createRow(rowNumber - 1);

        Cell cell = row.createCell(0);
        cell.setCellValue(reportEntry.getReport().getId());
        cell.setCellStyle(cellNumberStyle);

        cell = row.createCell(1);
        cell.setCellValue(employee.getId());
        cell.setCellStyle(cellNumberStyle);

        cell = row.createCell(2);
        cell.setCellValue(employee.getFirstName());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(3);
        cell.setCellValue(employee.getLastName());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(4);
        cell.setCellValue(employee.getDepartment());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(5);
        cell.setCellValue(employee.getPositions());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(6);
        cell.setCellValue(employee.getEmail());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(7);
        char netFormulaStart = (char) (8 + 65 + accrualsList.size());
        char netFormulaEnd = (char) (8 + 65 + 3 + accrualsList.size() + deductionsList.size());
        cell.setCellFormula(String.format("(%s%s-%s%s)", netFormulaStart, rowNumber, netFormulaEnd, rowNumber));
        cell.setCellStyle(cellNumberStyle);

        for (int i = 0; i < accrualsList.size(); i++) {
            cell = row.createCell(8 + i);
            cell.setCellValue(mapOfAmounts.get(accrualsList.get(i).getName()).doubleValue());
            cell.setCellStyle(cellNumberStyle);
        }

        cell = row.createCell(8 + accrualsList.size());
        char accrualFormulaStart = (char) (8 + 65);
        char accrualFormulaEnd = (char) (8 + 65 + accrualsList.size() - 1);
        cell.setCellFormula(String.format("sum(%s%s:%s%s)", accrualFormulaStart, rowNumber, accrualFormulaEnd, rowNumber));
        cell.setCellStyle(cellNumberStyle);

        for (int i = 0; i < deductionsList.size(); i++) {
            cell = row.createCell(9 + accrualsList.size() + i);
            cell.setCellValue(mapOfAmounts.get(deductionsList.get(i).getName()).doubleValue());
            cell.setCellStyle(cellNumberStyle);
        }

        cell = row.createCell(9 + accrualsList.size() + deductionsList.size());
        cell.setCellValue(mapOfAmounts.get(PERSONAL_INCOME_TAX).doubleValue());
        cell.setCellStyle(cellNumberStyle);

        cell = row.createCell(10 + accrualsList.size() + deductionsList.size());
        cell.setCellValue(mapOfAmounts.get(PENSIONS_FUND).doubleValue());
        cell.setCellStyle(cellNumberStyle);

        cell = row.createCell(11 + accrualsList.size() + deductionsList.size());
        char deductionFormulaStart = (char) (8 + 65 + 1 + accrualsList.size());
        char deductionFormulaEnd = (char) (8 + 65 + 2 + accrualsList.size() + deductionsList.size());
        cell.setCellFormula(String.format("sum(%s%s:%s%s)", deductionFormulaStart, rowNumber, deductionFormulaEnd, rowNumber));
        cell.setCellStyle(cellNumberStyle);
    }

    private void writeReportEntryDataInPaySlipFileRows(Workbook workbook, Sheet sheet, Map<Employee, Map<String, BigDecimal>> amountsPerEmployee, Employee employee, List<Benefit> benefits) {
        Map<String, BigDecimal> mapOfAmounts = amountsPerEmployee.get(employee);

        CellStyle cellNumberStyle = getCellNumberStyle(workbook);
        CellStyle cellStringStyle = getCellStringStyle(workbook);

        CellStyle lastRowStyle = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName(ARIAL);
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setItalic(true);

        lastRowStyle.setFont(font);
        lastRowStyle.setAlignment(HorizontalAlignment.RIGHT);
        lastRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        lastRowStyle.setBorderBottom(BorderStyle.DOUBLE);
        lastRowStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());


        int rowNum = 3;

        for (Benefit benefit : benefits) {
            Row row = sheet.createRow(rowNum);

            Cell cell = row.createCell(0);
            cell.setCellValue(benefit.getId());
            cell.setCellStyle(cellNumberStyle);

            cell = row.createCell(1);
            cell.setCellValue(benefit.getName());
            cell.setCellStyle(cellStringStyle);

            if (benefit.getBenefitTypeName().equals(ACCRUAL)) {
                cell = row.createCell(2);
                cell.setCellValue(mapOfAmounts.get(benefit.getName()).doubleValue());
                cell.setCellStyle(cellNumberStyle);

                cell = row.createCell(3);
                cell.setCellValue("");
                cell.setCellStyle(cellNumberStyle);
            } else {
                cell = row.createCell(2);
                cell.setCellValue("");
                cell.setCellStyle(cellNumberStyle);

                cell = row.createCell(3);
                cell.setCellValue(mapOfAmounts.get(benefit.getName()).doubleValue());
                cell.setCellStyle(cellNumberStyle);
            }
            rowNum += 1;
        }

            Row pensionsFundRow = sheet.createRow(rowNum);
            Cell cell = pensionsFundRow.createCell(0);
            cell.setCellValue("");
            cell.setCellStyle(cellStringStyle);

            cell = pensionsFundRow.createCell(1);
            cell.setCellValue("Pensions Fund");
            cell.setCellStyle(cellStringStyle);

            cell = pensionsFundRow.createCell(2);
            cell.setCellStyle(cellNumberStyle);

            cell = pensionsFundRow.createCell(3);
            cell.setCellValue(mapOfAmounts.get(PENSIONS_FUND).doubleValue());
            cell.setCellStyle(cellNumberStyle);

            rowNum += 1;
            Row personalIncomeTaxRow = sheet.createRow(rowNum);
            cell = personalIncomeTaxRow.createCell(0);
            cell.setCellValue("");
            cell.setCellStyle(cellStringStyle);

            cell = personalIncomeTaxRow.createCell(1);
            cell.setCellValue("Personal Income Tax");
            cell.setCellStyle(cellStringStyle);

            cell = personalIncomeTaxRow.createCell(2);
            cell.setCellStyle(cellNumberStyle);

            cell = personalIncomeTaxRow.createCell(3);
            cell.setCellValue(mapOfAmounts.get(PERSONAL_INCOME_TAX).doubleValue());
            cell.setCellStyle(cellNumberStyle);

            rowNum += 1;

            Row summarizingRow = sheet.createRow(rowNum);
            cell = summarizingRow.createCell(0);
            cell.setCellValue("");
            cell.setCellStyle(lastRowStyle);

            cell = summarizingRow.createCell(1);
            cell.setCellValue("TOTAL");
            cell.setCellStyle(lastRowStyle);

            cell = summarizingRow.createCell(2);
            cell.setCellFormula(String.format("sum(C4:C%s)", (rowNum)));
            cell.setCellStyle(lastRowStyle);

            cell = summarizingRow.createCell(3);
            cell.setCellFormula(String.format("sum(D4:D%s)", (rowNum)));
            cell.setCellStyle(lastRowStyle);

            Row lastRow = sheet.createRow(rowNum+1);
            lastRow.createCell(0);
            cell = lastRow.createCell(1);
            cell.setCellValue("NET Amount");
            lastRow.createCell(2);
            cell = lastRow.createCell(3);
            cell.setCellFormula(String.format("C%s-D%s", (rowNum+1), (rowNum+1)));

    }

    private CellStyle getCellStringStyle(Workbook workbook) {
        CellStyle stringStyle = workbook.createCellStyle();
        stringStyle.setWrapText(false);
        stringStyle.setBorderBottom(BorderStyle.THIN);
        stringStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        return stringStyle;
    }

    private CellStyle getCellNumberStyle(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(format.getFormat("#,###.##"));
        numberStyle.setWrapText(true);
        numberStyle.setBorderBottom(BorderStyle.THIN);
        numberStyle.setDataFormat((short) 1);
        numberStyle.setAlignment(HorizontalAlignment.LEFT);
        numberStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        numberStyle.setAlignment(HorizontalAlignment.RIGHT);
        return numberStyle;
    }

    private CellStyle getCellDateStyle(Workbook workbook) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setWrapText(true);
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setDataFormat((short) 14);
        dateStyle.setBottomBorderColor(HSSFColor.HSSFColorPredefined.TEAL.getIndex());
        return dateStyle;
    }

    private Map<Employee, Map<String, BigDecimal>> createMapOfAmountsForAllEmployee(List<ReportEntry> reportEntries) {
        Map<Employee, Map<String, BigDecimal>> amountsPerEmployee = new HashMap<>();

        for (ReportEntry reportEntry : reportEntries) {
            Employee employee = reportEntry.getEmployee();
            String benefitName = reportEntry.getBenefit().getName();
            BigDecimal amount = reportEntry.getGrossAmount();
            BigDecimal pensionsAmount = reportEntry.getPensionsFund();
            BigDecimal taxAmount = reportEntry.getPersonalIncomeTax();

            if (amountsPerEmployee.containsKey(employee)) {
                Map<String, BigDecimal> benefitsWithAmounts = amountsPerEmployee.get(employee);
                if (benefitsWithAmounts.containsKey(benefitName)) {
                    benefitsWithAmounts.put(benefitName, benefitsWithAmounts.get(benefitName).add(amount));
                    benefitsWithAmounts.put(PENSIONS_FUND, benefitsWithAmounts.get(PENSIONS_FUND).add(pensionsAmount));
                    benefitsWithAmounts.put(PERSONAL_INCOME_TAX, benefitsWithAmounts.get(PERSONAL_INCOME_TAX).add(taxAmount));
                } else {
                    benefitsWithAmounts.put(benefitName, amount);
                    benefitsWithAmounts.put(PENSIONS_FUND, benefitsWithAmounts.get(PENSIONS_FUND).add(pensionsAmount));
                    benefitsWithAmounts.put(PERSONAL_INCOME_TAX, benefitsWithAmounts.get(PERSONAL_INCOME_TAX).add(taxAmount));
                }
            } else {
                Map<String, BigDecimal> benefitsWithAmounts = new HashMap<>();
                benefitsWithAmounts.put(benefitName, amount);
                benefitsWithAmounts.put(PENSIONS_FUND, pensionsAmount);
                benefitsWithAmounts.put(PERSONAL_INCOME_TAX, taxAmount);
                amountsPerEmployee.put(employee, benefitsWithAmounts);
            }
        }
        return amountsPerEmployee;
    }

    private List<Benefit> createListOfUniqueBenefits(List<ReportEntry> reportEntries, String benefitTypeName){
        return reportEntries.stream()
                .map(ReportEntry::getBenefit)
                .filter(benefit -> benefit.getBenefitTypeName().equals(benefitTypeName))
                .distinct()
                .collect(Collectors.toList());
    }

    public Workbook extractReportEntriesByReportId(Long reportId) {
        List<ReportEntry> reportEntries = reportEntryRepository.findAllByReportId(reportId);

        List<Benefit> accrualsList = createListOfUniqueBenefits(reportEntries, ACCRUAL);
        List<Benefit> deductionsList = createListOfUniqueBenefits(reportEntries, DEDUCTION);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Payroll Register");
        applyCommonSheetStyleForPayrollRegisterFile(sheet);
        applyHeaderCellStyleForPayrollRegisterFile(workbook, sheet, accrualsList, deductionsList);

        Map<Employee, Map<String, BigDecimal>> amountsPerEmployee = createMapOfAmountsForAllEmployee(reportEntries);

        int rowNumber = 2;
        for (Map.Entry<Employee, Map<String, BigDecimal>> entry : amountsPerEmployee.entrySet()) {
            ReportEntry reportEntry = reportEntries.get(rowNumber - 2);

            writeReportEntryDataInPayrollRegisterFileRows(workbook, sheet, rowNumber, reportEntry, entry, accrualsList, deductionsList);
            rowNumber += 1;
        }
        applySummarizingCellStyleForPayrollRegisterFile(workbook, amountsPerEmployee, accrualsList, deductionsList);

        return workbook;
    }

    public List<ReportEntryDTO> getReportEntriesByReportId(Long reportId) {
        return reportEntryMapper.entityToDto(reportEntryRepository.findAllByReportId(reportId));
    }

    public Workbook extractAllReports() {
        List<Report> reportList = reportRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("List of reports");
        applyCommonSheetStyleForListOfReportsFile(sheet);

        applyHeaderCellStyleForListOfReportsFile(workbook, sheet);
        CellStyle numberStyle = getCellNumberStyle(workbook);
        CellStyle dateStyle = getCellDateStyle(workbook);

        for (int i = 0; i < reportList.size(); i++) {
            Report report = reportList.get(i);
            writeReportDataInListOfReportsFileRows(sheet, i + 1, report, numberStyle, dateStyle);
        }
        return workbook;
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

    public Workbook extractPaySlip(Long employeeId, Long reportId){
        List<ReportEntry> allByEmployeeIdAndReportId = reportEntryRepository.findAllByEmployeeIdAndReportId(employeeId, reportId);
        Employee employee = allByEmployeeIdAndReportId.get(0).getEmployee();
        Report report = allByEmployeeIdAndReportId.get(0).getReport();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("PaySlip");
        applyCommonSheetStyleForPaySlipFile(sheet);

        applyHeaderCellStyleForPaySlipFile(workbook, sheet, employee, report);

        List<Benefit> benefits = new ArrayList<>();
        benefits.addAll(createListOfUniqueBenefits(allByEmployeeIdAndReportId, ACCRUAL));
        benefits.addAll(createListOfUniqueBenefits(allByEmployeeIdAndReportId, DEDUCTION));

        Map<Employee, Map<String, BigDecimal>> amountsPerEmployee = createMapOfAmountsForAllEmployee(allByEmployeeIdAndReportId);

        writeReportEntryDataInPaySlipFileRows(workbook, sheet, amountsPerEmployee, employee, benefits);

        return workbook;
    }

    private void writeReportEntryDataInPaySlipPDFFileRows(com.lowagie.text.Document document,
                                                          Employee employee,
                                                          Report report,
                                                          Map<Employee, Map<String, BigDecimal>> amountsPerEmployee,
                                                          List<Benefit> benefits
                                                          ){
        Map<String, BigDecimal> amounts = amountsPerEmployee.get(employee);

        document.open();
        Font fontTitle = FontFactory.getFont(ARIAL);
        fontTitle.setSize(14);

        Paragraph title1 = new Paragraph(String.format("PaySlip details [from %s to %s]", report.getStartDate(), report.getEndDate()), fontTitle);
        Paragraph title2 = new Paragraph(String.format("%s %s, %s, %s", employee.getFirstName(), employee.getLastName(), employee.getDepartment(), employee.getPositions()), fontTitle);

        document.add(title1);
        document.add(title2);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[] {2,4,3,3});
        table.setSpacingBefore(5);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BLUE);
        cell.setPadding(5);

        Font font = FontFactory.getFont(ARIAL);
        font.setColor(WHITE);

        cell.setPhrase(new Phrase("Benefit Id", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Benefit Name", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Accruals", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Deductions", font));
        table.addCell(cell);


        BigDecimal totalAccruals = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (Benefit benefit : benefits) {
            table.addCell(String.valueOf(benefit.getId()));
            table.addCell(benefit.getName());

            if (benefit.getBenefitTypeName().equals(ACCRUAL)){
                table.addCell(String.valueOf(amounts.get(benefit.getName())));
                table.addCell("");
                totalAccruals = totalAccruals.add(amounts.get(benefit.getName()));
            } else {
                table.addCell("");
                table.addCell(String.valueOf(amounts.get(benefit.getName())));
                totalDeductions = totalDeductions.add(amounts.get(benefit.getName()));
            }
        }

        totalDeductions = totalDeductions.add(amounts.get(PENSIONS_FUND));
        totalDeductions = totalDeductions.add(amounts.get(PERSONAL_INCOME_TAX));

        table.addCell("");
        table.addCell("Pensions Fund");
        table.addCell("");
        table.addCell(amounts.get(PENSIONS_FUND).toString());

        table.addCell("");
        table.addCell("Personal Income Tax");
        table.addCell("");
        table.addCell(amounts.get(PERSONAL_INCOME_TAX).toString());

        table.addCell("");
        table.addCell("TOTAL");
        table.addCell(String.valueOf(totalAccruals));
        table.addCell(String.valueOf(totalDeductions));

        table.addCell("");
        table.addCell("NET Payable Amount");
        table.addCell("");
        table.addCell(String.valueOf(totalAccruals.subtract(totalDeductions)));

        document.add(table);

    }


    public com.lowagie.text.Document extractPaySlipInPDF(HttpServletResponse response, Long employeeId, Long reportId) throws IOException {

        com.lowagie.text.Document document = new com.lowagie.text.Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        List<ReportEntry> allByEmployeeIdAndReportId = reportEntryRepository.findAllByEmployeeIdAndReportId(employeeId, reportId);
        Employee employee = allByEmployeeIdAndReportId.get(0).getEmployee();
        Report report = allByEmployeeIdAndReportId.get(0).getReport();

        List<Benefit> benefits = new ArrayList<>();
        benefits.addAll(createListOfUniqueBenefits(allByEmployeeIdAndReportId, ACCRUAL));
        benefits.addAll(createListOfUniqueBenefits(allByEmployeeIdAndReportId, DEDUCTION));

        Map<Employee, Map<String, BigDecimal>> amountsPerEmployee = createMapOfAmountsForAllEmployee(allByEmployeeIdAndReportId);

        writeReportEntryDataInPaySlipPDFFileRows(document, employee, report, amountsPerEmployee, benefits);

        return document;
    }

}



