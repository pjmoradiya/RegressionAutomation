package com.JSONtoExcelApplication;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

public class RegressionTester {

    public static void performRegressionTesting(String SFDCCaseNumber, String RuleAppVersion, String RuleSetVersion, String TestingVersion, String apiType) {
        // Input JSON directory (as per existing code)
        String JSONRequestFolderPath = "C:/Development/CRDTesting/JSONRequestFilesFolder/";

        // Output directory as per aMainRun (hardcoded in aMainRun)
        String outputDirectory = "C:/Development/CRDTesting/ResponseJSONtoExcel/";

        // Directories for PROD and QA outputs
        String prodOutputDirectory = "C:/Development/CRDTesting/RegressionTestingOutput/PROD_CRDFiles"; // PROD files are generated here
        String qaOutputDirectory = "C:/Development/CRDTesting/RegressionTestingOutput/QA_CRDFiles"; // QA files are generated here

        // Ensure necessary directories exist
        createDirectoryIfNotExists(outputDirectory);
        createDirectoryIfNotExists(prodOutputDirectory);
        createDirectoryIfNotExists(qaOutputDirectory);

        // Clear previous files in output directories
        clearDirectory(qaOutputDirectory);
        clearDirectory(prodOutputDirectory);

        // Process JSON files against PROD API
        System.out.println("Processing JSON files against PROD API...");
        aMainRun.runConversion(SFDCCaseNumber, "PROD", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // Move PROD files to prodOutputDirectory
        moveFilesByInstance(outputDirectory, prodOutputDirectory, "PROD");

        // Process JSON files against QA API
        System.out.println("Processing JSON files against QA API...");
        aMainRun.runConversion(SFDCCaseNumber, "QA", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // Move PROD files to qaOutputDirectory
        moveFilesByInstance(outputDirectory, prodOutputDirectory, "QA");

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

    private static void moveFilesByInstance(String sourceDir, String targetDir, String instanceIdentifier) {
        try (Stream<Path> files = Files.list(Paths.get(sourceDir))) {
            files.filter(path -> path.getFileName().toString().contains("_" + instanceIdentifier + "_"))
                 .forEach(path -> {
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
