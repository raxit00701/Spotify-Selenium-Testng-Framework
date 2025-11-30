package base;

import env.EnvConfig;
import env.ProdEnv;
import env.PreProdEnv;
import env.TestEnv;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeOptions;

import java.time.Duration;
import java.util.Locale;

public class BaseTest {
    protected EnvConfig envConfig;
    protected WebDriver driver;
    protected String browserName;

    public BaseTest() {
        initEnvironment();
    }

    protected void initEnvironment() {
        String envName = System.getProperty("env", "test").trim().toLowerCase(Locale.ROOT);
        switch (envName) {
            case "prod":
            case "production":
                envConfig = new ProdEnv();
                break;
            case "preprod":
            case "pre-prod":
            case "staging":
                envConfig = new PreProdEnv();
                break;
            case "test":
            default:
                envConfig = new TestEnv();
                break;
        }

        // allow overrides from system properties
        envConfig.applySystemOverrides();
    }

    @BeforeClass(alwaysRun = true)
    @Parameters({"browser", "headless"})
    public void setUpBrowser(@Optional String browser, @Optional String headless) {
        if (browser != null && !browser.isEmpty()) {
            this.browserName = browser;
            System.out.println("🔧 Browser parameter from XML: " + browser);
        }

        if (headless != null && !headless.isEmpty()) {
            System.setProperty("headless", headless);
            System.out.println("🔧 Headless parameter from XML: " + headless);
        }
    }

    /**
     * Initializes WebDriver according to the envConfig.browser value.
     * Supported browser values: chrome, firefox, edge
     */
    public void initDriver() {
        String browser = (browserName != null) ? browserName : envConfig.getBrowser();
        browser = browser.toLowerCase(Locale.ROOT).trim();

        System.out.println("🚀 Initializing browser: " + browser);

        switch (browser) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (envConfig.isHeadless()) {
                    firefoxOptions.addArguments("--headless");
                }
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                if (envConfig.isHeadless()) {
                    edgeOptions.addArguments("--headless=new");
                }
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                if (envConfig.isHeadless()) {
                    chromeOptions.addArguments("--headless=new");
                }
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        // default timeouts and window settings (tweak if needed)
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(envConfig.getImplicitWaitSeconds()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(envConfig.getPageLoadTimeoutSeconds()));
    }

    public WebDriver getDriver() {
        return driver;
    }

    public EnvConfig getEnvConfig() {
        return envConfig;
    }

    public void openBaseUrl() {
        driver.get(envConfig.getBaseUrl());
    }

    @AfterClass(alwaysRun = true)
    public void quitDriver() {
        if (driver != null) {
            try {
                driver.quit();
                System.out.println("✅ Browser closed successfully");
            } catch (Exception ignored) {}
        }
    }

    // Hooks to extend
    protected void beforeTest() {}
    protected void afterTest() {}
}