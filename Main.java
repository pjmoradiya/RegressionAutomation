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

/**
 * This Java program is developed for Employer CRD Testing.
 * This program automates the conversion of JSON responses from an API to an Excel file.
 * It streamlines the testing process, significantly reducing the time required.
 *
 * Program Features:
 * - Specify the path of the request JSON file stored on your computer.
 * - Choose the environment for executing the request: DEV, QA, or PROD.
 * - Define the RuleApp and RuleSet version for the execution.
 * - The path of the file going to be saved at C:/Development/CRDTesting/ResponseJSONtoExcel/ path.
 * - You are not required to create the directory, this program will take care of creating the directory. 
 * - The excel file name will be as follows...
 * 			SFDC Case Number
 * 			Environment is being executed against
 * 			CRD
 * 			Name of the JSON Request File
 * 			Date of code execution
 * 			Version specified
 * 			Ex: 0000000 QA CRD - SPEC1 - 6-27-2024 - 06292024 - v101.xlsx
 *
 * Usage:
 * - Ensure the JSON request file is correctly specified.
 * - Configure the desired environment, RuleApp, RuleSet version, and output file path.
 * - Run this Java program.
 *
 * Performance:
 * - The program typically processes an average-sized request within 5-10 seconds.
 *
 * Excel File Structure:
 * - Each key in the JSON response's "benefitResponse" object will be represented as a separate sheet in the Excel file.
 * - Each sheet will contain:
 *   - A header row with column names derived from the JSON keys.
 *   - Subsequent rows representing the values for each JSON object in the array.
 *
 * Example:
 * If the JSON response contains the keys "otherPtntPayIPLst" and "ptntPayCrdLst", the Excel file will have two sheets named "otherPtntPayIPLst" and "ptntPayCrdLst".
 * Each sheet will have columns corresponding to the attributes of the JSON objects and rows containing their respective values.

 * @author 
 *
 *
 */

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
    	String DEV = "https://dsr.odm.apps.dev.p.aetna.com/";				// DEV Endpoint
    	String QA = "https://dsr.odm.str.apps.test.p.aetna.com/";			// QA Endpoint
    	String PROD = "https://dsr.cvs.odm.apps.prod.p.aetna.com/";			// PROD Endpoint
    	String EmpCRA = "DecisionService/rest/v1/RuleApp/";	// Employer RuleApp
    	String EmpCRS = "Rules/";								// Employer RuleSet
        String HPCRA = "DecisionService/rest/v1/RuleApp/"; 	// Health Plan RuleApp
        String HPCRS = "Rule/"; 							// Health Plan RuleSet

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
