package com.JSONtoExcelApplication;

import javax.swing.*;

import com.JSONtoExcelApplication.ExcelFileComparator.ProgressCallback;
import com.JSONtoExcelApplication.RegressionTester;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        // [Your existing code for setting up the UI components remains unchanged]

        // ... [UI setup code]

        // Action listener for compareExcelButton
        compareExcelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.setText("");

                String outputDirPath = "C:/Development/CRDTesting/Compare/ComparisonOutput";

                if (selectedSourceFile != null && selectedGeneratedFile != null) {
                    // Both files are selected, compare them directly
                    compareGeneratedFileCount = 0; // Reset the count
                    updateCompareGeneratedFileCount(); // Update the UI

                    executor = Executors.newSingleThreadExecutor();
                    futureTask = executor.submit(() -> {
                        try {
                            ExcelFileComparator.ProgressCallback callback = message -> SwingUtilities.invokeLater(() -> console.append(message + "\n"));

                            ExcelFileComparator.compareSpecificFiles(selectedSourceFile, selectedGeneratedFile, outputDirPath, callback);
                            SwingUtilities.invokeLater(() -> console.append("Comparison completed. Output files created in: " + outputDirPath + "\n"));
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                console.append("Error during comparison: " + ex.getMessage() + "\n");
                                ex.printStackTrace();
                            });
                        }
                    });
                } else if (selectedSourceFile != null) {
                    // Only source file is selected, compare with matching generated files
                    String generatedDirPath = "C:/Development/CRDTesting/ResponseJSONtoExcel/";

                    compareGeneratedFileCount = 0; // Reset the count
                    updateCompareGeneratedFileCount(); // Update the UI

                    executor = Executors.newSingleThreadExecutor();
                    futureTask = executor.submit(() -> {
                        try {
                            ExcelFileComparator.ProgressCallback callback = message -> SwingUtilities.invokeLater(() -> console.append(message + "\n"));

                            // Correctly call compareSpecificFiles with the required parameters
                            ExcelFileComparator.compareSpecificFiles(selectedSourceFile, null, generatedDirPath, outputDirPath, callback);
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

                    compareGeneratedFileCount = 0; // Reset the count
                    updateCompareGeneratedFileCount(); // Update the UI

                    executor = Executors.newSingleThreadExecutor();
                    futureTask = executor.submit(() -> {
                        try {
                            ExcelFileComparator.ProgressCallback callback = message -> SwingUtilities.invokeLater(() -> console.append(message + "\n"));

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

        // [Rest of your existing code remains unchanged]

        // ... [Other action listeners and methods]

    }

    // [Other methods in your class, such as updateSourceFileCount, remain unchanged]

    private static void updateSourceFileCount() {
        // Your existing implementation
    }

    public static void updateCompareSourceFileCount() {
        // Your existing implementation
    }

    public static synchronized void incrementGeneratedFileCount() {
        // Your existing implementation
    }

    public static synchronized void incrementCompareGeneratedFileCount() {
        // Your existing implementation
    }

    private static void updateGeneratedFileCount() {
        // Your existing implementation
    }

    private static void updateCompareGeneratedFileCount() {
        // Your existing implementation
    }

}
