package com.JSONtoExcelApplication;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class ExcelFileComparator {

    public interface ProgressCallback {
        void log(String message);
    }

    // Define a map for tab-specific key columns
    private static final Map<String, List<Integer>> tabKeyColumnsMap = new HashMap<>();
    static {
        tabKeyColumnsMap.put("Accum Override Copay", Arrays.asList(1, 3, 7));
        tabKeyColumnsMap.put("Program", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("drugCvrgIPLst - Program", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("Other Patient Pay", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("otherPtntPayIPLst - OPP", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("XREF Profile Fast Pass", Arrays.asList(1, 8));
        tabKeyColumnsMap.put("xrefFastPassLst - XREF Profile ", Arrays.asList(1, 8));
        tabKeyColumnsMap.put("Copay Modifier", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("Accum Part B OOP", Arrays.asList(1, 7, 9));
        tabKeyColumnsMap.put("Bypass Accum", Arrays.asList(1, 2));
        tabKeyColumnsMap.put("cltPlanIP", Arrays.asList(1));
        tabKeyColumnsMap.put("Client Plan", Arrays.asList(1));
        // Add additional mappings as needed
    }

    private static final double SIMILARITY_THRESHOLD = 0.7; // Adjust as needed

    public static void compareExcelFiles(String sourceDirPath, String generatedDirPath, String outputDirPath,
                                         ProgressCallback callback) throws IOException {
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            callback.log("Comparison Output Path Created at: " + outputDirPath);
        }

        Path sourceDirectory = Paths.get(sourceDirPath);
        if (!Files.exists(sourceDirectory)) {
            Files.createDirectories(sourceDirectory);
            callback.log("Comparison Source Path Created at: " + sourceDirPath);
        }

        File sourceDir = new File(sourceDirPath);
        File generatedDir = new File(generatedDirPath);

        if (!sourceDir.isDirectory() || !generatedDir.isDirectory()) {
            throw new IllegalArgumentException("Both paths must be directories.");
        }

        Map<String, File> sourceFiles = new HashMap<>();
        for (File sourceFile : Objects.requireNonNull(sourceDir.listFiles())) {
            if (isValidExcelFile(sourceFile)) {
                sourceFiles.put(sourceFile.getName(), sourceFile);
                callback.log("Valid source file found: " + sourceFile.getName());
            } else {
                callback.log("Invalid source file skipped: " + sourceFile.getName());
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (File generatedFile : Objects.requireNonNull(generatedDir.listFiles())) {
            if (isValidExcelFile(generatedFile)) {
                String generatedFileName = generatedFile.getName();
                File sourceFile = findMatchingSourceFile(sourceFiles, generatedFileName);

                if (sourceFile != null) {
                    callback.log("Queuing comparison for source file: " + sourceFile.getName()
                            + " with generated file: " + generatedFile.getName());
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            compareAndHighlightDifferences(sourceFile, generatedFile,
                                    Paths.get(outputDirPath, "Compared_" + generatedFileName).toString(), callback);
                            callback.log("Comparison completed for: " + generatedFileName);
                        } catch (IOException e) {
                            callback.log("Error processing file: " + generatedFileName + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    }, executor);
                    futures.add(future);
                } else {
                    callback.log("No matching source file found for generated file: " + generatedFile.getName());
                }
            } else {
                callback.log("Invalid generated file skipped: " + generatedFile.getName());
            }
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> {
            callback.log("All tasks completed.");
            executor.shutdown();
        });
    }

    public static void compareSpecificFiles(File sourceFile, File generatedFile, String outputDirPath,
                                            ProgressCallback callback) throws IOException {
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            callback.log("Comparison Output Path Created at: " + outputDirPath);
        }

        String generatedFileName = generatedFile.getName();
        String outputFilePath = Paths.get(outputDirPath, "Compared_" + generatedFileName).toString();

        callback.log("Starting comparison for: " + generatedFileName);
        try (FileInputStream sourceFis = new FileInputStream(sourceFile);
             FileInputStream generatedFis = new FileInputStream(generatedFile);
             Workbook sourceWorkbook = new XSSFWorkbook(sourceFis);
             Workbook generatedWorkbook = new XSSFWorkbook(generatedFis)) {

            callback.log("Opened workbooks for comparison.");

            // Remove the preprocessing calls to avoid exceptions during manual comparison
            // ExcelFilePreprocessor.preprocessWorkbook(sourceWorkbook);
            // ExcelFilePreprocessor.preprocessWorkbook(generatedWorkbook);

            List<String> summaryList = new ArrayList<>();
            CellStyle partialMismatchStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_ORANGE);
            CellStyle missingGeneratedStyle = createCellStyle(generatedWorkbook, IndexedColors.YELLOW);
            CellStyle missingSourceStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_GREEN);

            // Comparison logic remains the same as in compareAndHighlightDifferences method

            for (int sheetIndex = 0; sheetIndex < generatedWorkbook.getNumberOfSheets(); sheetIndex++) {
                Sheet outputSheet = generatedWorkbook.getSheetAt(sheetIndex);
                String sheetName = outputSheet.getSheetName();
                Sheet sourceSheet = sourceWorkbook.getSheet(sheetName);

                callback.log("Comparing sheet: " + sheetName);

                if (sourceSheet == null || outputSheet == null) {
                    callback.log("One of the sheets is null, skipping comparison for this sheet.");
                    continue;
                }

                // Get key columns for the current sheet
                List<Integer> keyColumns = tabKeyColumnsMap.get(sheetName);

                // Collect all source and generated rows
                List<RowData> sourceRows = getAllRowData(sourceSheet, keyColumns);
                List<RowData> generatedRows = getAllRowData(outputSheet, keyColumns);

                // Build maps for fast lookup, mapping keys to lists of RowData
                Map<String, List<RowData>> sourceRowMap = groupRowsByKey(sourceRows);
                Map<String, List<RowData>> generatedRowMap = groupRowsByKey(generatedRows);

                // Match and compare rows
                Set<String> allKeys = new HashSet<>();
                allKeys.addAll(sourceRowMap.keySet());
                allKeys.addAll(generatedRowMap.keySet());

                for (String key : allKeys) {
                    List<RowData> sourceRowList = sourceRowMap.getOrDefault(key, new ArrayList<>());
                    List<RowData> generatedRowList = generatedRowMap.getOrDefault(key, new ArrayList<>());

                    // Now, match rows in sourceRowList and generatedRowList
                    matchAndCompareRows(sourceRowList, generatedRowList, outputSheet, partialMismatchStyle,
                            missingGeneratedStyle, missingSourceStyle, summaryList, callback, sheetName);
                }
            }

            createSummarySheet(generatedWorkbook, summaryList);

            // Write the output workbook
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                generatedWorkbook.write(fos);
            }
            callback.log("Written output workbook for: " + generatedFileName);

        } catch (Exception e) {
            callback.log("Exception during comparison for file: " + generatedFileName + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isValidExcelFile(File file) {
        if (file.getName().startsWith("~$") || !file.getName().endsWith(".xlsx")) {
            return false;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            new XSSFWorkbook(fis).close();
            return true;
        } catch (IOException | NotOfficeXmlFileException e) {
            return false;
        }
    }

    private static File findMatchingSourceFile(Map<String, File> sourceFiles, String generatedFileName) {
        for (String sourceFileName : sourceFiles.keySet()) {
            if (generatedFileName.contains(sourceFileName)) {
                return sourceFiles.get(sourceFileName);
            }
        }
        return null;
    }

    private static void compareAndHighlightDifferences(File sourceFile, File generatedFile, String outputFilePath,
                                                       ProgressCallback callback) throws IOException {
        callback.log("Starting comparison for: " + generatedFile.getName());
        try (FileInputStream sourceFis = new FileInputStream(sourceFile);
             FileInputStream generatedFis = new FileInputStream(generatedFile);
             Workbook sourceWorkbook = new XSSFWorkbook(sourceFis);
             Workbook generatedWorkbook = new XSSFWorkbook(generatedFis)) {

            callback.log("Opened workbooks for comparison.");

            List<String> summaryList = new ArrayList<>();
            CellStyle partialMismatchStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_ORANGE);
            CellStyle missingGeneratedStyle = createCellStyle(generatedWorkbook, IndexedColors.YELLOW);
            CellStyle missingSourceStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_GREEN);

            for (int sheetIndex = 0; sheetIndex < generatedWorkbook.getNumberOfSheets(); sheetIndex++) {
                Sheet outputSheet = generatedWorkbook.getSheetAt(sheetIndex);
                String sheetName = outputSheet.getSheetName();
                Sheet sourceSheet = sourceWorkbook.getSheet(sheetName);

                callback.log("Comparing sheet: " + sheetName);

                if (sourceSheet == null || outputSheet == null) {
                    callback.log("One of the sheets is null, skipping comparison for this sheet.");
                    continue;
                }

                // Get key columns for the current sheet
                List<Integer> keyColumns = tabKeyColumnsMap.get(sheetName);

                // Collect all source and generated rows
                List<RowData> sourceRows = getAllRowData(sourceSheet, keyColumns);
                List<RowData> generatedRows = getAllRowData(outputSheet, keyColumns);

                // Build maps for fast lookup, mapping keys to lists of RowData
                Map<String, List<RowData>> sourceRowMap = groupRowsByKey(sourceRows);
                Map<String, List<RowData>> generatedRowMap = groupRowsByKey(generatedRows);

                // Match and compare rows
                Set<String> allKeys = new HashSet<>();
                allKeys.addAll(sourceRowMap.keySet());
                allKeys.addAll(generatedRowMap.keySet());

                for (String key : allKeys) {
                    List<RowData> sourceRowList = sourceRowMap.getOrDefault(key, new ArrayList<>());
                    List<RowData> generatedRowList = generatedRowMap.getOrDefault(key, new ArrayList<>());

                    // Now, match rows in sourceRowList and generatedRowList
                    matchAndCompareRows(sourceRowList, generatedRowList, outputSheet, partialMismatchStyle,
                            missingGeneratedStyle, missingSourceStyle, summaryList, callback, sheetName);
                }
            }

            createSummarySheet(generatedWorkbook, summaryList);

            // Write the output workbook
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                generatedWorkbook.write(fos);
            }
            callback.log("Written output workbook for: " + generatedFile.getName());

        } catch (Exception e) {
            callback.log("Exception during comparison for file: " + generatedFile.getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Group rows by key, mapping keys to lists of RowData to handle duplicate keys
    private static Map<String, List<RowData>> groupRowsByKey(List<RowData> rowDataList) {
        Map<String, List<RowData>> rowMap = new HashMap<>();
        for (RowData rowData : rowDataList) {
            String key = rowData.getKey();
            if (key == null) {
                continue;
            }
            rowMap.computeIfAbsent(key, k -> new ArrayList<>()).add(rowData);
        }
        return rowMap;
    }

    // Method to match and compare rows with the same key
    private static void matchAndCompareRows(List<RowData> sourceRows, List<RowData> generatedRows, Sheet outputSheet,
                                            CellStyle partialMismatchStyle, CellStyle missingGeneratedStyle, CellStyle missingSourceStyle,
                                            List<String> summaryList, ProgressCallback callback, String sheetName) {
        Set<RowData> matchedSourceRows = new HashSet<>();
        Set<RowData> matchedGeneratedRows = new HashSet<>();

        // For each source row, find the best matching generated row
        for (RowData sourceRowData : sourceRows) {
            double maxSimilarity = -1;
            RowData bestMatch = null;

            for (RowData generatedRowData : generatedRows) {
                if (matchedGeneratedRows.contains(generatedRowData)) {
                    continue;
                }
                double similarity = calculateRowSimilarity(sourceRowData, generatedRowData);
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    bestMatch = generatedRowData;
                }
            }

            if (maxSimilarity >= SIMILARITY_THRESHOLD && bestMatch != null) {
                // Match found
                matchedSourceRows.add(sourceRowData);
                matchedGeneratedRows.add(bestMatch);

                // Compare the matched rows
                compareRows(sourceRowData.getRow(), bestMatch.getRow(), partialMismatchStyle, summaryList, callback,
                        sheetName);
            }
        }

        // Handle unmatched source rows (missing in generated file)
        for (RowData sourceRowData : sourceRows) {
            if (!matchedSourceRows.contains(sourceRowData) && !isRowEmpty(sourceRowData.getRow())) {
                Row newRow = outputSheet.createRow(outputSheet.getLastRowNum() + 1);
                copyRow(sourceRowData.getRow(), newRow, missingGeneratedStyle);
                addComment(newRow.getCell(0), "Row missing in Generated File");
                summaryList.add("Sheet: " + sheetName + ", Source Row: " + (sourceRowData.getRow().getRowNum() + 1)
                        + " missing in Generated File");
                callback.log("Row missing in generated: " + (sourceRowData.getRow().getRowNum() + 1));
            }
        }

        // Handle unmatched generated rows (missing in source file)
        for (RowData generatedRowData : generatedRows) {
            if (!matchedGeneratedRows.contains(generatedRowData) && !isRowEmpty(generatedRowData.getRow())) {
                Row generatedRow = generatedRowData.getRow();
                setRowStyle(generatedRow, missingSourceStyle);
                addComment(generatedRow.getCell(0), "Row missing in Source File");
                summaryList.add("Sheet: " + sheetName + ", Generated Row: " + (generatedRow.getRowNum() + 1)
                        + " missing in Source File");
                callback.log("Row missing in source: " + (generatedRow.getRowNum() + 1));
            }
        }
    }

    // Update getAllRowData method to accept keyColumns
    private static List<RowData> getAllRowData(Sheet sheet, List<Integer> keyColumns) {
        List<RowData> rowDataList = new ArrayList<>();
        for (int rowNum = sheet.getFirstRowNum() + 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null && !isRowEmpty(row)) {
                RowData rowData = new RowData(row, keyColumns);
                rowDataList.add(rowData);
            }
        }
        return rowDataList;
    }

    private static class RowData {
        private Row row;
        private String rowHash;
        private List<String> cellValues;
        private String key; // Key based on key columns

        // Update constructor to accept keyColumns
        public RowData(Row row, List<Integer> keyColumns) {
            this.row = row;
            this.cellValues = new ArrayList<>();
            StringBuilder valueBuilder = new StringBuilder();
            StringBuilder keyBuilder = new StringBuilder();

            for (int i = 0; i < row.getLastCellNum(); i++) {
                String cellValue = getCellValue(row.getCell(i)).trim().toLowerCase();
                cellValues.add(cellValue);
                valueBuilder.append(cellValue).append("|");

                if (keyColumns != null && keyColumns.contains(i)) {
                    keyBuilder.append(cellValue).append("|");
                }
            }
            this.rowHash = generateRowHash(valueBuilder.toString());

            if (keyColumns != null) {
                this.key = keyBuilder.toString();
            } else {
                this.key = null;
            }
        }

        public Row getRow() {
            return row;
        }

        public String getRowHash() {
            return rowHash;
        }

        public List<String> getCellValues() {
            return cellValues;
        }

        public String getKey() {
            return key;
        }
    }

    private static String generateRowHash(String rowData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rowData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to generate hash", e);
        }
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static double calculateRowSimilarity(RowData sourceRowData, RowData generatedRowData) {
        List<String> sourceValues = sourceRowData.getCellValues();
        List<String> generatedValues = generatedRowData.getCellValues();
        double matchingCells = 0;
        double totalCells = Math.max(sourceValues.size(), generatedValues.size());

        for (int i = 0; i < totalCells; i++) {
            String sourceValue = i < sourceValues.size() ? sourceValues.get(i) : "";
            String generatedValue = i < generatedValues.size() ? generatedValues.get(i) : "";

            if (sourceValue.equalsIgnoreCase(generatedValue)) {
                matchingCells++;
            }
        }

        if (totalCells == 0) {
            return 0;
        }

        return matchingCells / totalCells;
    }

    private static void compareRows(Row sourceRow, Row generatedRow, CellStyle partialMismatchStyle,
                                    List<String> summaryList, ProgressCallback callback, String sheetName) {
        int maxCells = Math.max(sourceRow.getLastCellNum(), generatedRow.getLastCellNum());

        for (int cellNum = 0; cellNum < maxCells; cellNum++) {
            Cell sourceCell = sourceRow.getCell(cellNum);
            Cell generatedCell = generatedRow.getCell(cellNum);
            String sourceValue = getCellValue(sourceCell).trim();
            String generatedValue = getCellValue(generatedCell).trim();

            if (!sourceValue.equalsIgnoreCase(generatedValue)) {
                if (generatedCell == null) {
                    generatedCell = generatedRow.createCell(cellNum);
                }
                setCellStyle(generatedCell, partialMismatchStyle);
                addComment(generatedCell,
                        "Mismatch: Source value '" + sourceValue + "', Generated value '" + generatedValue + "'");
                summaryList.add("Sheet: " + sheetName + ", Row: " + (generatedRow.getRowNum() + 1) + ", Column: "
                        + (cellNum + 1) + " mismatch - Expected: " + sourceValue + ", Found: " + generatedValue);
                callback.log("Mismatch found at sheet: " + sheetName + ", Row: " + (generatedRow.getRowNum() + 1)
                        + ", Column: " + (cellNum + 1));
            }
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        try {
            CellType cellType = cell.getCellType();
            if (cellType == CellType.FORMULA) {
                cellType = cell.getCachedFormulaResultType();
            }

            switch (cellType) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        DataFormatter formatter = new DataFormatter();
                        return formatter.formatCellValue(cell);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case BLANK:
                    return "";
                default:
                    return cell.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static void copyRow(Row sourceRow, Row targetRow, CellStyle style) {
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            Cell oldCell = sourceRow.getCell(i);
            Cell newCell = targetRow.createCell(i);
            if (oldCell != null) {
                copyCellValue(oldCell, newCell);
                if (style != null) {
                    newCell.setCellStyle(style);
                } else {
                    copyCellStyle(oldCell, newCell);
                }
            }
        }
    }

    private static void copyCellValue(Cell oldCell, Cell newCell) {
        switch (oldCell.getCellType()) {
            case STRING:
                newCell.setCellValue(oldCell.getStringCellValue());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(oldCell)) {
                    newCell.setCellValue(oldCell.getDateCellValue());
                } else {
                    newCell.setCellValue(oldCell.getNumericCellValue());
                }
                break;
            case BOOLEAN:
                newCell.setCellValue(oldCell.getBooleanCellValue());
                break;
            case FORMULA:
                newCell.setCellFormula(oldCell.getCellFormula());
                break;
            case BLANK:
                newCell.setBlank();
                break;
            default:
                newCell.setCellValue(oldCell.toString());
                break;
        }
    }

    private static void setRowStyle(Row row, CellStyle style) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                cell = row.createCell(i);
            }
            cell.setCellStyle(style);
        }
    }

    private static void setCellStyle(Cell cell, CellStyle style) {
        cell.setCellStyle(style);
    }

    private static void addComment(Cell cell, String commentText) {
        if (cell == null) {
            return; // If the cell is null, do not add any comment
        }
        if (cell.getCellComment() != null) {
            // Append to existing comment
            Comment existingComment = cell.getCellComment();
            String existingText = existingComment.getString().getString();
            existingComment.setString(cell.getSheet().getWorkbook().getCreationHelper()
                    .createRichTextString(existingText + "\n" + commentText));
        } else {
            // Create new comment
            Sheet sheet = cell.getSheet();
            Workbook workbook = sheet.getWorkbook();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            CreationHelper factory = workbook.getCreationHelper();
            ClientAnchor anchor = factory.createClientAnchor();

            Comment comment = drawing.createCellComment(anchor);
            comment.setString(factory.createRichTextString(commentText));
            cell.setCellComment(comment);
        }
    }

    private static CellStyle createCellStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        // Set borders
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private static void copyCellStyle(Cell oldCell, Cell newCell) {
        CellStyle newCellStyle = newCell.getSheet().getWorkbook().createCellStyle();
        newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
        newCell.setCellStyle(newCellStyle);
    }

    private static void createSummarySheet(Workbook workbook, List<String> summaryList) {
        Sheet summarySheet = workbook.createSheet("Summary");
        int rowNum = 0;
        for (String summary : summaryList) {
            Row row = summarySheet.createRow(rowNum++);
            Cell cell = row.createCell(0);
            cell.setCellValue(summary);
        }
        // Move Summary Tab to beginning
        workbook.setSheetOrder(summarySheet.getSheetName(), 0);
    }

    public static void main(String[] args) {
        try {
            String sourceDirPath = "C:/Development/CRDTesting/Compare/SourceFiles/Archive";
            String generatedDirPath = "C:/Development/CRDTesting/ResponseJSONtoExcel/";
            String outputDirPath = "C:/Development/CRDTesting/Compare/ComparisonOutput";

            compareExcelFiles(sourceDirPath, generatedDirPath, outputDirPath, message -> System.out.println(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
