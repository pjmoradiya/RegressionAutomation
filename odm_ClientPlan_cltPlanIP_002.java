package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class odm_ClientPlan_cltPlanIP implements JsonProcessor {
    
    private String clientCode;
    private String planId;

    public odm_ClientPlan_cltPlanIP(String clientCode, String planId) {
        this.clientCode = clientCode;
        this.planId = planId;
    }

    @Override
    public void process(Workbook workbook, String sheetName, JSONArray jsonArray, JSONObject jsonResponse) {
        Sheet sheet = workbook.createSheet("Client Plan - " + sheetName);

        // Define the headers upfront
        Map<String, String> headerMap = new LinkedHashMap<>();
        headerMap.put("cltCd", "Client Code");
        headerMap.put("planId", "Client Plan ID");
        headerMap.put("planNm", "Plan Description \r\n Required");
        headerMap.put("prcsgTyp", "Processing Type");
        headerMap.put("copyPlanId", "Copy Client Plan ID \r\n Conditionally Required");
        headerMap.put("autoPrdtnCd", "Auto to Production");
        headerMap.put("rtlCvrgCd", "Retail Covered? \r\n Required");

        // Create main and sub-header rows
        Row mainHeaderRow = sheet.createRow(0);
        mainHeaderRow.setHeightInPoints(40);
        Row subHeaderRow = sheet.createRow(1);
        subHeaderRow.setHeightInPoints(40);

        // Create header styles
        CellStyle lightTanColor = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(196, 189, 151), null));
        CellStyle lightGreyColor = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(166, 166, 166), null));

        // Define main header ranges (example)
        MainHeaderRange[] mainHeaderRanges = {
            new MainHeaderRange(0, 3, "Client Plan - Use this tab to specify high level Plan information...", lightTanColor),
            new MainHeaderRange(4, 6, "", lightTanColor)
        };

        // Create subheader row (just headers)
        int columnIndex = 0;
        for (String header : headerMap.values()) {
            Cell cell = subHeaderRow.createCell(columnIndex);
            // Default style for all columns - you can apply mainHeaderRanges logic if needed
            cell.setCellStyle(lightTanColor);
            cell.setCellValue(header);
            columnIndex++;
        }

        // Merge main headers
        for (MainHeaderRange range : mainHeaderRanges) {
            Cell mainCell = mainHeaderRow.createCell(range.getStart());
            mainCell.setCellValue(range.getHeader());
            mainCell.setCellStyle(range.getStyle());
            if (range.getStart() != range.getEnd()) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, range.getStart(), range.getEnd()));
            }
        }

        // Now handle data rows
        // If jsonArray is null or empty, we do NOT add any data rows.
        if (jsonArray == null || jsonArray.length() == 0) {
            // No data rows to add, just headers. End here.
        } else {
            // We have data, add rows
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                Row row = sheet.createRow(i + 2); // Data rows start from row 2
                columnIndex = 0;
                for (String key : headerMap.keySet()) {
                    Cell cell = row.createCell(columnIndex++);
                    if (key.equals("cltCd")) {
                        cell.setCellValue(clientCode); 
                    } else if (key.equals("planId")) {
                        cell.setCellValue(planId); 
                    } else {
                        cell.setCellValue(item.optString(key, ""));
                    }
                }
            }
        }

        // Resize all columns to fit the content
        for (int i = 0; i < headerMap.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook, XSSFColor color) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
}
