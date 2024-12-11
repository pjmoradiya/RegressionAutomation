package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

public class ExcelGenerator {
    public static void generateExcelFile(JSONObject benefitResponse, Path excelOutputPath, String clientCode, String planId) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Process each object in the benefitResponse
        processJsonObject(workbook, benefitResponse, "cltPlanIPLst", new odm_ClientPlan_cltPlanIP(clientCode, planId), benefitResponse); // Order #1
        processJsonObject(workbook, benefitResponse, "mabIPCrdLst", new odm_AccumBenefitMax_mabIPCrdLst(clientCode, planId), benefitResponse); // Order #2
        processJsonObject(workbook, benefitResponse, "dedtblIPCardLst", new odm_AccumDed_dedtblIPCardLst(clientCode, planId), benefitResponse); // Order #3

        // Process undefined objects/collections
        Iterator<String> keys = benefitResponse.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!isDefinedObject(key)) {
                JSONArray jsonArray = benefitResponse.optJSONArray(key);
                // Since we want tabs created regardless, we don't skip if null
                // We'll let createSheet or processor handle null scenario if needed
                createSheet(workbook, key, jsonArray == null ? new JSONArray() : jsonArray);
            }
        }

        // Write the Excel file to disk
        try (FileOutputStream fileOut = new FileOutputStream(excelOutputPath.toFile())) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    private static void processJsonObject(Workbook workbook, JSONObject benefitResponse, String key, JsonProcessor processor, JSONObject jsonResponse) {
        JSONArray jsonArray = benefitResponse.optJSONArray(key);
        // Call processor even if jsonArray is null
        processor.process(workbook, key, jsonArray, jsonResponse);
    }

    private static boolean isDefinedObject(String key) {
        return "accumIclOopLst".equals(key) ||
               "accumOverrideCopayLst".equals(key) ||
               "acmltnHraIPLst".equals(key);
    }

    private static void createSheet(Workbook workbook, String sheetName, JSONArray jsonArray) {
        Sheet sheet = workbook.createSheet(sheetName);

        // If jsonArray is empty, we do not know the headers.
        // In this scenario, since these are "undefined objects",
        // we don't have a predefined header set. If you want to ensure a tab even without knowing headers,
        // you can create a blank sheet or define some placeholder headers here.
        // For now, if array is empty, we just create a blank sheet with no headers.

        if (jsonArray.length() > 0) {
            // Use the first item to determine headers
            JSONObject firstItem = jsonArray.getJSONObject(0);
            Iterator<String> keys = firstItem.keys();
            int columnIndex = 0;

            Row headerRow = sheet.createRow(0);
            while (keys.hasNext()) {
                String key = keys.next();
                Cell cell = headerRow.createCell(columnIndex++);
                cell.setCellValue(key);
            }

            // Create data rows
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                Row row = sheet.createRow(i + 1);
                columnIndex = 0;
                for (String k : firstItem.keySet()) {
                    Cell cell = row.createCell(columnIndex++);
                    cell.setCellValue(item.optString(k, ""));
                }
            }
        } else {
            // Empty array scenario: Just create the sheet with no headers or data
            // If you prefer some placeholder text, you can add a note here.
        }
    }
}
