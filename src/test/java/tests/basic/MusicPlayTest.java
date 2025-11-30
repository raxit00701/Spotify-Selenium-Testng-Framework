package tests.basic;
import pages.LoginPage;

import base.BaseTest;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.interactions.Actions;
import java.time.Duration;

public class MusicPlayTest extends BaseTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private LoginPage loginPage;





    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        initDriver(); // This method comes from BaseTest
        driver = getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        loginPage = new LoginPage(driver, wait);




        driver.manage().window().maximize();
        driver.get(getEnvConfig().getBaseUrl()); // e.g. https://open.spotify.com
        System.out.println("[MusicPlayTest] Opened Spotify: " + getEnvConfig().getBaseUrl());
    }

    @Test
    public void playMusicTest() throws InterruptedException {


        loginPage.clickSignIn();
        loginPage.enterUsername("banefo8720@bialode.com");
        loginPage.clickContinueAfterUsername();
        loginPage.clickPasswordContinue();
        loginPage.enterPassword("Password@12");
        loginPage.clickFinalLogin();

        Thread.sleep(10000);

        // ================== NEW STEPS START HERE ==================
// 1. Hover over the page title / "Home" header to make the "Hide Now Playing" button appear
        WebElement homeHeader = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[class='e-91000-text encore-text-title-small l3ePjQ6SwNdQQCnLpywl'] a[draggable='false']")));
        Actions actions = new Actions(driver);
        actions.moveToElement(homeHeader).perform();
        Thread.sleep(500); // small pause to ensure the button becomes visible
        // 2. Click the "Hide Now Playing view" button (the little downward chevron)
        WebElement hideNowPlayingButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[aria-label='Hide Now Playing view'] span[class='e-91000-button__icon-wrapper'] svg")));
        hideNowPlayingButton.click();
        Thread.sleep(1000); // short wait for the bar to collapse
        // 1. Click on the specific div → button (the "Close ad" or "Continue" button that appears after login)


        WebElement closeOrContinueButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("(//div[@role='button'])[15]")));
        closeOrContinueButton.click();
        Thread.sleep(3000); // wait 3 seconds

        // 2. Scroll down to the track/playlist that contains text "AZUL"
        WebElement azulElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(text(),'AZUL')]")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", azulElement);

        WebElement homeHeader2 = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(text(),'AZUL')]")));
        Actions actions1 = new Actions(driver);
        actions1.moveToElement(homeHeader2).perform();
        Thread.sleep(100);

        // 3. Click the Play button for "AZUL by Guru Randhawa, Gurjit Gill, Lavish Dhiman"
        WebElement playButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@aria-label='Play AZUL by Guru Randhawa, Gurjit Gill, Lavish Dhiman']//*[name()='svg']")));
        playButton.click();

        // Get and print the now playing text
        WebElement nowPlayingElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("(//div[@class='hb8C1VAjyUg0VMxrwpix'])[5]")));
        String nowPlayingText = nowPlayingElement.getText();
        System.out.println("now playing: " + nowPlayingText);

        // 4. Wait 30 seconds (let the song play)
        Thread.sleep(50000);

        // 5. Close the browser
        driver.quit();

        // ============================================================
    }
}