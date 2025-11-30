package listeners;

import base.BaseTest;
import env.EnvConfig;
import io.qameta.allure.Allure;
import org.monte.media.Format;
import org.monte.media.FormatKeys;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

/**
 * TestNG Listener that captures screenshots, videos, and logs ONLY on test failure
 * and attaches them to Allure reports.
 */
public class TestListener implements ITestListener {

    private static final String ALLURE_RESULTS_FOLDER = "C:\\Users\\raxit\\IdeaProjects\\selenium2\\target\\allure-results";
    private static final String ALLURE_REPORT_FOLDER = "C:\\Users\\raxit\\IdeaProjects\\selenium2\\target\\site\\allure-maven-plugin";
    private static final String VIDEO_FOLDER = "C:\\Users\\raxit\\IdeaProjects\\selenium2\\target\\allure-results\\videos";
    private static final String SCREENSHOT_FOLDER = "C:\\Users\\raxit\\IdeaProjects\\selenium2\\target\\allure-results\\screenshots";
    private static final String LOG_FOLDER = "C:\\Users\\raxit\\IdeaProjects\\selenium2\\target\\allure-results\\logs";

    private ScreenRecorder screenRecorder;
    private String currentTestName;
    private boolean testFailed = false;
    private boolean isHeadlessMode = false;

    @Override
    public void onStart(ITestContext context) {
        System.out.println("========================================");
        System.out.println("📊 Test Suite Started: " + context.getName());
        System.out.println("========================================");

        // Detect headless mode
        detectHeadlessMode(context);

        // Create necessary directories
        createDirectories();

        // Copy history from previous report (for trends)
        copyAllureHistory();

        // Create environment properties for Allure dashboard
        createEnvironmentProperties(context);
    }

    @Override
    public void onTestStart(ITestResult result) {
        currentTestName = getTestName(result);
        testFailed = false;
        System.out.println("\n▶️  Starting Test: " + currentTestName);

        // Start video recording only if NOT in headless mode
        if (!isHeadlessMode) {
            startVideoRecording(currentTestName);
        } else {
            System.out.println("🎥 Video recording disabled (headless mode detected)");
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("✅ Test Passed: " + getTestName(result));

        // Stop and DELETE video for passed tests
        stopVideoRecording(true);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        testFailed = true;
        System.out.println("❌ Test Failed: " + getTestName(result));

        WebDriver driver = getDriverFromTest(result);
        String testName = getTestName(result);

        if (driver != null) {
            // 1. Capture Screenshot
            captureScreenshot(driver, testName);

            // 2. Capture ALL Logs (Browser Console, TestNG, Custom Application)
            captureBrowserLogs(driver, testName);
            captureTestNGLogs(result, testName);

            // 3. Stop video recording and KEEP it
            stopVideoRecording(false);
        } else {
            System.out.println("⚠️  WebDriver is null, cannot capture screenshot or logs");
            stopVideoRecording(false);
        }

        // 4. Attach exception details
        attachExceptionDetails(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("⏭️  Test Skipped: " + getTestName(result));

        // Check if test was skipped due to an exception (broken test)
        if (result.getThrowable() != null) {
            System.out.println("⚠️  Test skipped due to exception - capturing artifacts");

            WebDriver driver = getDriverFromTest(result);
            String testName = getTestName(result);

            if (driver != null) {
                // Capture artifacts for broken tests
                captureScreenshot(driver, testName);
                captureBrowserLogs(driver, testName);
                captureTestNGLogs(result, testName);
                stopVideoRecording(false); // Keep video
            } else {
                System.out.println("⚠️  WebDriver is null, cannot capture artifacts");
                stopVideoRecording(false);
            }

            // Attach exception details
            attachExceptionDetails(result);
        } else {
            // Delete video for normally skipped tests
            stopVideoRecording(true);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("\n========================================");
        System.out.println("📊 Test Suite Finished: " + context.getName());
        System.out.println("✅ Passed: " + context.getPassedTests().size());
        System.out.println("❌ Failed: " + context.getFailedTests().size());
        System.out.println("⏭️  Skipped: " + context.getSkippedTests().size());
        System.out.println("========================================\n");

        // Ensure history directory exists in allure-results for next run
        createHistoryPlaceholder();
    }

    // ==================== ENVIRONMENT PROPERTIES ====================

    /**
     * Detects if tests are running in headless mode
     */
    private void detectHeadlessMode(ITestContext context) {
        try {
            // Check system property
            String headlessProperty = System.getProperty("headless");
            if ("true".equalsIgnoreCase(headlessProperty)) {
                isHeadlessMode = true;
                System.out.println("🔍 Headless mode detected via system property");
                return;
            }

            // Try to get from BaseTest instance
            if (context.getAllTestMethods().length > 0) {
                Object[] instances = context.getAllTestMethods()[0].getTestClass().getInstances(true);
                if (instances != null && instances.length > 0) {
                    Object testInstance = instances[0];
                    if (testInstance instanceof BaseTest) {
                        BaseTest baseTest = (BaseTest) testInstance;
                        EnvConfig envConfig = baseTest.getEnvConfig();
                        if (envConfig != null && envConfig.isHeadless()) {
                            isHeadlessMode = true;
                            System.out.println("🔍 Headless mode detected via EnvConfig");
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If detection fails, assume non-headless
            System.out.println("⚠️  Could not detect headless mode, assuming GUI mode");
        }
    }

    /**
     * Copies history folder from previous Allure report to current results
     * This enables trend graphs and historical data in Allure reports
     */
    private void copyAllureHistory() {
        try {
            Path mavenPluginHistory = Paths.get(ALLURE_REPORT_FOLDER, "history");
            Path resultsHistory = Paths.get(ALLURE_RESULTS_FOLDER, "history");

            System.out.println("📊 Looking for Allure history...");
            System.out.println("   Checking: " + mavenPluginHistory);

            // Check if Maven plugin history exists and has files
            if (Files.exists(mavenPluginHistory) && Files.isDirectory(mavenPluginHistory)) {
                File[] historyFiles = mavenPluginHistory.toFile().listFiles();

                if (historyFiles != null && historyFiles.length > 0) {
                    System.out.println("   Found " + historyFiles.length + " history files");

                    // Delete old history in results
                    if (Files.exists(resultsHistory)) {
                        deleteDirectory(resultsHistory.toFile());
                    }

                    // Copy history
                    copyDirectory(mavenPluginHistory.toFile(), resultsHistory.toFile());

                    System.out.println("✅ Allure history copied successfully!");
                    System.out.println("   History files preserved for trend graphs");
                    return;
                }
            }

            System.out.println("⚠️  No previous Allure history found");
            System.out.println("   After generating report with 'mvn allure:report', run tests again");

        } catch (Exception e) {
            System.err.println("❌ Failed to copy Allure history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Recursively copies a directory
     */
    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    Files.copy(file.toPath(), destFile.toPath());
                }
            }
        }
    }

    /**
     * Recursively deletes a directory
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * Creates a placeholder in history directory to ensure it's tracked
     * This helps allure:serve maintain history between runs
     */
    private void createHistoryPlaceholder() {
        try {
            Path historyDir = Paths.get(ALLURE_RESULTS_FOLDER, "history");
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir);
            }

            // Create a marker file
            Path markerFile = historyDir.resolve("history-marker.txt");
            String marker = "Allure History Marker\nCreated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            Files.write(markerFile, marker.getBytes());

            System.out.println("📊 History directory prepared for next run");
        } catch (Exception e) {
            // Silent fail - not critical
        }
    }

    /**
     * Creates environment.properties file for Allure report dashboard
     */
    private void createEnvironmentProperties(ITestContext context) {
        try {
            // Get the first test instance to access BaseTest
            Object testInstance = null;

            // Try to get instance from test methods
            if (context.getAllTestMethods().length > 0) {
                Object[] instances = context.getAllTestMethods()[0].getTestClass().getInstances(true);
                if (instances != null && instances.length > 0) {
                    testInstance = instances[0];
                }
            }

            if (testInstance instanceof BaseTest) {
                BaseTest baseTest = (BaseTest) testInstance;
                EnvConfig envConfig = baseTest.getEnvConfig();
                String browser = envConfig.getBrowser();

                // Create properties content
                StringBuilder properties = new StringBuilder();
                properties.append("# Allure Environment Information\n");
                properties.append("# Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");

                // Environment Details
                properties.append("Environment=").append(getEnvironmentName(envConfig)).append("\n");
                properties.append("Base.URL=").append(envConfig.getBaseUrl()).append("\n");
                properties.append("Browser=").append(browser.toUpperCase()).append("\n");
                properties.append("Headless.Mode=").append(envConfig.isHeadless() ? "Yes" : "No").append("\n");
                properties.append("Implicit.Wait=").append(envConfig.getImplicitWaitSeconds()).append(" seconds\n");
                properties.append("Page.Load.Timeout=").append(envConfig.getPageLoadTimeoutSeconds()).append(" seconds\n");

                // System Information
                properties.append("\n# System Information\n");
                properties.append("OS=").append(System.getProperty("os.name")).append("\n");
                properties.append("OS.Version=").append(System.getProperty("os.version")).append("\n");
                properties.append("Java.Version=").append(System.getProperty("java.version")).append("\n");
                properties.append("User=").append(System.getProperty("user.name")).append("\n");

                // Test Suite Information
                properties.append("\n# Test Suite Information\n");
                properties.append("Suite.Name=").append(context.getName()).append("\n");
                properties.append("Total.Tests=").append(context.getAllTestMethods().length).append("\n");

                // Write to file
                Path envPropertiesPath = Paths.get(ALLURE_RESULTS_FOLDER, "environment.properties");
                Files.write(envPropertiesPath, properties.toString().getBytes());

                System.out.println("🌐 Environment properties file created for Allure dashboard");
            }
        } catch (Exception e) {
            System.err.println("⚠️  Failed to create environment.properties: " + e.getMessage());
            // Don't fail the test if environment file creation fails
        }
    }

    /**
     * Determines environment name from EnvConfig instance
     */
    private String getEnvironmentName(EnvConfig envConfig) {
        String className = envConfig.getClass().getSimpleName();
        if (className.contains("Prod") && !className.contains("PreProd")) {
            return "PRODUCTION";
        } else if (className.contains("PreProd")) {
            return "PRE-PRODUCTION";
        } else {
            return "TEST";
        }
    }

    // ==================== SCREENSHOT CAPTURE ====================

    private void captureScreenshot(WebDriver driver, String testName) {
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // Save to file
            String fileName = testName + "_" + getTimestamp() + ".png";
            Path screenshotPath = Paths.get(SCREENSHOT_FOLDER, fileName);
            Files.write(screenshotPath, screenshot);

            // Attach to Allure - try/catch to prevent errors
            try {
                Allure.addAttachment("Screenshot - " + testName, "image/png",
                        new ByteArrayInputStream(screenshot), "png");
            } catch (Exception allureEx) {
                // Allure lifecycle not ready, but file is saved
                System.out.println("⚠️  Allure attachment skipped (file saved locally)");
            }

            System.out.println("📸 Screenshot captured: " + fileName);
        } catch (Exception e) {
            System.err.println("❌ Failed to capture screenshot: " + e.getMessage());
        }
    }

    // ==================== VIDEO RECORDING ====================

    private void startVideoRecording(String testName) {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();

            File videoFolder = new File(VIDEO_FOLDER);

            this.screenRecorder = new SpecializedScreenRecorder(
                    gc,
                    gc.getBounds(),
                    new Format(MediaTypeKey, FormatKeys.MediaType.FILE, MimeTypeKey, MIME_AVI),
                    new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                            DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                            QualityKey, 1.0f,
                            KeyFrameIntervalKey, 15 * 60),
                    new Format(MediaTypeKey, FormatKeys.MediaType.VIDEO, EncodingKey, "black", FrameRateKey, Rational.valueOf(30)),
                    null,
                    videoFolder,
                    testName
            );

            screenRecorder.start();
            System.out.println("🎥 Video recording started for: " + testName);
        } catch (Exception e) {
            System.err.println("❌ Failed to start video recording: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopVideoRecording(boolean deleteVideo) {
        if (screenRecorder != null) {
            try {
                screenRecorder.stop();

                List<File> createdMovies = screenRecorder.getCreatedMovieFiles();

                if (!createdMovies.isEmpty()) {
                    File videoFile = createdMovies.get(0);

                    if (deleteVideo) {
                        // Delete video for passed/skipped tests
                        if (videoFile.delete()) {
                            System.out.println("🗑️  Video deleted (test passed/skipped)");
                        }
                    } else {
                        // Keep and attach video to Allure for failed tests
                        try {
                            byte[] videoBytes = Files.readAllBytes(videoFile.toPath());
                            Allure.addAttachment("Video Recording - " + currentTestName, "video/avi",
                                    new ByteArrayInputStream(videoBytes), "avi");
                            System.out.println("🎥 Video saved and attached to Allure: " + videoFile.getName());
                        } catch (IOException e) {
                            System.err.println("❌ Failed to attach video to Allure: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to stop video recording: " + e.getMessage());
            } finally {
                screenRecorder = null;
            }
        }
    }

    // ==================== LOG CAPTURE ====================

    /**
     * Captures Browser Console Logs (errors, warnings, info)
     */
    private void captureBrowserLogs(WebDriver driver, String testName) {
        try {
            LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("╔═══════════════════════════════════════════════════════════════╗\n");
            logBuilder.append("║           BROWSER CONSOLE LOGS                                ║\n");
            logBuilder.append("║   Test: ").append(String.format("%-50s", testName)).append("║\n");
            logBuilder.append("╚═══════════════════════════════════════════════════════════════╝\n\n");

            int errorCount = 0;
            int warningCount = 0;
            int infoCount = 0;

            for (LogEntry entry : logEntries) {
                String logLevel = entry.getLevel().toString();
                String logMessage = entry.getMessage();

                logBuilder.append("[").append(String.format("%-7s", entry.getLevel())).append("] ")
                        .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(entry.getTimestamp())))
                        .append(" - ").append(logMessage).append("\n");

                if (entry.getLevel().equals(Level.SEVERE)) errorCount++;
                else if (entry.getLevel().equals(Level.WARNING)) warningCount++;
                else infoCount++;
            }

            logBuilder.append("\n╔═══════════════════════════════════════════════════════════════╗\n");
            logBuilder.append("║   SUMMARY                                                     ║\n");
            logBuilder.append("╠═══════════════════════════════════════════════════════════════╣\n");
            logBuilder.append("║   Total Logs: ").append(String.format("%-46s", logEntries.getAll().size())).append("║\n");
            logBuilder.append("║   🔴 Errors: ").append(String.format("%-47s", errorCount)).append("║\n");
            logBuilder.append("║   🟡 Warnings: ").append(String.format("%-45s", warningCount)).append("║\n");
            logBuilder.append("║   🔵 Info: ").append(String.format("%-49s", infoCount)).append("║\n");
            logBuilder.append("╚═══════════════════════════════════════════════════════════════╝\n");

            String logContent = logBuilder.toString();

            // Save to file
            String fileName = testName + "_browser_" + getTimestamp() + ".log";
            Path logPath = Paths.get(LOG_FOLDER, fileName);
            Files.write(logPath, logContent.getBytes());

            // Attach to Allure
            try {
                Allure.addAttachment("Browser Console Logs - " + testName, "text/plain", logContent, "log");
            } catch (Exception allureEx) {
                System.out.println("⚠️  Allure browser log attachment skipped (file saved locally)");
            }

            System.out.println("📝 Browser logs captured: " + fileName);
            System.out.println("   🔴 Errors: " + errorCount + " | 🟡 Warnings: " + warningCount + " | 🔵 Info: " + infoCount);
        } catch (Exception e) {
            System.err.println("❌ Failed to capture browser logs: " + e.getMessage());
        }
    }

    /**
     * Captures TestNG execution logs and test context information
     */
    private void captureTestNGLogs(ITestResult result, String testName) {
        try {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("╔═══════════════════════════════════════════════════════════════╗\n");
            logBuilder.append("║           TESTNG EXECUTION LOGS                               ║\n");
            logBuilder.append("║   Test: ").append(String.format("%-50s", testName)).append("║\n");
            logBuilder.append("╚═══════════════════════════════════════════════════════════════╝\n\n");

            // Test Information
            logBuilder.append("📋 Test Information:\n");
            logBuilder.append("   • Test Name: ").append(result.getMethod().getMethodName()).append("\n");
            logBuilder.append("   • Test Class: ").append(result.getTestClass().getName()).append("\n");
            logBuilder.append("   • Status: ").append(getStatusString(result.getStatus())).append("\n");
            logBuilder.append("   • Start Time: ").append(new Date(result.getStartMillis())).append("\n");
            logBuilder.append("   • End Time: ").append(new Date(result.getEndMillis())).append("\n");
            logBuilder.append("   • Duration: ").append((result.getEndMillis() - result.getStartMillis()) / 1000.0).append(" seconds\n");

            // Parameters
            if (result.getParameters().length > 0) {
                logBuilder.append("\n📌 Test Parameters:\n");
                Object[] params = result.getParameters();
                for (int i = 0; i < params.length; i++) {
                    logBuilder.append("   • Parameter[").append(i).append("]: ").append(params[i]).append("\n");
                }
            }

            // TestNG Reporter Logs
            List<String> reporterMessages = org.testng.Reporter.getOutput(result);
            if (!reporterMessages.isEmpty()) {
                logBuilder.append("\n📢 Reporter Messages:\n");
                for (String message : reporterMessages) {
                    logBuilder.append("   • ").append(message).append("\n");
                }
            }

            // Test Context
            logBuilder.append("\n🌐 Test Context:\n");
            logBuilder.append("   • Suite Name: ").append(result.getTestContext().getSuite().getName()).append("\n");
            logBuilder.append("   • Test Name: ").append(result.getTestContext().getName()).append("\n");
            logBuilder.append("   • Host: ").append(result.getHost() != null ? result.getHost() : "N/A").append("\n");

            String logContent = logBuilder.toString();

            // Save to file
            String fileName = testName + "_testng_" + getTimestamp() + ".log";
            Path logPath = Paths.get(LOG_FOLDER, fileName);
            Files.write(logPath, logContent.getBytes());

            // Attach to Allure
            try {
                Allure.addAttachment("TestNG Logs - " + testName, "text/plain", logContent, "log");
            } catch (Exception allureEx) {
                System.out.println("⚠️  Allure TestNG log attachment skipped (file saved locally)");
            }

            System.out.println("📝 TestNG logs captured: " + fileName);
        } catch (Exception e) {
            System.err.println("❌ Failed to capture TestNG logs: " + e.getMessage());
        }
    }

    /**
     * Attaches detailed exception information to Allure report
     */
    private void attachExceptionDetails(ITestResult result) {
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            StringBuilder exceptionDetails = new StringBuilder();
            exceptionDetails.append("╔═══════════════════════════════════════════════════════════════╗\n");
            exceptionDetails.append("║           EXCEPTION DETAILS                                   ║\n");
            exceptionDetails.append("╚═══════════════════════════════════════════════════════════════╝\n\n");

            exceptionDetails.append("🔴 Exception Type: ").append(throwable.getClass().getName()).append("\n");
            exceptionDetails.append("💬 Message: ").append(throwable.getMessage()).append("\n\n");

            exceptionDetails.append("📚 Stack Trace:\n");
            exceptionDetails.append("─────────────────────────────────────────────────────────────────\n");
            for (StackTraceElement element : throwable.getStackTrace()) {
                exceptionDetails.append("  at ").append(element.toString()).append("\n");
            }

            // Check for cause
            if (throwable.getCause() != null) {
                exceptionDetails.append("\n⚠️  Caused by: ").append(throwable.getCause().getClass().getName()).append("\n");
                exceptionDetails.append("💬 Cause Message: ").append(throwable.getCause().getMessage()).append("\n\n");
                exceptionDetails.append("📚 Cause Stack Trace:\n");
                exceptionDetails.append("─────────────────────────────────────────────────────────────────\n");
                for (StackTraceElement element : throwable.getCause().getStackTrace()) {
                    exceptionDetails.append("  at ").append(element.toString()).append("\n");
                }
            }

            try {
                Allure.addAttachment("Exception Details - " + getTestName(result), "text/plain",
                        exceptionDetails.toString(), "txt");
            } catch (Exception allureEx) {
                System.out.println("⚠️  Allure exception attachment skipped (details logged)");
            }

            System.out.println("📝 Exception details attached to Allure report");
        }
    }

    // ==================== UTILITY METHODS ====================

    private WebDriver getDriverFromTest(ITestResult result) {
        Object testInstance = result.getInstance();
        if (testInstance instanceof BaseTest) {
            return ((BaseTest) testInstance).getDriver();
        }
        return null;
    }

    private String getTestName(ITestResult result) {
        return result.getMethod().getMethodName();
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    private String getStatusString(int status) {
        switch (status) {
            case ITestResult.SUCCESS: return "✅ SUCCESS";
            case ITestResult.FAILURE: return "❌ FAILURE";
            case ITestResult.SKIP: return "⏭️  SKIPPED";
            default: return "❓ UNKNOWN";
        }
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(VIDEO_FOLDER));
            Files.createDirectories(Paths.get(SCREENSHOT_FOLDER));
            Files.createDirectories(Paths.get(LOG_FOLDER));
            System.out.println("📁 Artifact directories created/verified");
        } catch (IOException e) {
            System.err.println("❌ Failed to create directories: " + e.getMessage());
        }
    }

    // ==================== CUSTOM SCREEN RECORDER ====================

    /**
     * Custom ScreenRecorder to control video file naming
     */
    private static class SpecializedScreenRecorder extends ScreenRecorder {
        private String testName;

        public SpecializedScreenRecorder(GraphicsConfiguration cfg, Rectangle captureArea,
                                         Format fileFormat, Format screenFormat, Format mouseFormat,
                                         Format audioFormat, File movieFolder, String testName) throws IOException, AWTException {
            super(cfg, captureArea, fileFormat, screenFormat, mouseFormat, audioFormat, movieFolder);
            this.testName = testName;
        }

        @Override
        protected File createMovieFile(Format fileFormat) throws IOException {
            if (!movieFolder.exists()) {
                movieFolder.mkdirs();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            return new File(movieFolder, testName + "_" + dateFormat.format(new Date()) + ".avi");
        }
    }
}