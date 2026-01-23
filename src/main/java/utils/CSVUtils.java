package utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV Utility class for TestNG DataProviders
 * Reads CSV files ONLY from:
 * C:\Users\raxit\IdeaProjects\selenium2\src\test\resources
 *
 * Safe for IntelliJ, TestNG, Maven, CI, and parallel runs
 */
public final class CSVUtils {

    private CSVUtils() {
        /* utility class - prevent instantiation */
    }

    /**
     * Base directory for all CSV files
     */
    private static final Path TEST_RESOURCES_DIR =
            Paths.get("C:/Users/raxit/IdeaProjects/selenium2/src/test/resources");

    /**
     * Reads a CSV file from the fixed test resources directory
     *
     * @param fileName   CSV file name or subpath (e.g. "login.csv", "data/users.csv")
     * @param skipHeader true to skip first row
     * @return List of String[] (each array = one row)
     */
    public static List<String[]> readResourceCsv(String fileName, boolean skipHeader) {

        Path csvPath = TEST_RESOURCES_DIR.resolve(fileName).normalize();
        List<String[]> data = new ArrayList<>();

        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException(
                    "[CSVUtils] CSV file not found: " + csvPath
            );
        }

        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            String[] row;
            boolean isFirstRow = true;

            while ((row = csvReader.readNext()) != null) {
                if (skipHeader && isFirstRow) {
                    isFirstRow = false;
                    continue;
                }
                data.add(trimRow(row));
                isFirstRow = false;
            }

            System.out.println("[CSVUtils] Loaded " + data.size() +
                    " rows from: " + csvPath.getFileName());

            return data;

        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(
                    "[CSVUtils] Failed to read CSV: " + csvPath, e
            );
        }
    }

    /**
     * Directly returns Object[][] ready for TestNG @DataProvider
     *
     * @param fileName   CSV file name or subpath
     * @param skipHeader true to skip header row
     * @return Object[][] for TestNG DataProvider
     */
    public static Object[][] readResourceCsvToDataProvider(String fileName, boolean skipHeader) {
        List<String[]> rows = readResourceCsv(fileName, skipHeader);
        Object[][] result = new Object[rows.size()][];
        for (int i = 0; i < rows.size(); i++) {
            result[i] = rows.get(i);
        }
        return result;
    }

    /**
     * Trims all values in a row and converts null → empty string
     */
    private static String[] trimRow(String[] row) {
        if (row == null) return new String[0];
        String[] trimmed = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            trimmed[i] = row[i] == null ? "" : row[i].trim();
        }
        return trimmed;
    }

    /**
     * Generic helper: convert List<String[]> → Object[][]
     */
    public static Object[][] toDataProvider(List<String[]> csvData) {
        Object[][] data = new Object[csvData.size()][];
        for (int i = 0; i < csvData.size(); i++) {
            data[i] = csvData.get(i);
        }
        return data;
    }
}
