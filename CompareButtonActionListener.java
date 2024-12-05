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
