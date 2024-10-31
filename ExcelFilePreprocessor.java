package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;

import java.util.HashMap;
import java.util.Map;

public class ExcelFilePreprocessor {

    // Method to preprocess the workbook
    public static void preprocessWorkbook(Workbook workbook) {
        // Check if the first sheet is named "Cover"
        Sheet firstSheet = workbook.getSheetAt(0);
        if ("Cover".equalsIgnoreCase(firstSheet.getSheetName())) {
            // Delete the first 4 rows from each sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                deleteFirstNRows(sheet, 4);
            }
            // Rename each tab
            renameSheets(workbook);
        }
    }

    private static void deleteFirstNRows(Sheet sheet, int n) {
        int lastRowNum = sheet.getLastRowNum();
        if (lastRowNum >= n) {
            sheet.shiftRows(n, lastRowNum, -n);
        } else {
            // If there are fewer rows than n, remove all rows
            for (int i = lastRowNum; i >= 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
        }
    }

    private static void renameSheets(Workbook workbook) {
        // Define a mapping from old sheet names to new sheet names
        Map<String, String> sheetNameMapping = new HashMap<>();
        // Add mapping entries: sheetNameMapping.put("OldName", "NewName");
        // You need to fill in the actual mapping based on your requirements

        // Example mapping (replace with your actual mapping)
        sheetNameMapping.put("Cover", "Introduction");
        sheetNameMapping.put("Sheet1", "Data");
        sheetNameMapping.put("Sheet2", "Summary");
        // ... add more mappings as needed

        // Iterate over sheets and rename them
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String oldName = workbook.getSheetName(i);
            String newName = sheetNameMapping.getOrDefault(oldName, oldName);
            workbook.setSheetName(i, newName);
        }
    }
}
