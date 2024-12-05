package com.JSONtoExcelApplication;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.JOptionPane;

public class aMainRun {
	
	private static volatile boolean stopRequested = false;
	
    public static void main(String[] args) {
        // Show the user input UI
        UserInputUI userInputUI = new UserInputUI();
        userInputUI.display();
    }

//    public static void stopExecution() {
//        stopRequested.set(true);
//    }

	public static void runConversion(String SFDCCaseNumber, String InstanceToExecute, String RuleAppVersion, String RuleSetVersion, String TestingVersion, boolean generateExcel, String apiType) {

		// DO NOT MODIFY NEXT FIVE STRINGS
    	String DEV = "https://dev.cloudpack.com/";				// DEV Endpoint
    	String QA = "https://str.apps.test.cloudpak.com/";			// QA Endpoint
    	String PROD = "https://prod.cloudpak.com/";			// PROD Endpoint
    	String EmpCRA = "DecisionService/rest/v1/BuilderRuleApp/";	// Employer RuleApp
    	String EmpCRS = "BuilderRules/";								// Employer RuleSet
        String HPCRA = "DecisionService/rest/v1/HealthPlansRuleApp/"; 	// Health Plan RuleApp
        String HPCRS = "HealthPlansRule/"; 							// Health Plan RuleSet

    	String JSONRequestFolderPath = "C:/Development/CRDTesting/JSONRequestFilesFolder/";

        // This is the directory where the file will be saved. This will take care of creating the repository on your machine.
        String outputDirectory = "C:/Development/CRDTesting/ResponseJSONtoExcel/";

        // Ensure the JSON request directory exists
        Path inputPath = Paths.get(JSONRequestFolderPath);
        if (!Files.exists(inputPath)) {
            try {
                Files.createDirectories(inputPath);
                System.out.println("Directory created: " + JSONRequestFolderPath);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Ensure the output directory exists
        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
                System.out.println("Directory created: " + outputDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Check number of files in the source directory for PROD
        if (InstanceToExecute.equals("PROD")) {
            try {
                long fileCount = Files.list(Paths.get(JSONRequestFolderPath)).count();
                if (fileCount > 50) {
                    JOptionPane.showMessageDialog(null, "You have selected PROD. Do not run more than 50 files at once.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // Get the current date in mmddyyyy format
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyyyy");
        String formattedDate = currentDate.format(formatter);
        	
        // Determine the APO URL based on the environment. 
        String baseUrl = InstanceToExecute.equals("DEV") ? DEV : InstanceToExecute.equals("QA") ? QA : PROD;
        String apiUrl = baseUrl + (apiType.equals("Employer") ? EmpCRA : HPCRA) + RuleAppVersion + "/" + (apiType.equals("Employer") ? EmpCRS : HPCRS) + RuleSetVersion;

        // Process each JSON request file in the input directory
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(JSONRequestFolderPath), "*.json")) {
            for (Path entry : stream) {
                if (stopRequested) {
                    System.out.println("Execution stopped.");
                    break;
                }                
				processJsonRequest(entry.toString(), apiUrl, outputDirectory, formattedDate, SFDCCaseNumber, InstanceToExecute, TestingVersion, generateExcel);
				incrementGeneratedFileCount();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private static void processJsonRequest(String jsonFilePath, String apiUrl, String outputDirectory, String formattedDate, String SFDCCaseNumber, String InstanceToExecute, String TestingVersion, boolean generateExcel) {
        // Configure the file names based on the JSON request file name and the current date
        String jsonFileName = Paths.get(jsonFilePath).getFileName().toString();
        String baseFileName = jsonFileName.substring(0, jsonFileName.lastIndexOf('.'));
        String excelFileName = SFDCCaseNumber + "_" + InstanceToExecute + "_CRD_" + baseFileName + "_" + formattedDate + "_v" + TestingVersion + ".xlsx";
        String jsonOutputFileName = SFDCCaseNumber + "_" + InstanceToExecute + "_CRD_" + baseFileName + "_" + formattedDate + "_v" + TestingVersion + ".json";
        Path excelOutputPath = Paths.get(outputDirectory, excelFileName);
        Path jsonOutputPath = Paths.get(outputDirectory, jsonOutputFileName);
        
        try {
            // Read the JSON request from the file
            String jsonRequest = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();

            BufferedReader in;
            if (responseCode >= 200 && responseCode < 300) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine.trim());
            }
            in.close();

            connection.disconnect();

            // Parse the response JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            

            // Debugging: Print out the entire JSON response
//			System.out.println("Full jsonResponse: " + jsonResponse.toString(2));
			
			// Extract values for clientCode and planId from the full JSON response
			String clientCode = jsonResponse.optString("cltCd", ""); 
			String planId = jsonResponse.optString("planId", ""); 
			
			// Debugging: Print out clientCode and planId values
//			System.out.println("clientCode: " + clientCode); 
//			System.out.println("planId: " + planId); 

            
            JSONObject benefitResponse = jsonResponse.getJSONObject("benefitResponse");

            // Save JSON response to file
            if (!generateExcel) {
                JsonGenerator.saveJsonToFile(jsonResponse, jsonOutputPath);
                System.out.println("JSON file has been created successfully at " + jsonOutputPath);
            }

            // Generate Excel file if required
            if (generateExcel) {
                ExcelGenerator.generateExcelFile(benefitResponse, excelOutputPath, clientCode, planId);
                System.out.println("Excel file has been created successfully at " + excelOutputPath);
            }

            // System.out.println("Files have been created successfully at " + outputDirectory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * Method to cancel the ongoing conversion process.
	 */
    public static void cancelConversion() {
    	stopRequested = true;
    }
    
    /**
     * Method to increment the generated file count.
     */
    private static void incrementGeneratedFileCount() {
        UserInputUI.incrementGeneratedFileCount();
    }
}
