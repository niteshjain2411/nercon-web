package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.RegistrationData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExcelService {
    private static final String EXCEL_FILE_PATH = "registrations/NERCON_2026_Registrations.xlsx";
    private static final String BACKUP_FOLDER = "registrations";
    private static final String[] HEADERS = {
        "S.No", "Delegate ID", "Full Name", "Email", "Phone", "Gender",
        "Institute", "City", "State", "Medical Council", "Registration No.",
        "Workshops", "Accompanying Persons", "Total Amount", "Transaction ID",
        "Transaction Date", "Submission Time", "Designation"
    };

    public ExcelService() {
        // Create backup folder if it doesn't exist
        try {
            Files.createDirectories(Paths.get(BACKUP_FOLDER));
        } catch (IOException e) {
            System.err.println("Error creating backup folder: " + e.getMessage());
        }
    }

    /**
     * Save registration data to Excel file
     */
    public synchronized boolean saveRegistrationToExcel(RegistrationData registration) {
        try {
            Workbook workbook;
            Sheet sheet;
            int rowCount;

            File file = new File(EXCEL_FILE_PATH);

            // Check if file exists, if yes open it, otherwise create new
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                ensureHeaderRow(sheet);
                rowCount = sheet.getPhysicalNumberOfRows();
                fis.close();
            } else {
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Registrations");
                ensureHeaderRow(sheet);
                rowCount = 1;
            }

            // Add data row
            Row row = sheet.createRow(rowCount);
            fillRegistrationRow(row, registration, rowCount);

            // Auto-size columns
            for (int i = 0; i < 18; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            System.out.println("Registration saved successfully: " + registration.getDelegateId());
            return true;

        } catch (IOException e) {
            System.err.println("Error saving registration to Excel: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create header row for Excel sheet
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void ensureHeaderRow(Sheet sheet) {
        if (sheet.getRow(0) == null || sheet.getRow(0).getPhysicalNumberOfCells() < HEADERS.length) {
            createHeaderRow(sheet);
        }
    }

    /**
     * Fill registration data into Excel row
     */
    private void fillRegistrationRow(Row row, RegistrationData registration, int rowNumber) {
        CellStyle centerStyle = row.getSheet().getWorkbook().createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // S.No
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(rowNumber);
        cell0.setCellStyle(centerStyle);

        // Delegate ID
        Cell cell1 = row.createCell(1);
        cell1.setCellValue(registration.getDelegateId());
        cell1.setCellStyle(centerStyle);

        // Full Name
        Cell cell2 = row.createCell(2);
        cell2.setCellValue(registration.getFullname());

        // Email
        Cell cell3 = row.createCell(3);
        cell3.setCellValue(registration.getEmail());

        // Phone
        Cell cell4 = row.createCell(4);
        cell4.setCellValue(registration.getPhone());

        // Gender
        Cell cell5 = row.createCell(5);
        cell5.setCellValue(registration.getGender());
        cell5.setCellStyle(centerStyle);

        // Institute
        Cell cell6 = row.createCell(6);
        cell6.setCellValue(registration.getInstitute());

        // City
        Cell cell7 = row.createCell(7);
        cell7.setCellValue(registration.getCity());

        // State
        Cell cell8 = row.createCell(8);
        cell8.setCellValue(registration.getState());

        // Medical Council
        Cell cell9 = row.createCell(9);
        cell9.setCellValue(registration.getMedcouncil());

        // Registration No
        Cell cell10 = row.createCell(10);
        cell10.setCellValue(registration.getRegistration());

        // Workshops (comma-separated)
        Cell cell11 = row.createCell(11);
        String workshops = registration.getWorkshops() != null ?
                String.join(", ", registration.getWorkshops()) : "None";
        cell11.setCellValue(workshops);

        // Accompanying Persons
        Cell cell12 = row.createCell(12);
        cell12.setCellValue(registration.getAccompany());
        cell12.setCellStyle(centerStyle);

        // Total Amount
        Cell cell13 = row.createCell(13);
        cell13.setCellValue(registration.getTotalAmount());
        cell13.setCellStyle(centerStyle);

        // Transaction ID
        Cell cell14 = row.createCell(14);
        cell14.setCellValue(registration.getTxnid());

        // Transaction Date
        Cell cell15 = row.createCell(15);
        cell15.setCellValue(registration.getTxndate());

        // Submission Time
        Cell cell16 = row.createCell(16);
        cell16.setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Designation
        Cell cell17 = row.createCell(17);
        cell17.setCellValue(registration.getDesignation());
    }

    /**
     * Get all registrations from Excel
     */
    public List<RegistrationData> getAllRegistrations() {
        List<RegistrationData> registrations = new ArrayList<>();
        try {
            File file = new File(EXCEL_FILE_PATH);
            if (!file.exists()) {
                return registrations;
            }

            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    RegistrationData reg = new RegistrationData();
                    reg.setDelegateId(getCellValueAsString(row.getCell(1)));
                    reg.setFullname(getCellValueAsString(row.getCell(2)));
                    reg.setEmail(getCellValueAsString(row.getCell(3)));
                    reg.setPhone(getCellValueAsString(row.getCell(4)));
                    reg.setGender(getCellValueAsString(row.getCell(5)));
                    reg.setInstitute(getCellValueAsString(row.getCell(6)));
                    reg.setCity(getCellValueAsString(row.getCell(7)));
                    reg.setState(getCellValueAsString(row.getCell(8)));
                    reg.setMedcouncil(getCellValueAsString(row.getCell(9)));
                    reg.setRegistration(getCellValueAsString(row.getCell(10)));
                    reg.setAccompany(getCellValueAsString(row.getCell(12)));
                    reg.setTotalAmount(getCellValueAsString(row.getCell(13)));
                    reg.setTxnid(getCellValueAsString(row.getCell(14)));
                    reg.setTxndate(getCellValueAsString(row.getCell(15)));
                    reg.setDesignation(getCellValueAsString(row.getCell(17)));

                    registrations.add(reg);
                }
            }

            fis.close();
            workbook.close();
        } catch (IOException e) {
            System.err.println("Error reading registrations from Excel: " + e.getMessage());
        }

        return registrations;
    }

    /**
     * Helper method to get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}

