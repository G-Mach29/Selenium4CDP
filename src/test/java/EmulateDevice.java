
import java.util.HashMap;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.chrome.ChromeDriver;

import org.openqa.selenium.devtools.DevTools;

import org.openqa.selenium.devtools.v126.log.Log;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
@Slf4j
public class EmulateDevice {
    public ChromeDriver driver;
    /**
     * Initialize the WebDriverManager and EdgeDriver.
     * Go to the website under Test and maximize the browser window.
     */
    @BeforeEach
    public void setupUrl() {
        WebDriverManager.chromedriver().setup();
         driver = new ChromeDriver(); // Create driver instance
    }

    /**
     * Close the browser window.
     */
    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test

    public void emulateDeviceWithSend() throws InterruptedException {

        DevTools devTool = driver.getDevTools(); // Create devTool instance

        devTool.createSession();
        // Simulating Device Mode
        HashMap deviceMetrics = new HashMap() {{
            put("width", 500);
            put("height", 600);
            put("mobile", true);
            put("deviceScaleFactor", 50);
            put("screenOrientation", new HashMap() {{
                put("type", "portraitPrimary");
                put("angle", 20);
            }});
        }};
        driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", deviceMetrics);

        driver.get("https://ecommerce-playground.lambdatest.io");

    }
    @Test

    public void getConsoleLogs() {




        DevTools devTool = driver.getDevTools();

        devTool.createSession();

       /* Browser.GetVersionResponse browser = devTool.send(Browser.getVersion());

        System.out.println("Browser Version => "+browser.getProduct());

        System.out.println("User Agent => "+browser.getUserAgent());*/
        devTool.send(Log.enable());
        devTool.addListener(Log.entryAdded(),
                logEntry -> {
                    System.out.println("log: "+logEntry.getText());
                    System.out.println("level: "+logEntry.getLevel());
                });

        driver.get("https://ecommerce-playground.lambdatest.io");


    }

}
