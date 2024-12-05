package com.JSONtoExcelApplication;

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
    }
}
