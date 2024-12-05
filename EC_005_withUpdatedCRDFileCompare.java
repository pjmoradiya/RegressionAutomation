private static String extractFileNamePart(String fileName) {
    // Remove the extension
    if (fileName.endsWith(".xlsx")) {
        fileName = fileName.substring(0, fileName.length() - 5);
    }

    // Split the filename by "_"
    String[] parts = fileName.split("_");

    // Find the index of "CRD"
    int crdIndex = -1;
    for (int i = 0; i < parts.length; i++) {
        if (parts[i].equals("CRD")) {
            crdIndex = i;
            break;
        }
    }

    if (crdIndex >= 0 && crdIndex + 1 < parts.length) {
        // Find the date index by looking for an 8-digit number (mmddyyyy)
        int dateIndex = -1;
        for (int i = crdIndex + 1; i < parts.length; i++) {
            if (parts[i].matches("\\d{8}")) {
                dateIndex = i;
                break;
            }
        }

        // Set the end index for the filename parts
        int endIndex = (dateIndex != -1) ? dateIndex : parts.length - 1;

        // Check if the next part after date is "v###"
        if (dateIndex != -1 && endIndex + 1 < parts.length && parts[endIndex + 1].matches("v\\d+")) {
            endIndex = endIndex; // dateIndex is correct
        } else {
            // If date is not found or "v###" is not after date, try to find "v###" directly
            for (int i = crdIndex + 1; i < parts.length; i++) {
                if (parts[i].matches("v\\d+")) {
                    endIndex = i - 1;
                    break;
                }
            }
        }

        // Reconstruct the <FileName> by joining parts from crdIndex+1 to endIndex
        StringBuilder fileNameBuilder = new StringBuilder();
        for (int i = crdIndex + 1; i <= endIndex; i++) {
            if (fileNameBuilder.length() > 0) {
                fileNameBuilder.append("_");
            }
            fileNameBuilder.append(parts[i]);
        }
        return fileNameBuilder.toString();
    }

    // If pattern does not match, return the filename without extension
    return fileName;
}
