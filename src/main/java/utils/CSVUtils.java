package utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced CSV Utility class for TestNG DataProviders
 * Supports both file system and classpath resources
 *perfectly in parallel runs*
 */
public final class CSVUtils {

    private CSVUtils() { /* utility class - prevent instantiation */ }

    /**
     * Reads a CSV file from the filesystem
     *
     * @param pathStr     Absolute or relative path to CSV file
     * @param skipHeader  true to skip first row
     * @return List of String[] (each array = one row)
     */
    public static List<String[]> readCsv(String pathStr, boolean skipHeader) {
        Path path = Paths.get(pathStr);
        List<String[]> data = new ArrayList<>();

        if (!Files.exists(path)) {
            System.err.println("[CSVUtils] File not found: " + path.toAbsolutePath());
            return data;
        }

        try (Reader reader = Files.newBufferedReader(path);
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

            System.out.println("[CSVUtils] Loaded " + data.size() + " data rows from file: " + path.getFileName());

        } catch (IOException | CsvValidationException e) {
            System.err.println("[CSVUtils] Error reading CSV file: " + pathStr + " | " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Reads a CSV file from src/test/resources (recommended for tests)
     * Works reliably in IDE, Maven, Gradle, and parallel execution
     *
     * @param resourceFileName e.g. "login.csv", "users/signup-data.csv"
     * @param skipHeader       true to skip header row
     * @return List of trimmed String arrays
     */
    public static List<String[]> readResourceCsv(String resourceFileName, boolean skipHeader) {
        try (Reader reader = new InputStreamReader(
                CSVUtils.class.getClassLoader().getResourceAsStream(resourceFileName),
                StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader).build()) {

            if (reader == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceFileName +
                        " — Make sure file is in src/test/resources");
            }

            List<String[]> data = new ArrayList<>();
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

            System.out.println("[CSVUtils] Loaded " + data.size() + " data rows from resource: " + resourceFileName);
            return data;

        } catch (Exception e) {
            System.err.println("[CSVUtils] Failed to load resource CSV: " + resourceFileName);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Directly returns Object[][] ready for @DataProvider (most convenient!)
     *
     * @param resourceFileName CSV in src/test/resources
     * @param skipHeader       true to skip header
     * @return Object[][] for TestNG DataProvider
     */
    public static Object[][] readResourceCsvToDataProvider(String resourceFileName, boolean skipHeader) {
        List<String[]> rows = readResourceCsv(resourceFileName, skipHeader);
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