Implementing Regression Testing Without Modifying Existing Classes

Hello! I understand your requirement to implement regression testing while avoiding changes to your existing classes, especially aMainRun. Instead, you want to move the logic of saving files into the RegressionTester class. I’ll provide the complete code with the necessary modifications, ensuring that all existing logic remains intact.

Approach Overview

	•	Step 1: You’re okay with adding the regression testing button and action listener in your UserInputUI class.
	•	Step 2: We’ll create a new class, RegressionTester, to handle regression testing logic without modifying aMainRun.
	•	Step 3: We’ll adjust the RegressionTester class to use existing methods from aMainRun and manipulate files as needed.

1. Creating the RegressionTester Class

We’ll create a new class called RegressionTester that will:

	•	Call aMainRun.runConversion for both PROD and QA environments without modifying aMainRun.
	•	After generating the files, move or copy them to the appropriate directories for comparison.
	•	Use the existing ExcelFileComparator.compareExcelFiles method to compare the files.

RegressionTester.java

  ------------------------------------------------------------------------------------------------------------------------------------------------

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
        String prodOutputDirectory = "C:/Development/CRDTesting/Compare/SourceFiles/Archive/";
        String qaOutputDirectory = outputDirectory; // QA files are generated here

        // Ensure necessary directories exist
        createDirectoryIfNotExists(outputDirectory);
        createDirectoryIfNotExists(prodOutputDirectory);

        // Process JSON files against PROD API
        System.out.println("Processing JSON files against PROD API...");
        aMainRun.runConversion(SFDCCaseNumber, "PROD", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // Move PROD files to prodOutputDirectory
        moveFilesByInstance(outputDirectory, prodOutputDirectory, "PROD");

        // Process JSON files against QA API
        System.out.println("Processing JSON files against QA API...");
        aMainRun.runConversion(SFDCCaseNumber, "QA", RuleAppVersion, RuleSetVersion, TestingVersion, true, apiType);

        // No need to move QA files; they are already in outputDirectory (qaOutputDirectory)

        // Compare the generated Excel files
        String outputComparisonDirectory = "C:/Development/CRDTesting/Compare/ComparisonOutput/";
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
}

------------------------------------------------------------------------------------------------------------------------------------------------
  Explanation:

	•	performRegressionTesting Method:
	•	Calls aMainRun.runConversion for both “PROD” and “QA” instances.
	•	After processing “PROD”, moves the generated files containing “PROD” in their filenames to the prodOutputDirectory.
	•	Processes “QA” files, which remain in the qaOutputDirectory (ResponseJSONtoExcel).
	•	Compares the files between prodOutputDirectory and qaOutputDirectory using ExcelFileComparator.compareExcelFiles.
	•	moveFilesByInstance Method:
	•	Moves files from the source directory to the target directory based on the instance identifier in the filename (e.g., “PROD”).
	•	No Changes to aMainRun:
	•	We use aMainRun.runConversion as-is without modifications.
	•	The logic for saving files is managed within RegressionTester.

2. Updating the UserInputUI Class

Add the regression testing button and action listener in your UserInputUI class.

UserInputUI.java

--------------------------------------


package com.JSONtoExcelApplication;

// [Your existing imports]
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UserInputUI {

    // [Your existing UI components]
    private JTextField sfdcCaseNumberTextField;
    private JTextField ruleAppVersionTextField;
    private JTextField ruleSetVersionTextField;
    private JTextField testingVersionTextField;

    // [Other UI components and methods]

    public void display() {
        // [Your existing UI setup code]

        // Create Regression Testing button
        JButton regressionTestingButton = new JButton("Regression Testing");
        panel.add(regressionTestingButton);

        regressionTestingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Collect user inputs
                String SFDCCaseNumber = sfdcCaseNumberTextField.getText();
                String RuleAppVersion = ruleAppVersionTextField.getText();
                String RuleSetVersion = ruleSetVersionTextField.getText();
                String TestingVersion = testingVersionTextField.getText();
                String apiType = "Employer"; // Adjust based on your application

                // Run the regression testing in a separate thread to keep the UI responsive
                new Thread(() -> {
                    RegressionTester.performRegressionTesting(SFDCCaseNumber, RuleAppVersion, RuleSetVersion, TestingVersion, apiType);
                    // Optionally, update UI components or display a message when done
                    JOptionPane.showMessageDialog(null, "Regression testing completed.");
                }).start();
            }
        });

        // [Rest of your UI setup code]
    }

    // [Other methods as needed]
}

-------------------------
  Explanation:

	•	Added the Regression Testing Button:
	•	Created a new JButton for “Regression Testing” and added it to the UI panel.
	•	Action Listener:
	•	When clicked, it collects user inputs and calls RegressionTester.performRegressionTesting in a separate thread.
	•	This keeps the UI responsive during the regression testing process.

3. Existing Classes Remain Unchanged

As per your request, we haven’t modified the existing aMainRun class or any other existing classes. All the logic for saving files and managing directories has been moved into the RegressionTester class.

Testing and Verification

To test the implementation:

	1.	Compile Your Application:
	•	Ensure all classes are compiled without errors.
	2.	Run Your Application:
	•	Launch the UI and fill in the necessary fields.
	3.	Perform Regression Testing:
	•	Click the “Regression Testing” button.
	4.	Verify File Generation:
	•	After processing, check the following directories:
	•	PROD Excel Files: C:/Development/CRDTesting/Compare/SourceFiles/Archive/
	•	QA Excel Files: C:/Development/CRDTesting/ResponseJSONtoExcel/
	5.	Verify Comparison Output:
	•	Check the comparison results in C:/Development/CRDTesting/Compare/ComparisonOutput/.
	6.	Review Console Output:
	•	Observe the console for progress messages and any potential errors.

Additional Notes

	•	File Naming Convention:
	•	Since aMainRun includes InstanceToExecute (e.g., “PROD”, “QA”) in the filenames, we can reliably identify and move the files based on this identifier.
	•	Concurrency Considerations:
	•	The processing is done sequentially: first “PROD”, then “QA”.
	•	Ensure that the ResponseJSONtoExcel directory is clear of old files before running regression testing to avoid mixing files from previous runs.
	•	Cleanup Before Testing (Optional):
	•	You might want to clean up or archive existing files in the output directories before running regression tests to avoid any conflicts.
	•	Extensibility:
	•	If you need to test other environments (e.g., “DEV”), you can extend the RegressionTester class accordingly.

Final Remarks

By implementing the RegressionTester class as shown, we have:

	•	Kept Existing Classes Unchanged:
	•	No modifications were made to aMainRun or other existing classes.
	•	Moved Logic Into RegressionTester:
	•	All directory management and file handling logic specific to regression testing is encapsulated within RegressionTester.
	•	Provided Complete Code:
	•	The code provided includes all necessary modifications and is ready for integration into your application.

Please integrate the RegressionTester class and update your UserInputUI class as shown. If you have any questions or need further assistance, feel free to ask. I’m here to help!
