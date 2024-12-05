// Inside the regressionTestingButton ActionListener
new Thread(() -> {
    // Create a ProgressCallback to update the console
    ProgressCallback callback = message -> {
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
