package com.JSONtoExcelApplication;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class RegressionTester {

    public static void performRegressionTesting(String SFDCCaseNumber, String RuleAppVersion, String RuleSetVersion, String TestingVersion, String apiType) {
        // Input JSON directory
        String JSONRequestFolderPath = "C:/Development/CRDTesting/JSONRequestFilesFolder/";

        // Output directory used by aMainRun (hardcoded in aMainRun)
        String outputDirectory = "C:/Development/CRDTesting/ResponseJSONtoExcel/";

        // Directories for PROD and QA outputs
        String prodOutputDirectory = "C:/Development/CRDTesting/RegressionTestingOutput/PROD_CRDFiles";
        String qaOutputDirectory = "C:/Development/CRDTesting/RegressionTestingOutput/QA_CRDFiles";

        // Ensure necessary directories exist
        createDirectoryIfNotExists(outputDirectory);
        createDirectoryIfNotExists(prodOutputDirectory);
        createDirectoryIfNotExists(qaOutputDirectory);

        // Clear previous files in output directories
        clearDirectory(outputDirectory);
        clearDirectory(prodOutputDirectory);
        clearDirectory(qaOutputDirectory);

        // Process JSON files against PROD API
        System.out.println("Processing JSON files against PROD API...");
        aMainRun.runConversion(SFDCCaseNumber, "PROD", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // Move all generated files to prodOutputDirectory
        moveFiles(outputDirectory, prodOutputDirectory);

        // Clear output directory before processing QA files
        clearDirectory(outputDirectory);

        // Process JSON files against QA API
        System.out.println("Processing JSON files against QA API...");
        aMainRun.runConversion(SFDCCaseNumber, "QA", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // Move all generated files to qaOutputDirectory
        moveFiles(outputDirectory, qaOutputDirectory);

        // Compare the generated Excel files
        String outputComparisonDirectory = "C:/Development/CRDTesting/RegressionTestingOutput/ComparisonOutput/";
        createDirectoryIfNotExists(outputComparisonDirectory);

        try {
            System.out.println("Comparing the generated Excel files...");
            ExcelFileComparator.compareExcelFiles(prodOutputDirectory, qaOutputDirectory, outputComparisonDirectory, message -> {
                // Log messages
                System.out.println(message);
            });
            // Notify the user that regression testing is complete
            System.out.println("Regression testing completed. Results are in: " + outputComparisonDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred during regression testing: " + e.getMessage());
        }
    }

    private static void moveFiles(String sourceDir, String targetDir) {
        try (Stream<Path> files = Files.list(Paths.get(sourceDir))) {
            files.forEach(path -> {
                try {
                    Path targetPath = Paths.get(targetDir, path.getFileName().toString());
                    Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Moved file: " + path.getFileName().toString() + " to " + targetDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                System.out.println("Created directory: " + directoryPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clearDirectory(String directoryPath) {
        try (Stream<Path> files = Files.list(Paths.get(directoryPath))) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                    System.out.println("Deleted file: " + path.getFileName().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            // Directory might not exist yet; that's okay
        }
    }
}
