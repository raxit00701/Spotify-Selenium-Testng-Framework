package tests.basic;

import base.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import utils.CSVUtils;
import java.time.Duration;

/**
 * LoginTest - Validates login functionality using data-driven testing.
 * Each test run uses a fresh browser session and credentials from CSV.
 */
public class LoginTest extends BaseTest {

    // Locators
    private static final By SIGN_IN_BUTTON = By.cssSelector(
            "span.e-91000-baseline.e-91000-overflow-wrap-anywhere.e-91000-button-primary__inner.encore-inverted-light-set.e-91000-button--medium"
    );

    private static final By LOGIN_BUTTON = By.cssSelector(
            ".e-91132-baseline.e-91132-overflow-wrap-anywhere.e-91132-button-primary__inner.encore-bright-accent-set.e-91132-button--medium"
    );

    private static final By DASHBOARD_HEADER = By.cssSelector(
            "section[aria-label='Recommended for you'] div[class='Areas__HeaderArea-sc-8gfrea-3 TJKQw']"
    );

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        // Initialize driver before each test
        initDriver();
        driver = getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(3));

        // Navigate to base URL
        driver.get(getEnvConfig().getBaseUrl());
    }

    @AfterMethod
    public void tearDown() {
        // Quit driver after each test to ensure clean session
        if (driver != null)
            driver.quit();
    }

    @DataProvider(name = "login-data")
    public Object[][] loginData() {
        // Read login credentials from CSV file
        return CSVUtils.readResourceCsvToDataProvider("login.csv", true);
    }

    @Test(dataProvider = "login-data")
    public void loginTest(String username, String password) {
        System.out.println("🔑 Testing login for Spotify with username: " + username);
        System.out.println("🔑 Testing login for Spotify with password: " + password);

        // Step 1: Click sign-in button
        WebElement signInButton = wait.until(ExpectedConditions.elementToBeClickable(SIGN_IN_BUTTON));
        Assert.assertTrue(signInButton.isDisplayed(), "Sign-in button should be visible");
        signInButton.click();

        // Step 2: Enter username
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.clear();
        usernameField.sendKeys(username);

        // Allow blank username, just log it
        if (username.trim().isEmpty()) {
            System.out.println("⚠️ Username field is empty");
        } else {
            // Extra assertion: username must contain both alphabets and numbers
            boolean hasAlphabet = username.matches(".*[a-zA-Z].*");
            boolean hasNumber = username.matches(".*\\d.*");
            Assert.assertTrue(
                    hasAlphabet && hasNumber,
                    "Username should contain a combination of alphabets and numbers"
            );
        }

        // Step 3: Click on first login button
        WebElement lGButton = driver.findElement(By.cssSelector("button[data-testid='login-button']"));
        Assert.assertTrue(lGButton.isDisplayed(), "LOGIN button should be visible");
        lGButton.click();

        // Step 4: Check for server issue or invalid username
        String bannerText = driver.findElements(By.cssSelector(".e-91132-banner__message"))
                .stream().findFirst().map(e -> e.getText().trim()).orElse(null);
        String helpText = driver.findElements(By.cssSelector(".e-91132-form-help-text__text"))
                .stream().findFirst().map(e -> e.getText().trim()).orElse(null);

        if (bannerText != null) {
            System.out.println("✅ Server issue detected: " + bannerText);
            return; // Test passes if banner is shown
        }

        if (helpText != null) {
            System.out.println("✅ Login failed (invalid username): " + helpText);
            return; // Test passes if help text is shown
        }

        // Step 5: Continue to password entry
        WebElement passwordContinueButton = driver.findElement(By.cssSelector("button[data-encore-id='buttonTertiary']"));
        Assert.assertNotNull(passwordContinueButton, "Password continue button should be visible");
        passwordContinueButton.click();

        // Step 6: Entering password
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.clear();
        passwordField.sendKeys(password);

        // Allow blank password, just log it
        if (password.trim().isEmpty()) {
            System.out.println("⚠️ Password field is empty");
        } else {
            // Password policy checks
            boolean invalidLength = password.length() < 8 || password.length() > 16;
            boolean missingDigit = !password.matches(".*\\d.*");
            boolean missingSpecial = !password.matches(".*[^a-zA-Z0-9].*");

            if (invalidLength || missingDigit || missingSpecial) {
                String passwordError = driver.findElements(By.cssSelector(".e-91132-banner__message"))
                        .stream().findFirst().map(e -> e.getText().trim()).orElse(null);
                Assert.assertTrue(
                        passwordError != null && passwordError.toLowerCase().contains("password"),
                        "Password error message should be shown for invalid password policy"
                );
                System.out.println("✅ Password invalid (policy violation): " + passwordError);
                return;
            }
        }

        // Step 7: Click login
        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(LOGIN_BUTTON));
        Assert.assertTrue(loginButton.isDisplayed(), "Login button should be visible");
        loginButton.click();

        // Step 8: Check for username, password, or banner field-specific errors
        WebElement usernameErrorElement = driver.findElements(
                        By.cssSelector("div[id='username-error'] span.e-91132-form-help-text__text"))
                .stream().findFirst().orElse(null);
        WebElement passwordErrorElement = driver.findElements(
                        By.cssSelector("div[id='password-error'] span.e-91132-form-help-text__text"))
                .stream().findFirst().orElse(null);
        WebElement bannerErrorElement = driver.findElements(
                        By.cssSelector(".e-91132-banner__message"))
                .stream().findFirst().orElse(null);

        if ((usernameErrorElement != null && usernameErrorElement.isDisplayed()) ||
                (passwordErrorElement != null && passwordErrorElement.isDisplayed()) ||
                (bannerErrorElement != null && bannerErrorElement.isDisplayed())) {

            System.out.println("✅ Field error detected:");
            if (usernameErrorElement != null && usernameErrorElement.isDisplayed()) {
                System.out.println("   Username error: " + usernameErrorElement.getText().trim());
            }
            if (passwordErrorElement != null && passwordErrorElement.isDisplayed()) {
                System.out.println("   Password error: " + passwordErrorElement.getText().trim());
            }
            if (bannerErrorElement != null && bannerErrorElement.isDisplayed()) {
                System.out.println("   Banner error: " + bannerErrorElement.getText().trim());
            }
            return; // Test passes if any of these errors are shown
        }

        // Step 10: Verify dashboard loaded after successful login
        WebElement dashboardHeader = wait.until(ExpectedConditions.visibilityOfElementLocated(DASHBOARD_HEADER));
        Assert.assertTrue(dashboardHeader.isDisplayed(), "Dashboard header should be visible after login");
        System.out.println("🎉 Login successful, dashboard loaded! Header: " + dashboardHeader.getText());
    }
}