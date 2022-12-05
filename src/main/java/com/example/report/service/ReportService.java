package com.example.report.service;

import com.example.report.model.*;
import com.example.report.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;
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
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(1)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.valueOf(0));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(1)));
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
            calculatedNumbers.put(NET_AMOUNT, amount.multiply(BigDecimal.valueOf(1)));
            calculatedNumbers.put(PENSIONS_FUND, BigDecimal.valueOf(0));
            calculatedNumbers.put(PERSONAL_INCOME_TAX, BigDecimal.valueOf(0));
            calculatedNumbers.put(GROSS_AMOUNT, amount.multiply(BigDecimal.valueOf(1)));
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
        font.setFontName("Arial");
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

    private void applySummarizingCellStyleForPayrollRegisterFile(Workbook workbook, Map<Employee, Map<String, BigDecimal>> amountsPerEmployee, List<Benefit> accrualsList, List<Benefit> deductionsList) {
        CellStyle lastRowStyle = workbook.createCellStyle();

        XSSFFont font = ((XSSFWorkbook) workbook).createFont();
        font.setFontName("Arial");
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

    private void writeReportEntryDataInPayrollRegisterFileRows(Sheet sheet, Integer rowNumber, ReportEntry reportEntry, Employee key, Map<String, BigDecimal> value, List<Benefit> accrualsList, List<Benefit> deductionsList, CellStyle numberStyle, CellStyle cellStringStyle) {
        Row row = sheet.createRow(rowNumber - 1);

        Cell cell = row.createCell(0);
        cell.setCellValue(reportEntry.getReport().getId());
        cell.setCellStyle(numberStyle);

        cell = row.createCell(1);
        cell.setCellValue(key.getId());
        cell.setCellStyle(numberStyle);

        cell = row.createCell(2);
        cell.setCellValue(key.getFirstName());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(3);
        cell.setCellValue(key.getLastName());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(4);
        cell.setCellValue(key.getDepartment());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(5);
        cell.setCellValue(key.getPositions());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(6);
        cell.setCellValue(key.getEmail());
        cell.setCellStyle(cellStringStyle);

        cell = row.createCell(7);
        char netFormulaStart = (char) (8 + 65 + accrualsList.size());
        char netFormulaEnd = (char) (8 + 65 + 3 + accrualsList.size() + deductionsList.size());
        cell.setCellFormula(String.format("(%s%s-%s%s)", netFormulaStart, rowNumber, netFormulaEnd, rowNumber));
        cell.setCellStyle(numberStyle);

        for (int i = 0; i < accrualsList.size(); i++) {
            cell = row.createCell(8 + i);
            cell.setCellValue(value.get(accrualsList.get(i).getName()).doubleValue());
            cell.setCellStyle(numberStyle);
        }

        cell = row.createCell(8 + accrualsList.size());
        char accrualFormulaStart = (char) (8 + 65);
        char accrualFormulaEnd = (char) (8 + 65 + accrualsList.size() - 1);
        cell.setCellFormula(String.format("sum(%s%s:%s%s)", accrualFormulaStart, rowNumber, accrualFormulaEnd, rowNumber));
        cell.setCellStyle(numberStyle);

        for (int i = 0; i < deductionsList.size(); i++) {
            cell = row.createCell(9 + accrualsList.size() + i);
            cell.setCellValue(value.get(deductionsList.get(i).getName()).doubleValue());
            cell.setCellStyle(numberStyle);
        }

        cell = row.createCell(9 + accrualsList.size() + deductionsList.size());
        cell.setCellValue(value.get(PERSONAL_INCOME_TAX).doubleValue());
        cell.setCellStyle(numberStyle);

        cell = row.createCell(10 + accrualsList.size() + deductionsList.size());
        cell.setCellValue(value.get(PENSIONS_FUND).doubleValue());
        cell.setCellStyle(numberStyle);

        cell = row.createCell(11 + accrualsList.size() + deductionsList.size());
        char deductionFormulaStart = (char) (8 + 65 + 1 + accrualsList.size());
        char deductionFormulaEnd = (char) (8 + 65 + 2 + accrualsList.size() + deductionsList.size());
        cell.setCellFormula(String.format("sum(%s%s:%s%s)", deductionFormulaStart, rowNumber, deductionFormulaEnd, rowNumber));
        cell.setCellStyle(numberStyle);
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
                if (amountsPerEmployee.get(employee).containsKey(benefitName)) {
                    amountsPerEmployee.get(employee).put(benefitName, amountsPerEmployee.get(employee).get(benefitName).add(amount));
                    amountsPerEmployee.get(employee).put(PENSIONS_FUND, amountsPerEmployee.get(employee).get(PENSIONS_FUND).add(pensionsAmount));
                    amountsPerEmployee.get(employee).put(PERSONAL_INCOME_TAX, amountsPerEmployee.get(employee).get(PERSONAL_INCOME_TAX).add(taxAmount));
                } else {
                    amountsPerEmployee.get(employee).put(benefitName, amount);
                    amountsPerEmployee.get(employee).put(PENSIONS_FUND, amountsPerEmployee.get(employee).get(PENSIONS_FUND).add(pensionsAmount));
                    amountsPerEmployee.get(employee).put(PERSONAL_INCOME_TAX, amountsPerEmployee.get(employee).get(PERSONAL_INCOME_TAX).add(taxAmount));
                }
            } else {
                Map<String, BigDecimal> values = new HashMap<>();
                amountsPerEmployee.put(employee, values);
                amountsPerEmployee.get(employee).put(benefitName, amount);
                amountsPerEmployee.get(employee).put(PENSIONS_FUND, pensionsAmount);
                amountsPerEmployee.get(employee).put(PERSONAL_INCOME_TAX, taxAmount);
            }
        }
        return amountsPerEmployee;
    }

    public Workbook extractReportEntriesByReportId(Long reportId) {
        List<ReportEntry> reportEntries = reportEntryRepository.findAllByReportId(reportId);

        Set<Benefit> accruals = reportEntries.stream().map(ReportEntry::getBenefit).filter(benefit -> benefit.getBenefitTypeName().equals(ACCRUAL)).collect(Collectors.toSet());
        Set<Benefit> deductions = reportEntries.stream().map(ReportEntry::getBenefit).filter(benefit -> benefit.getBenefitTypeName().equals(DEDUCTION)).collect(Collectors.toSet());
        List<Benefit> accrualsList = new ArrayList<>(accruals);
        List<Benefit> deductionsList = new ArrayList<>(deductions);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Payroll Register");
        applyCommonSheetStyleForPayrollRegisterFile(sheet);
        applyHeaderCellStyleForPayrollRegisterFile(workbook, sheet, accrualsList, deductionsList);


        Map<Employee, Map<String, BigDecimal>> amountsPerEmployee = createMapOfAmountsForAllEmployee(reportEntries);

        int rowNumber = 2;
        for (Map.Entry<Employee, Map<String, BigDecimal>> entry : amountsPerEmployee.entrySet()) {
            Employee employee = entry.getKey();
            Map<String, BigDecimal> amounts = entry.getValue();
            ReportEntry reportEntry = reportEntries.get(rowNumber - 2);
            CellStyle cellNumberStyle = getCellNumberStyle(workbook);
            CellStyle cellStringStyle = getCellStringStyle(workbook);

            writeReportEntryDataInPayrollRegisterFileRows(sheet, rowNumber, reportEntry, employee, amounts, accrualsList, deductionsList, cellNumberStyle, cellStringStyle);
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

}



