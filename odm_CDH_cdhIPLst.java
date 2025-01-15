package com.JSONtoExcelApplication;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class odm_CDH_cdhIPLst implements JsonProcessor {
    
	private String clientCode;
    private String planId;

    public odm_CDH_cdhIPLst(String clientCode, String planId) {
        this.clientCode = clientCode;
        this.planId = planId;
    }
	
    @Override
    public void process(Workbook workbook, String sheetName, JSONArray jsonArray, JSONObject jsonResponse) {
        Sheet sheet = workbook.createSheet("CDH - " + sheetName);
        Map<String, String> headerMap = new LinkedHashMap<>();

        /**
         * Below all headerMap statements renames the JSON response attributes.
         * It also keeps the order in which those are defined. 
         */

        // Define custom headers and order
        
        headerMap.put("cltCd", "Client Code");												// Order #1
        headerMap.put("planId", "Client Plan ID");											// Order #2
        headerMap.put("cdhCltId", "CDH Client ID \r\n(Required)");							// Order #3
        headerMap.put("cdhCltNm", "CDH Client Name");										// Order #4
        headerMap.put("medItgrId", "Medical Integrator ID \r\n(Required)");					// Order #5
        headerMap.put("medItgrNm", "Medical Integrator Name");								// Order #6
        headerMap.put("fromDt", "From Date");												// Order #7
        headerMap.put("thruDt", "Thru Date");												// Order #8
        headerMap.put("accToIncl", "Accums to Include \r\n(Required)");						// Order #9
        headerMap.put("shrId", "Shared ID");												// Order #10
        headerMap.put("shrIdLen", "Shared ID Length");										// Order #11
        headerMap.put("rmtHra", "Remote HRA");												// Order #12
        headerMap.put("oonPrcsg", "OON Processing");										// Order #13
        headerMap.put("blank1", "");														// Order #14
        headerMap.put("prflVsBasePlan", "Profile v Base plan");								// Order #15
        headerMap.put("carrierId", "Carrier Identifier");									// Order #16
        headerMap.put("cltId1", "Client Identifier 1");										// Order #17
        headerMap.put("cltId2", "Client Identifier 2");										// Order #18

        
            
            // Create header row with background color
            Row mainHeaderRow = sheet.createRow(0);
            mainHeaderRow.setHeightInPoints(40);
            Row subHeaderRow = sheet.createRow(1);
            subHeaderRow.setHeightInPoints(40);
            int columnIndex = 0;

            // Create header styles with custom colors
            CellStyle lightTanColor = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(196, 189, 151), null)); 		
            // Define ranges for main headers with color
            MainHeaderRange[] mainHeaderRanges = {
                new MainHeaderRange(0, 12, "CDH - Use this tab to provide the DBOR CDHIA (Dual Book of Record Consumer Driven Health Integrated Accums) setup for the Plan (F23.2 on the Base Plan).", lightTanColor),
                new MainHeaderRange(13, 13, "", lightTanColor),
                new MainHeaderRange(14, 17, "Profile Type", lightTanColor)
            };
            
            // Create subheader row and set its style
            columnIndex = 0; // Reset the columnIndex before setting subheaders
            for (String header : headerMap.values()) {
                Cell cell = subHeaderRow.createCell(columnIndex);
                
                // Apply the same style as the corresponding main header
                for (MainHeaderRange range : mainHeaderRanges) {
                    if (columnIndex >= range.getStart() && columnIndex <= range.getEnd()) {
                        cell.setCellStyle(range.getStyle());
                        break;
                    }
                }
                cell.setCellValue(header);
                columnIndex++;
            }
            

            for (MainHeaderRange range : mainHeaderRanges) {
                Cell mainCell = mainHeaderRow.createCell(range.getStart());
                mainCell.setCellValue(range.getHeader());
                mainCell.setCellStyle(range.getStyle());
                if (range.getStart() != range.getEnd()) { 
                	sheet.addMergedRegion(new CellRangeAddress(0, 0, range.getStart(), range.getEnd()));
                }
                
            }

            // Create data rows
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                Row row = sheet.createRow(i + 2); // Data rows start from row 2
                columnIndex = 0;
                for (String key : headerMap.keySet()) {
                    Cell cell = row.createCell(columnIndex++);
                    if (key.equals("cltCd")) {
                        cell.setCellValue(clientCode); // Use value from the header
                    } else if (key.equals("planId")) {
                        cell.setCellValue(planId); // Use value from the header
                    } else if (key.equals("")) {
                    	cell.setCellValue(""); // Ensure blank cells are created
                    } else {
                        cell.setCellValue(item.optString(key, ""));
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
