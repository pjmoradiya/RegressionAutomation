package com.JSONtoExcelApplication;

import javax.swing.*;

import com.JSONtoExcelApplication.ExcelFileComparator.ProgressCallback;
import com.JSONtoExcelApplication.RegressionTester;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UserInputUI {
	private static ExecutorService executor;
	private static Future<?> futureTask;
	private static JLabel sourceFileCountLabel;
	private static JLabel generatedFileCountLabel;
	private static JLabel compareSourceFileCountLabel; // Label for source folder file count
	private static JLabel compareGeneratedFileCountLabel; // Label for comparison output folder file count
	private static int generatedFileCount;
	private static int compareGeneratedFileCount;

	// Variables to hold selected files
	private static File selectedSourceFile = null;
	private static File selectedGeneratedFile = null;

	public static void display() {
        JFrame frame = new JFrame("Employer and Healthplan CRD Test Automation Tool - v5.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridBagLayout());
        frame.setSize(1200, 700);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 5; // Make title and description span two columns

        // Logo
        JLabel logoLabel = new JLabel(new ImageIcon("C:/Development/CRDTesting/Documents/CVSLogo.png"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 5;
        frame.add(logoLabel, gbc);

        // Empty line
        gbc.gridy++;
        frame.add(new JLabel(" "), gbc);

        // Title
        JLabel titleLabel = new JLabel("CRD Test Automation", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.RED);
        gbc.gridy++;
        gbc.gridwidth = 5;
        frame.add(titleLabel, gbc);
        
        // Version Title
        JLabel versionLabel = new JLabel("Version 5.0", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Arial", Font.BOLD, 18));
        versionLabel.setForeground(Color.BLACK);
        gbc.gridy++;
        gbc.gridwidth = 5;
        frame.add(versionLabel, gbc);

        // Description
        JLabel descriptionLabel1 = new JLabel("<html>The tool will run a JSON request file against a specified ODM environment to produce a JSON response and convert it into a Benefit Builder CRD.<br>"
        		+ "The tool also has a compare function to compare pre/post ODM rule changes.<br>" 
        		+ "<br>" 
        		+ "All fields are mandatory for generating JSON Response or Benefit Builder CRD files only.</html>", SwingConstants.CENTER);
        descriptionLabel1.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridy++;
        gbc.gridwidth = 5;
        frame.add(descriptionLabel1, gbc);

        // Empty lines
        gbc.gridy++;
        frame.add(new JLabel(" "), gbc);

        // Reset grid width for input fields
        gbc.gridwidth = 1;
        
        
        // First Row
        JLabel apiTypeLabel = new JLabel("LOB:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(apiTypeLabel, gbc);

        String[] apiOptions = {"Employer", "Health Plan"};
        JComboBox<String> apiComboBox = new JComboBox<>(apiOptions);
        gbc.gridx = 1;
        frame.add(apiComboBox, gbc);

        JLabel apiOptionsComment = new JLabel("Drop-down to select the LOB.");
        apiOptionsComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(apiOptionsComment, gbc);
        
        JLabel sourceFileLabelEmpty = new JLabel("                    ");
        gbc.gridx = 3;
        frame.add(sourceFileLabelEmpty, gbc);
        
        JLabel sourceFileLabel = new JLabel("# of JSON Request Files:    ");
        gbc.gridx = 4;
        frame.add(sourceFileLabel, gbc);

        sourceFileCountLabel = new JLabel("0");
        gbc.gridx = 5;
        frame.add(sourceFileCountLabel, gbc);
        
        
        // Second Row       
        JLabel sfdcLabel = new JLabel("SFDC Case Number:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(sfdcLabel, gbc);

        JTextField sfdcField = new JTextField(10);
        gbc.gridx = 1;
        frame.add(sfdcField, gbc);

        JLabel sfdcFieldComment = new JLabel("This accepts any alpha-numeric value without spaces");
        sfdcFieldComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(sfdcFieldComment, gbc);
        
        JLabel generatedFileLabelEmpty = new JLabel("                    ");
        gbc.gridx = 3;
        frame.add(generatedFileLabelEmpty, gbc);
        
        JLabel generatedFileLabel = new JLabel("# of Generated CRD Files:    ");
        gbc.gridx = 4;
        frame.add(generatedFileLabel, gbc);

        generatedFileCountLabel = new JLabel("0");
        gbc.gridx = 5;
        frame.add(generatedFileCountLabel, gbc);
        
        
        // Third Row        
        JLabel envLabel = new JLabel("Environment:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(envLabel, gbc);

        String[] envOptions = {"DEV", "QA", "PROD"};
        JComboBox<String> envComboBox = new JComboBox<>(envOptions);
        gbc.gridx = 1;
        frame.add(envComboBox, gbc);

        JLabel envOptionsComment = new JLabel("Drop-down to select the environment.");
        envOptionsComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(envOptionsComment, gbc);
        
        JLabel compareSourceFileLabelEmpty = new JLabel("                    ");
        gbc.gridx = 3;
        frame.add(compareSourceFileLabelEmpty, gbc);
        
        JLabel compareSourceFileLabel = new JLabel("# of Source Files for Comparison:");
        gbc.gridx = 4;
        frame.add(compareSourceFileLabel, gbc);

        compareSourceFileCountLabel = new JLabel("0");
        gbc.gridx = 5;
        frame.add(compareSourceFileCountLabel, gbc);
        
        
        // Fourth Row   
        JLabel ruleAppLabel = new JLabel("RuleApp Version:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(ruleAppLabel, gbc);

        JTextField ruleAppField = new JTextField("1.0", 30);
        gbc.gridx = 1;
        frame.add(ruleAppField, gbc);

        JLabel ruleAppComment = new JLabel("<html>Default 1.0 for QA/PROD. Change Only for DEV or as needed.</html>");
        ruleAppComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(ruleAppComment, gbc);
        
        JLabel compareGeneratedFileLabelEmpty = new JLabel("                    ");
        gbc.gridx = 3;
        frame.add(compareGeneratedFileLabelEmpty, gbc);
        
        JLabel compareGeneratedFileLabel = new JLabel("# of Files Compared Files Generated:  ");
        gbc.gridx = 4;
        frame.add(compareGeneratedFileLabel, gbc);

        compareGeneratedFileCountLabel = new JLabel("0");
        gbc.gridx = 5;
        frame.add(compareGeneratedFileCountLabel, gbc);
        
                
        // Fifth Row
        JLabel ruleSetLabel = new JLabel("RuleSet Version:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(ruleSetLabel, gbc);

        JTextField ruleSetField = new JTextField("1.0", 30);
        gbc.gridx = 1;
        frame.add(ruleSetField, gbc);

        JLabel ruleSetComment = new JLabel("<html>Default 1.0 for QA/PROD. Change Only for DEV or as needed.</html>");
        ruleSetComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(ruleSetComment, gbc);
        
        
        // Sixth Row
        JLabel testVerLabel = new JLabel("Testing Version:");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(testVerLabel, gbc);

        JTextField testVerField = new JTextField(10);
        gbc.gridx = 1;
        frame.add(testVerField, gbc);

        JLabel testVerComment = new JLabel("Versioning of files generated to maintain history.");
        testVerComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(testVerComment, gbc);
        
        // Buttons  
        
        // Seventh Row
        JButton createRepoButton = new JButton("Create Repositories");
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(createRepoButton, gbc);    
        
        // Eighth Row
        JButton runJsonButton = new JButton("Generate JSON Response Files");
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(runJsonButton, gbc);

        JButton runExcelButton = new JButton("Generate CRD Files");
        gbc.gridx = 2;
        frame.add(runExcelButton, gbc);
        
        // Ninth Row
        JLabel selectFilesLabel = new JLabel("Select Files for Comparison: ");
        gbc.gridx = 0;
        gbc.gridy++;
        frame.add(selectFilesLabel, gbc);
        
        JButton selectSourceFileButton = new JButton("Select Source File");
        gbc.gridx = 1;
        frame.add(selectSourceFileButton, gbc);
        
        JButton selectGeneratedFileButton = new JButton("Select Generated File");
        gbc.gridx = 2;
        frame.add(selectGeneratedFileButton, gbc);
        
        // Tenth Row
        JLabel selectedSourceFileLabel = new JLabel("If no source files selected, tool will automatically pick files.");
        selectedSourceFileLabel.setForeground(Color.BLUE);
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(selectedSourceFileLabel, gbc);
        
        JLabel selectedGeneratedFileLabel = new JLabel("If no Generated files selected, tool will automatically pick files.");
        selectedGeneratedFileLabel.setForeground(Color.BLUE);
        gbc.gridx = 2;
        frame.add(selectedGeneratedFileLabel, gbc);
        
        // Eleventh Row
        JButton compareExcelButton = new JButton("Compare Excel Files");
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(compareExcelButton, gbc);
        
        JLabel compareExcelComment = new JLabel("<html>Fields from LOB to Testing version are not required for comparison.</html>");
        compareExcelComment.setForeground(Color.GRAY);
        gbc.gridx = 2;
        frame.add(compareExcelComment, gbc);
        
        // Twelth Row
        JButton regressionTestingButton = new JButton("Regression Testing");
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(regressionTestingButton, gbc);
        
        // Thirteenth Row
        JButton stopButton = new JButton("Stop Execution");
        gbc.gridx = 1;
        gbc.gridy++;
        frame.add(stopButton, gbc);      
        
        
        // Console
        JTextArea console = new JTextArea(10, 80);
        console.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(console);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 5;
        frame.add(scrollPane, gbc);
        
        JButton clearConsoleButton = new JButton("Clear");
        gbc.gridx = 5;
        frame.add(clearConsoleButton, gbc);

        // Redirecting sysout
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                console.append(String.valueOf((char) b));
                console.setCaretPosition(console.getDocument().getLength());
            }
        });

        System.setOut(printStream);
        System.setErr(printStream);
        
        // Action listener to clear the console when the button is clicked
        clearConsoleButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
					console.setText("");
				
			}
		});
        
        // Initial file count
        updateSourceFileCount();
        updateCompareSourceFileCount();

        runJsonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
                String sfdcCaseNumber = sfdcField.getText();
                String instanceToExecute = (String) envComboBox.getSelectedItem();
                String ruleAppVersion = ruleAppField.getText();
                String ruleSetVersion = ruleSetField.getText();
                String testingVersion = testVerField.getText();
                String apiType = (String) apiComboBox.getSelectedItem();

                if (sfdcCaseNumber.isEmpty() || instanceToExecute.isEmpty() || ruleAppVersion.isEmpty() || ruleSetVersion.isEmpty() || testingVersion.isEmpty() || apiType.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "All fields are mandatory.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check for PROD environment and file count
                if (instanceToExecute.equals("PROD") || instanceToExecute.equals("DEV") || instanceToExecute.equals("QA")) {
                    try {
                        long fileCount = Files.list(Paths.get("C:/Development/CRDTesting/JSONRequestFilesFolder/")).count();
                        if (fileCount > 50) {
                            JOptionPane.showMessageDialog(frame, "You have selected more than 50 files for execution. Please reduce the file count to less than 50", "Warning", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // Double verification for PROD
                    int confirm = JOptionPane.showConfirmDialog(frame, "You are about to run in " + instanceToExecute + ". Are you sure?", "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                generatedFileCount = 0;
                updateGeneratedFileCount();

                executor = Executors.newSingleThreadExecutor();
                futureTask = executor.submit(() -> {
                    aMainRun.runConversion(sfdcCaseNumber, instanceToExecute, ruleAppVersion, ruleSetVersion, testingVersion, false, apiType);
                });
                
                System.out.println("Generating JSON Files...");
            }
        });

        runExcelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
                String sfdcCaseNumber = sfdcField.getText();
                String instanceToExecute = (String) envComboBox.getSelectedItem();
                String ruleAppVersion = ruleAppField.getText();
                String ruleSetVersion = ruleSetField.getText();
                String testingVersion = testVerField.getText();
                String apiType = (String) apiComboBox.getSelectedItem();

                if (sfdcCaseNumber.isEmpty() || instanceToExecute.isEmpty() || ruleAppVersion.isEmpty() || ruleSetVersion.isEmpty() || testingVersion.isEmpty() || apiType.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "All fields are mandatory.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Check for PROD environment and file count
                if (instanceToExecute.equals("PROD") || instanceToExecute.equals("DEV") || instanceToExecute.equals("QA")) {
                    try {
                        long fileCount = Files.list(Paths.get("C:/Development/CRDTesting/JSONRequestFilesFolder/")).count();
                        if (fileCount > 50) {
                            JOptionPane.showMessageDialog(frame, "You have selected more than 50 files for execution. Please reduce the file count to less than 50", "Warning", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    // Double verification for PROD
                    int confirm = JOptionPane.showConfirmDialog(frame, "You have selected " + instanceToExecute + " environment. Press Yes to proceed?", "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                generatedFileCount = 0;
                updateGeneratedFileCount();

                executor = Executors.newSingleThreadExecutor();
                futureTask = executor.submit(() -> {
                    aMainRun.runConversion(sfdcCaseNumber, instanceToExecute, ruleAppVersion, ruleSetVersion, testingVersion, true, apiType);
                });
                
                System.out.println("Generating CRD Files...");
            }
        });
        
     // Action listeners for file selection buttons
        selectSourceFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("C:/Development/CRDTesting/Compare/SourceFiles");
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedSourceFile = fileChooser.getSelectedFile();
                    selectedSourceFileLabel.setText(selectedSourceFile.getName());
                }
            }
        });

        selectGeneratedFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("C:/Development/CRDTesting/ResponseJSONtoExcel/");
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    selectedGeneratedFile = fileChooser.getSelectedFile();
                    selectedGeneratedFileLabel.setText(selectedGeneratedFile.getName());
                }
            }
        });
        
        compareExcelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
                
                // If both files are selected, compare them directly
                if (selectedSourceFile != null && selectedGeneratedFile != null) {
                    String outputDirPath = "C:/Development/CRDTesting/Compare/ComparisonOutput";
                    compareGeneratedFileCount = 0; // Reset the count
                    updateCompareGeneratedFileCount(); // Update the UI

                    executor = Executors.newSingleThreadExecutor();
                    futureTask = executor.submit(() -> {
                        try {
                            // Pass the callback implementation to log messages to the JTextArea
                            ExcelFileComparator.ProgressCallback callback = new ExcelFileComparator.ProgressCallback() {
                                @Override
                                public void log(String message) {
                                    SwingUtilities.invokeLater(() -> console.append(message + "\n"));
                                }
                            };

                            ExcelFileComparator.compareSpecificFiles(selectedSourceFile, selectedGeneratedFile, outputDirPath, callback);
                            SwingUtilities.invokeLater(() -> console.append("Comparison completed. Output files created in: " + outputDirPath + "\n"));
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                console.append("Error during comparison: " + ex.getMessage() + "\n");
                                ex.printStackTrace();
                            });
                        }
                    });
                } else {
                	
                	// No specific files selected, proceed with default directory comparison
                	
	                String sourceDirPath = "C:/Development/CRDTesting/Compare/SourceFiles";
	                String generatedDirPath = "C:/Development/CRDTesting/ResponseJSONtoExcel/";
	                String outputDirPath = "C:/Development/CRDTesting/Compare/ComparisonOutput";
	                
	                compareGeneratedFileCount = 0; // Reset the count
	                updateCompareGeneratedFileCount(); // Update the UI
	
	                executor = Executors.newSingleThreadExecutor();
	                futureTask = executor.submit(() -> {
	                    try {
	                        // Pass the callback implementation to log messages to the JTextArea
	                        ExcelFileComparator.ProgressCallback callback = new ExcelFileComparator.ProgressCallback() {
	                            @Override
	                            public void log(String message) {
	                                SwingUtilities.invokeLater(() -> console.append(message + "\n"));
	                            }
	                        };
	
	                        ExcelFileComparator.compareExcelFiles(sourceDirPath, generatedDirPath, outputDirPath, callback);
	                        SwingUtilities.invokeLater(() -> console.append("Comparison completed. Output files created in: " + outputDirPath + "\n"));
	                    } catch (IOException ex) {
	                        SwingUtilities.invokeLater(() -> {
	                            console.append("Error during comparison: " + ex.getMessage() + "\n");
	                            ex.printStackTrace();
	                        });
	                    }
	                });
	            }
            }
        });

        stopButton.addActionListener(new ActionListener() {

	@Override
            public void actionPerformed(ActionEvent e) {
                if (futureTask != null && !futureTask.isDone()) {
                    aMainRun.cancelConversion(); // Signal to cancel the conversion
                    executor.shutdownNow();
                    console.append("Execution stopped.\n");
                }
            }
        });
        
        // Add ActionListener to create the repositories when the button is clicked
        createRepoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RepositoryManager.createRepositories();  // Call the method to create repositories
            }
        });
        
     // Action Listener for the Regression Testing Button
        regressionTestingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");
                String sfdcCaseNumber = sfdcField.getText();
                String ruleAppVersion = ruleAppField.getText();
                String ruleSetVersion = ruleSetField.getText();
                String testingVersion = testVerField.getText();
                String apiType = (String) apiComboBox.getSelectedItem();

                // Validate inputs
                if (sfdcCaseNumber.isEmpty() || ruleAppVersion.isEmpty() || ruleSetVersion.isEmpty() || testingVersion.isEmpty() || apiType.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "All fields are mandatory for regression testing.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Confirm with the user
                int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to perform regression testing? This will process JSON files against both PROD and QA environments.", "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Run the regression testing in a separate thread to keep the UI responsive
                new Thread(() -> {
                    RegressionTester.performRegressionTesting(sfdcCaseNumber, ruleAppVersion, ruleSetVersion, testingVersion, apiType);
                    // Optionally, update UI components or display a message when done
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Regression testing completed."));
                }).start();
            }
        });

        // Refresh source file count periodically
        Timer timer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSourceFileCount();
                updateCompareSourceFileCount();
            }
        });
        timer.start();

        frame.pack();
        frame.setVisible(true);
    }

	private static void updateSourceFileCount() {
		try {
			long fileCount = Files.list(Paths.get("C:/Development/CRDTesting/JSONRequestFilesFolder/"))
					.filter(path -> !Files.isDirectory(path))
					.filter(path -> !path.getFileName().toString().startsWith("~$")).count();
			sourceFileCountLabel.setText(String.valueOf(fileCount));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateCompareSourceFileCount() {
		try {
			long fileCount = Files.list(Paths.get("C:/Development/CRDTesting/Compare/SourceFiles"))
					.filter(path -> !Files.isDirectory(path))
					.filter(path -> !path.getFileName().toString().startsWith("~$")).count();
			compareSourceFileCountLabel.setText(String.valueOf(fileCount));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized void incrementGeneratedFileCount() {
		generatedFileCount++;
		updateGeneratedFileCount();
	}

	public static synchronized void incrementCompareGeneratedFileCount() {
		compareGeneratedFileCount++;
		updateCompareGeneratedFileCount();
	}

	private static void updateGeneratedFileCount() {
		SwingUtilities.invokeLater(() -> generatedFileCountLabel.setText(String.valueOf(generatedFileCount)));
	}

	private static void updateCompareGeneratedFileCount() {
		SwingUtilities.invokeLater(() -> compareGeneratedFileCountLabel.setText(String.valueOf(compareGeneratedFileCount)));
	}

}
