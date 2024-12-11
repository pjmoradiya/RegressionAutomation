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

        // Create data rows
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
        cell.setCellValue("No Data"); 
        // If you know the expected headers for this tab, you can set them here instead.
    }
}
