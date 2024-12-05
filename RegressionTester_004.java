package com.JSONtoExcelApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class RegressionTester {

    public static void performRegressionTesting(String sfdcCaseNumber, String ruleAppVersion, String ruleSetVersion,
                                                String testingVersion, String apiType, ProgressCallback callback) {
        // Directories for QA and PROD outputs
        String qaOutputDirectory = "C:/Development/CRDTesting/RegressionTesting/QA";
        String prodOutputDirectory = "C:/Development/CRDTesting/RegressionTesting/PROD";

        // Run conversion for QA environment
        callback.log("Starting regression testing for QA environment...");
        aMainRun.runConversionWithOutputDirectory(sfdcCaseNumber, "QA", ruleAppVersion, ruleSetVersion, testingVersion,
                                                  true, apiType, qaOutputDirectory);

        // Run conversion for PROD environment
        callback.log("Starting regression testing for PROD environment...");
        aMainRun.runConversionWithOutputDirectory(sfdcCaseNumber, "PROD", ruleAppVersion, ruleSetVersion,
                                                  testingVersion, true, apiType, prodOutputDirectory);

        callback.log("File generation for regression testing completed.");

        // Now, perform the comparison
        callback.log("Starting comparison of regression test results...");
        compareRegressionTestResults(prodOutputDirectory, qaOutputDirectory, callback);
        callback.log("Comparison of regression test results completed.");
    }

    public static void compareRegressionTestResults(String prodDirPath, String qaDirPath, ProgressCallback callback) {
        String outputDirPath = "C:/Development/CRDTesting/RegressionTesting/CompareOutput";

        File prodDir = new File(prodDirPath);
        File qaDir = new File(qaDirPath);

        if (!prodDir.isDirectory() || !qaDir.isDirectory()) {
            callback.log("Invalid directories for comparison.");
            return;
        }

        // Create output directory if it doesn't exist
        Path outputDir = Paths.get(outputDirPath);
        if (!Files.exists(outputDir)) {
            try {
                Files.createDirectories(outputDir);
                callback.log("Comparison Output Path Created at: " + outputDirPath);
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
                        callback.log("Queuing comparison for PROD file: " + prodFile.getName()
                                + " with QA file: " + matchingQaFile.getName());

                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                // Use the same callback for nested methods
                                ExcelFileComparator.compareSpecificFiles(prodFile, matchingQaFile, outputDirPath, callback);
                                callback.log("Comparison completed for: " + matchingQaFile.getName());
                            } catch (IOException e) {
                                callback.log("Error processing file: " + matchingQaFile.getName() + " - " + e.getMessage());
                                e.printStackTrace();
                            }
                        }, executor);
                        futures.add(future);
                    } else {
                        callback.log("No matching QA file found for PROD file: " + prodFile.getName());
                    }
                } else {
                    callback.log("Invalid PROD file name pattern: " + prodFile.getName());
                }
            }
        }

        // Wait for all tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            allOf.get();
            callback.log("All comparison tasks completed.");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    // Helper methods remain unchanged...
}
