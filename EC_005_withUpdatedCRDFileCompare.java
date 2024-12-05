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

    // Updated extractFileNamePart method
    private static String extractFileNamePart(String fileName) {
        // Remove the extension
        if (fileName.endsWith(".xlsx")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }

        // Split the filename by "_"
        String[] parts = fileName.split("_");

        // Find the index of "CRD"
        int crdIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("CRD")) {
                crdIndex = i;
                break;
            }
        }

        if (crdIndex >= 0 && crdIndex + 1 < parts.length) {
            // Find the dateIndex by looking for an 8-digit number (mmddyyyy)
            int dateIndex = -1;
            for (int i = crdIndex + 1; i < parts.length; i++) {
                if (parts[i].matches("\\d{8}")) {
                    dateIndex = i;
                    break;
                }
            }

            // Set the end index for the filename parts
            int endIndex = (dateIndex != -1) ? dateIndex : parts.length;

            // Reconstruct the <FileName> by joining parts from crdIndex+1 to endIndex-1
            StringBuilder fileNameBuilder = new StringBuilder();
            for (int i = crdIndex + 1; i < endIndex; i++) {
                if (fileNameBuilder.length() > 0) {
                    fileNameBuilder.append("_");
                }
                fileNameBuilder.append(parts[i]);
            }
            return fileNameBuilder.toString();
        }

        // If pattern does not match, return the filename without extension
        return fileName;
    }

    // Rest of the methods remain unchanged
    // [Include the rest of the methods from the previous code provided]
    // For brevity, I have omitted the unchanged methods here.

    // ...

}
