package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExcelGenerator {
    public static void generateExcelFile(JSONObject benefitResponse, Path excelOutputPath, String clientCode, String planId) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Process each object in the benefitResponse
        
        processJsonObject(workbook, benefitResponse, "cltPlanIPLst", new odm_ClientPlan_cltPlanIP(clientCode, planId), benefitResponse);								// Order #1
        processJsonObject(workbook, benefitResponse, "mabIPCrdLst", new odm_AccumBenefitMax_mabIPCrdLst(clientCode, planId), benefitResponse);							// Order #2
        processJsonObject(workbook, benefitResponse, "dedtblIPCardLst", new odm_AccumDed_dedtblIPCardLst(clientCode, planId), benefitResponse);							// Order #3
        processJsonObject(workbook, benefitResponse, "acmltnHraIPLst", new odm_AccumHRA_acmltnHraIPLst(clientCode, planId), benefitResponse);							// Order #4 
        // Process undefined objects/collections
        Iterator<String> keys = benefitResponse.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!isDefinedObject(key)) {
                JSONArray jsonArray = benefitResponse.optJSONArray(key);
                if (jsonArray != null) {
                    createSheet(workbook, key, jsonArray);
                }
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
//        if (jsonArray != null || jsonArray == null) {
            processor.process(workbook, key, jsonArray, jsonResponse);
//        }
    }

    private static boolean isDefinedObject(String key) {
        return 	"accumIclOopLst".equals(key) ||
                "accumOverrideCopayLst".equals(key) || 
                "acmltnHraIPLst".equals(key) ;
    }

    private static void createSheet(Workbook workbook, String sheetName, JSONArray jsonArray) {
        Sheet sheet = workbook.createSheet(sheetName);
        Map<String, Integer> headerMap = new LinkedHashMap<>();

        if (jsonArray.length() > 0) {
            JSONObject firstItem = jsonArray.getJSONObject(0);
            Iterator<String> keys = firstItem.keys();
            int columnIndex = 0;
            while (keys.hasNext()) {
                String key = keys.next();
                headerMap.put(key, columnIndex++);
            }

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                Cell cell = headerRow.createCell(entry.getValue());
                cell.setCellValue(entry.getKey());
            }

            // Create data row
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                Row row = sheet.createRow(i + 1);
                for (String key : headerMap.keySet()) {
                    Cell cell = row.createCell(headerMap.get(key));
                    cell.setCellValue(item.optString(key, ""));
                }
            }
        } else {
        	// No data present, but still create the sheet with headers
            // Here we just create one header cell "No Data"
            Row headerRow = sheet.createRow(0);
            Cell cell = headerRow.createCell(0);
        }
    }
}
