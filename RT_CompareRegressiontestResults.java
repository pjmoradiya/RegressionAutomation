package com.JSONtoExcelApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class RegressionTester {

    public static void performRegressionTesting(String sfdcCaseNumber, String ruleAppVersion, String ruleSetVersion,
                                                String testingVersion, String apiType) {
        // Directories for QA and PROD outputs
        String qaOutputDirectory = "C:/Development/CRDTesting/RegressionTesting/QA";
        String prodOutputDirectory = "C:/Development/CRDTesting/RegressionTesting/PROD";

        // Run conversion for QA environment
        System.out.println("Starting regression testing for QA environment...");
        aMainRun.runConversionWithOutputDirectory(sfdcCaseNumber, "QA", ruleAppVersion, ruleSetVersion, testingVersion,
                                                  true, apiType, qaOutputDirectory);

        // Run conversion for PROD environment
        System.out.println("Starting regression testing for PROD environment...");
        aMainRun.runConversionWithOutputDirectory(sfdcCaseNumber, "PROD", ruleAppVersion, ruleSetVersion,
                                                  testingVersion, true, apiType, prodOutputDirectory);

        System.out.println("Regression testing completed.");

        // Now, perform the comparison
        System.out.println("Starting comparison of regression test results...");
        compareRegressionTestResults(prodOutputDirectory, qaOutputDirectory);
        System.out.println("Comparison of regression test results completed.");
    }

    public static void compareRegressionTestResults(String prodDirPath, String qaDirPath) {
        String outputDirPath = "C:/Development/CRDTesting/RegressionTesting/CompareOutput";

        File prodDir = new File(prodDirPath);
        File qaDir = new File(qaDirPath);

        if (!prodDir.isDirectory() || !qaDir.isDirectory()) {
            System.out.println("Invalid directories for comparison.");
            return;
        }

        // Create output directory if it doesn't exist
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
                System.out.println("Comparison Output Path Created at: " + outputDirPath);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Build a map of files in QA directory for quick lookup
        Map<String, File> qaFilesMap = new HashMap<>();
        for (File qaFile : Objects.requireNonNull(qaDir.listFiles())) {
            if (isValidExcelFile(qaFile)) {
                String fileNamePart = extractFileNamePart(qaFile.getName());
                if (fileNamePart != null) {
                    qaFilesMap.put(fileNamePart, qaFile);
                }
            }
        }

        // Executor for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (File prodFile : Objects.requireNonNull(prodDir.listFiles())) {
            if (isValidExcelFile(prodFile)) {
                String fileNamePart = extractFileNamePart(prodFile.getName());
                if (fileNamePart != null) {
                    File matchingQaFile = qaFilesMap.get(fileNamePart);
                    if (matchingQaFile != null) {
                        // Found matching file, perform comparison
                        System.out.println("Queuing comparison for PROD file: " + prodFile.getName()
                                + " with QA file: " + matchingQaFile.getName());

                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                ExcelFileComparator.ProgressCallback callback = message -> System.out.println(message);

                                ExcelFileComparator.compareSpecificFiles(prodFile, matchingQaFile, outputDirPath, callback);
                                System.out.println("Comparison completed for: " + matchingQaFile.getName());
                            } catch (IOException e) {
                                System.out.println("Error processing file: " + matchingQaFile.getName() + " - " + e.getMessage());
                                e.printStackTrace();
                            }
                        }, executor);
                        futures.add(future);
                    } else {
                        System.out.println("No matching QA file found for PROD file: " + prodFile.getName());
                    }
                } else {
                    System.out.println("Invalid PROD file name pattern: " + prodFile.getName());
                }
            }
        }

        // Wait for all tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.get();
            System.out.println("All comparison tasks completed.");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    // Helper method to extract the file name part between "CRD_" and "_<Date>_v"
    private static String extractFileNamePart(String fileName) {
        // Remove the extension
        if (fileName.endsWith(".xlsx")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }

        // Find the start index of "CRD_"
        int crdIndex = fileName.indexOf("CRD_");
        if (crdIndex == -1) {
            return null;
        }
        crdIndex += 4; // Move past "CRD_"

        // Find the index of "_<Date>_v"
        int dateIndex = fileName.indexOf("_", crdIndex);
        if (dateIndex == -1) {
            return null;
        }

        // Extract the substring between "CRD_" and "_<Date>_v"
        return fileName.substring(crdIndex, dateIndex);
    }

    // Helper method to check if a file is a valid Excel file
    private static boolean isValidExcelFile(File file) {
        return file.isFile() && file.getName().endsWith(".xlsx") && !file.getName().startsWith("~$");
    }
}
