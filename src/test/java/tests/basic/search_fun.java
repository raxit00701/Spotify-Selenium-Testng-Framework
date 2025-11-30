package tests.basic;

import base.BaseTest;
import pages.LoginPage;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.interactions.Actions;
import java.time.Duration;
import listeners.TestListener;  // ← Add this import
import org.testng.annotations.Listeners;  // ← Add this import
import org.testng.annotations.Test;
@Listeners({
        io.qameta.allure.testng.AllureTestNg.class, // Allure first
        listeners.TestListener.class                // then your listener
})
public class search_fun extends BaseTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private LoginPage loginPage;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        initDriver(); // This method comes from BaseTest
        driver = getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        loginPage = new LoginPage(driver, wait);


        driver.get(getEnvConfig().getBaseUrl()); // e.g. https://open.spotify.com
        System.out.println("[Checking search song functionality] Opened Spotify: " + getEnvConfig().getBaseUrl());
    }

    @Test(groups = {"regression"}, priority = 1)
    public void playMusicTest() throws InterruptedException {

        loginPage.clickSignIn();
        loginPage.enterUsername("banefo8720@bialode.com");
        loginPage.clickContinueAfterUsername();
        loginPage.clickPasswordContinue();
        loginPage.enterPassword("Password@12");
        loginPage.clickFinalLogin();

        Thread.sleep(10000);



        // 6. Search input field
        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@placeholder='What do you want to play?']")));
        searchInput.click();

// 7. Enter search text and pause for 3 seconds
        String songQuery = "mickey singh rani";
        searchInput.sendKeys(songQuery);
        Thread.sleep(3000);

// 8. Dynamically print whatever is inside the search input
        String enteredText = searchInput.getAttribute("value");
        System.out.println("[Inside search box] Entered search text : " + enteredText);



        // 8a. Scroll down to the track element before clicking Play
        WebElement trackElement = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class='e-91000-text encore-text-title-medium encore-internal-color-text-base BVqIO7mYwD5fLLh6i1D3']")));
        //((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", trackElement);
        //Thread.sleep(1000); // small pause to ensure scroll completes

// 8b. Hover on the track element
        Actions actions = new Actions(driver);
        actions.moveToElement(trackElement).perform();
        Thread.sleep(100); // pause to visually confirm hover



        // 9. Click Play button for the specific track
        WebElement playButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[aria-label='Play'] span[class='e-91000-baseline e-91000-overflow-wrap-anywhere e-91000-button-primary__inner encore-bright-accent-set e-91000-button-icon-only--medium'] span[class='e-91000-button__icon-wrapper'] svg")));
        playButton.click();

        // 10. Print the now playing track text
        WebElement nowPlayingText = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class='e-91000-text encore-text-title-medium encore-internal-color-text-base BVqIO7mYwD5fLLh6i1D3']")));
        System.out.println(" Now playing on track: " + nowPlayingText.getText());

        // 11. Wait for 30 seconds while the song plays
        Thread.sleep(50000);

        // 12. Quit driver
        driver.quit();

    }
}