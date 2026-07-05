package com.travelease.backend.busbooking.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility for exporting report data to CSV and Excel formats.
 */
@Component
public class ReportExportUtil {

    /**
     * Generate CSV content from report data.
     */
    public String generateCsv(String reportName, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        StringBuilder csv = new StringBuilder();

        // Header
        List<String> headers = new java.util.ArrayList<>(data.get(0).keySet());
        csv.append(String.join(",", headers)).append("\n");

        // Data rows
        for (Map<String, Object> row : data) {
            List<String> values = new java.util.ArrayList<>();
            for (String header : headers) {
                Object value = row.get(header);
                String strValue = value != null ? escapeCsvField(value.toString()) : "";
                values.add(strValue);
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString();
    }

    /**
     * Generate Excel workbook bytes from report data.
     */
    public byte[] generateExcel(String reportName, List<Map<String, Object>> data) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(reportName);

            if (data == null || data.isEmpty()) {
                sheet.createRow(0).createCell(0).setCellValue("No data available");
                return toByteArray(workbook);
            }

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Headers
            List<String> headers = new java.util.ArrayList<>(data.get(0).keySet());
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Map<String, Object> row : data) {
                Row dataRow = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String header : headers) {
                    Cell cell = dataRow.createCell(colNum++);
                    Object value = row.get(header);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            return toByteArray(workbook);
        }
    }

    private byte[] toByteArray(Workbook workbook) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        return bos.toByteArray();
    }

    private String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
