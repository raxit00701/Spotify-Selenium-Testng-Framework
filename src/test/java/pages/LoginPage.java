package pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginPage {
    private WebDriver driver;
    private WebDriverWait wait;

    // 1. Log in button (top-right)
    @FindBy(css = "button[data-testid='login-button']")
    private WebElement signInButton;

    // 2. Username/email field
    @FindBy(id = "username")
    private WebElement usernameField;

    // 3. Continue after username
    @FindBy(css = "button[data-testid='login-button']")
    private WebElement continueAfterUsername;

    // Step 5: Continue to password entry
    @FindBy(css = "button[data-encore-id='buttonTertiary']")
    private WebElement passwordContinueButton;

    // 4. Password field
    @FindBy(id = "password")
    private WebElement passwordField;

    // 5. Final Login button
    @FindBy(css = "button[data-testid='login-button']")
    private WebElement loginButton;

    // Constructor
    public LoginPage(WebDriver driver, WebDriverWait wait) {
        this.driver = driver;
        this.wait = wait;
        PageFactory.initElements(driver, this);
    }

    // Actions
    public void clickSignIn() {
        wait.until(ExpectedConditions.elementToBeClickable(signInButton)).click();
    }

    public void enterUsername(String username) {
        usernameField.clear();
        usernameField.sendKeys(username);
    }

    public void clickContinueAfterUsername() {
        wait.until(ExpectedConditions.elementToBeClickable(continueAfterUsername)).click();
    }

    public void clickPasswordContinue() {
        passwordContinueButton.click();
    }

    public void enterPassword(String password) {
        passwordField.clear();
        passwordField.sendKeys(password);
    }

    public void clickFinalLogin() {
        wait.until(ExpectedConditions.elementToBeClickable(loginButton)).click();
    }
}