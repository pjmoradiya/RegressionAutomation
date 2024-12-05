package com.JSONtoExcelApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class UserInputUI {
    // Existing variables...

    public static void display() {
        // Existing UI code...

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
                    // Create a ProgressCallback to update the console
                    ExcelFileComparator.ProgressCallback callback = message -> {
                        SwingUtilities.invokeLater(() -> {
                            console.append(message + "\n");
                            console.setCaretPosition(console.getDocument().getLength());
                        });
                    };

                    RegressionTester.performRegressionTesting(sfdcCaseNumber, ruleAppVersion, ruleSetVersion,
                                                              testingVersion, apiType, callback);
                    // Optionally, update UI components or display a message when done
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Regression testing completed."));
                }).start();
            }
        });

        // Existing code...
    }

    // Existing methods...
}
