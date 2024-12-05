package com.JSONtoExcelApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

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

        // [Existing UI code remains unchanged]

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
                if (sfdcCaseNumber.isEmpty() || ruleAppVersion.isEmpty() || ruleSetVersion.isEmpty()
                        || testingVersion.isEmpty() || apiType.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "All fields are mandatory for regression testing.", "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Confirm with the user
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to perform regression testing? This will process JSON files against both PROD and QA environments.",
                        "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }

                // Run the regression testing in a separate thread to keep the UI responsive
                new Thread(() -> {
                    RegressionTester.performRegressionTesting(sfdcCaseNumber, ruleAppVersion, ruleSetVersion,
                                                              testingVersion, apiType);
                    // Optionally, update UI components or display a message when done
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Regression testing completed."));
                }).start();
            }
        });

        // [Rest of your existing code remains unchanged]
    }

    // [Other methods in your class, such as updateSourceFileCount, remain unchanged]

    public static synchronized void incrementGeneratedFileCount() {
        generatedFileCount++;
        updateGeneratedFileCount();
    }

    private static void updateGeneratedFileCount() {
        SwingUtilities.invokeLater(() -> generatedFileCountLabel.setText(String.valueOf(generatedFileCount)));
    }

    // [Other methods remain unchanged]
}
