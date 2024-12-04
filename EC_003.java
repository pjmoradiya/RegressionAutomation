package com.JSONtoExcelApplication;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

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

    // Define a map for tab-specific key columns (ensure zero-based indexing)
    private static final Map<String, List<Integer>> tabKeyColumnsMap = new HashMap<>();
    static {
        tabKeyColumnsMap.put("Accum Override Copay", Arrays.asList(1, 3, 7)); // Adjust indices if needed
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

    private static final List<Integer> defaultKeyColumns = Arrays.asList(1); // Default key column index 1

    private static final double SIMILARITY_THRESHOLD = 0.5; // Adjusted as needed

    // Sheet name mapping (from old names to new names)
    private static final Map<String, String> sheetNameMapping = new HashMap<>();
    static {
        // Add mapping entries: sheetNameMapping.put("OldName", "NewName");
        // You need to fill in the actual mapping based on your requirements

        // Example mapping (replace with your actual mapping)
        sheetNameMapping.put("Cover", "Cover");
        sheetNameMapping.put("Accum ICL & MedD OOP", "Accum ICL&MedD - accumIclOopLst");
        sheetNameMapping.put("Accum Override Copay", "AccumOv - accumOverrideCopayLst");
        // ... add more mappings as needed
    }

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

        // Build map of <FileName> to list of source files
        Map<String, List<File>> sourceFilesMap = new HashMap<>();
        for (File sourceFile : Objects.requireNonNull(sourceDir.listFiles())) {
            if (isValidExcelFile(sourceFile)) {
                String fileNamePart = extractFileNamePart(sourceFile.getName());
                if (fileNamePart != null) {
                    sourceFilesMap.computeIfAbsent(fileNamePart, k -> new ArrayList<>()).add(sourceFile);
                    callback.log("Valid source file found: " + sourceFile.getName());
                } else {
                    callback.log("Invalid source file (filename pattern not matched): " + sourceFile.getName());
                }
            } else {
                callback.log("Invalid source file skipped: " + sourceFile.getName());
            }
        }

        // Build map of <FileName> to list of generated files
        Map<String, List<File>> generatedFilesMap = new HashMap<>();
        for (File generatedFile : Objects.requireNonNull(generatedDir.listFiles())) {
            if (isValidExcelFile(generatedFile)) {
                String fileNamePart = extractFileNamePart(generatedFile.getName());
                if (fileNamePart != null) {
                    generatedFilesMap.computeIfAbsent(fileNamePart, k -> new ArrayList<>()).add(generatedFile);
                    callback.log("Valid generated file found: " + generatedFile.getName());
                } else {
                    callback.log("Invalid generated file (filename pattern not matched): " + generatedFile.getName());
                }
            } else {
                callback.log("Invalid generated file skipped: " + generatedFile.getName());
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // For each <FileName> in source files
        for (Map.Entry<String, List<File>> sourceEntry : sourceFilesMap.entrySet()) {
            String fileNamePart = sourceEntry.getKey();
            List<File> sourceFiles = sourceEntry.getValue();

            // Find matching generated files
            List<File> matchingGeneratedFiles = generatedFilesMap.getOrDefault(fileNamePart, Collections.emptyList());

            if (matchingGeneratedFiles.isEmpty()) {
                for (File sourceFile : sourceFiles) {
                    callback.log("No matching generated files found for source file: " + sourceFile.getName());
                }
                continue;
            }

            // For each source file, compare with each matching generated file
            for (File sourceFile : sourceFiles) {
                for (File generatedFile : matchingGeneratedFiles) {
                    String generatedFileName = generatedFile.getName();
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
                }
            }
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.thenRun(() -> {
            callback.log("All tasks completed.");
            executor.shutdown();
        });

        // Wait for all tasks to complete
        try {
            allOf.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // Updated method to handle cases where only the source file is selected
    public static void compareSpecificFiles(File sourceFile, File generatedFile, String generatedDirPath, String outputDirPath,
                                            ProgressCallback callback) throws IOException {
        if (generatedFile != null) {
            // Compare the specific files
            compareAndHighlightDifferences(sourceFile, generatedFile,
                    Paths.get(outputDirPath, "Compared_" + generatedFile.getName()).toString(), callback);
        } else {
            // Find matching generated files based on source file's <FileName>
            String fileNamePart = extractFileNamePart(sourceFile.getName());
            if (fileNamePart == null) {
                callback.log("Cannot extract <FileName> from source file: " + sourceFile.getName());
                return;
            }

            File generatedDir = new File(generatedDirPath);
            if (!generatedDir.isDirectory()) {
                throw new IllegalArgumentException("Generated files path must be a directory.");
            }

            // Build list of matching generated files
            List<File> matchingGeneratedFiles = new ArrayList<>();
            for (File generatedFileCandidate : Objects.requireNonNull(generatedDir.listFiles())) {
                if (isValidExcelFile(generatedFileCandidate)) {
                    String generatedFileNamePart = extractFileNamePart(generatedFileCandidate.getName());
                    if (fileNamePart.equals(generatedFileNamePart)) {
                        matchingGeneratedFiles.add(generatedFileCandidate);
                    }
                }
            }

            if (matchingGeneratedFiles.isEmpty()) {
                callback.log("No matching generated files found for source file: " + sourceFile.getName());
                return;
            }

            // Prepare executor for parallel processing
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (File generatedFileMatch : matchingGeneratedFiles) {
                String generatedFileName = generatedFileMatch.getName();
                callback.log("Queuing comparison for source file: " + sourceFile.getName()
                        + " with generated file: " + generatedFileMatch.getName());
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        compareAndHighlightDifferences(sourceFile, generatedFileMatch,
                                Paths.get(outputDirPath, "Compared_" + generatedFileName).toString(), callback);
                        callback.log("Comparison completed for: " + generatedFileName);
                    } catch (IOException e) {
                        callback.log("Error processing file: " + generatedFileName + " - " + e.getMessage());
                        e.printStackTrace();
                    }
                }, executor);
                futures.add(future);
            }

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.thenRun(() -> {
                callback.log("All tasks completed.");
                executor.shutdown();
            });

            // Wait for all tasks to complete
            try {
                allOf.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to extract the <FileName> part from the filename
    private static String extractFileNamePart(String fileName) {
        // Example filename: 12345_PROD_CRD_TestFile_v001.xlsx
        // We want to extract 'TestFile'

        // First, remove the extension
        if (fileName.endsWith(".xlsx")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }

        // Find indices of "_CRD_" and "_v"
        int crdIndex = fileName.indexOf("_CRD_");
        int vIndex = fileName.lastIndexOf("_v");

        if (crdIndex >= 0 && vIndex > crdIndex) {
            return fileName.substring(crdIndex + 5, vIndex);
        }

        // If the pattern does not match, return the filename without extension
        return fileName;
    }

    // Rest of the methods remain unchanged
    private static void compareAndHighlightDifferences(File sourceFile, File generatedFile, String outputFilePath,
                                                       ProgressCallback callback) throws IOException {
        callback.log("Starting comparison for: " + generatedFile.getName());
        try (FileInputStream sourceFis = new FileInputStream(sourceFile);
             FileInputStream generatedFis = new FileInputStream(generatedFile);
             Workbook sourceWorkbook = new XSSFWorkbook(sourceFis);
             Workbook generatedWorkbook = new XSSFWorkbook(generatedFis)) {

            callback.log("Opened workbooks for comparison.");

            // Rename sheets based on mapping
            renameSheets(sourceWorkbook);
            renameSheets(generatedWorkbook);

            // Read sheet name mappings
            Map<String, String> sheetNameMap = getSheetNameMap(sourceWorkbook, generatedWorkbook);

            List<String> summaryList = new ArrayList<>();
            CellStyle partialMismatchStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_ORANGE);
            CellStyle missingGeneratedStyle = createCellStyle(generatedWorkbook, IndexedColors.YELLOW);
            CellStyle missingSourceStyle = createCellStyle(generatedWorkbook, IndexedColors.LIGHT_GREEN);

            // Get list of sheets to compare based on mappings
            Set<String> sheetNamesToCompare = sheetNameMap.keySet();

            for (String sheetName : sheetNamesToCompare) {
                String sourceSheetName = sheetName;
                String generatedSheetName = sheetNameMap.get(sheetName);

                Sheet sourceSheet = sourceWorkbook.getSheet(sourceSheetName);
                Sheet generatedSheet = generatedWorkbook.getSheet(generatedSheetName);

                callback.log("Comparing sheet: " + sheetName);

                if (sourceSheet == null || generatedSheet == null) {
                    callback.log("One of the sheets is null, skipping comparison for this sheet.");
                    continue;
                }

                // Get key columns for the current sheet
                List<Integer> keyColumns = getKeyColumnsForSheet(sheetName);

                // Collect all source and generated rows
                List<RowData> sourceRows = getAllRowData(sourceSheet, keyColumns);
                List<RowData> generatedRows = getAllRowData(generatedSheet, keyColumns);

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
                    matchAndCompareRows(sourceRowList, generatedRowList, generatedSheet, partialMismatchStyle,
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

    // Rename sheets based on the mapping
    private static void renameSheets(Workbook workbook) {
        // Iterate over sheets and rename them
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String oldName = workbook.getSheetName(i);
            String newName = sheetNameMapping.getOrDefault(oldName, oldName);
            workbook.setSheetName(i, newName);
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

    private static Map<String, String> getSheetNameMap(Workbook sourceWorkbook, Workbook generatedWorkbook) {
        Map<String, String> sheetNameMap = new HashMap<>();

        // Build a set of source sheet names
        Set<String> sourceSheetNames = new HashSet<>();
        for (int i = 0; i < sourceWorkbook.getNumberOfSheets(); i++) {
            sourceSheetNames.add(sourceWorkbook.getSheetName(i));
        }

        // For each sheet in the generated workbook
        for (int i = 0; i < generatedWorkbook.getNumberOfSheets(); i++) {
            String generatedSheetName = generatedWorkbook.getSheetName(i);
            String mappedSheetName = sheetNameMapping.getOrDefault(generatedSheetName, generatedSheetName);

            if (sourceSheetNames.contains(mappedSheetName)) {
                // If the mapped sheet name exists in source, add to the map
                sheetNameMap.put(mappedSheetName, generatedSheetName);
            } else if (sourceSheetNames.contains(generatedSheetName)) {
                // If the sheet names match exactly, add to the map
                sheetNameMap.put(generatedSheetName, generatedSheetName);
            }
            // Else, do not add to the map (sheet will not be compared)
        }

        return sheetNameMap;
    }

    private static List<Integer> getKeyColumnsForSheet(String sheetName) {
        return tabKeyColumnsMap.getOrDefault(sheetName, defaultKeyColumns);
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
                compareRows(sourceRowData.getRow(), bestMatch.getRow(), partialMismatchStyle,
                        summaryList, callback, sheetName);
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

    // getAllRowData method remains unchanged
    private static List<RowData> getAllRowData(Sheet sheet, List<Integer> keyColumns) {
        List<RowData> rowDataList = new ArrayList<>();
        boolean startCollecting = false;

        for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                continue;
            }

            Cell firstCell = row.getCell(0);
            String firstCellValue = getCellValue(firstCell).trim();

            if (!startCollecting) {
                if (firstCellValue.equalsIgnoreCase("Client Code")) {
                    startCollecting = true;
                    continue; // Skip the header row
                } else {
                    continue; // Skip until we find "Client Code"
                }
            }

            if (!isRowEmpty(row)) {
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

        // Updated constructor to accept keyColumns and added debug logging
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
                // Debug log
                System.out.println("Generated key for sheet '" + row.getSheet().getSheetName() + "' at row " + (row.getRowNum() + 1) + ": " + this.key);
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

            if (areValuesSimilar(sourceValue, generatedValue)) {
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

            if (!areValuesSimilar(sourceValue, generatedValue)) {
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

    private static boolean areValuesSimilar(String value1, String value2) {
        // Handle nulls
        if (value1 == null || value2 == null) {
            return value1 == value2; // True if both are null
        }

        // Trim and ignore case
        value1 = value1.trim();
        value2 = value2.trim();

        // Exact match
        if (value1.equalsIgnoreCase(value2)) {
            return true;
        }

        // Numeric comparison with tolerance
        try {
            double num1 = Double.parseDouble(value1);
            double num2 = Double.parseDouble(value2);
            double tolerance = 0.0001;
            return Math.abs(num1 - num2) < tolerance;
        } catch (NumberFormatException e) {
            // Not numeric, continue
        }

        // String similarity
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        double score = similarity.apply(value1.toLowerCase(), value2.toLowerCase());

        return score >= 0.85; // Adjust threshold as needed
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

        // Summary Tab Formatting
        summarySheet.autoSizeColumn(0);
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
