import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.NetworkInterceptor;
import org.openqa.selenium.devtools.v126.emulation.Emulation;
import org.openqa.selenium.devtools.v124.log.Log;
import org.openqa.selenium.devtools.v126.fetch.Fetch;
import org.openqa.selenium.devtools.v126.network.Network;
import org.openqa.selenium.devtools.v126.network.model.*;
import org.openqa.selenium.devtools.v126.performance.Performance;
import org.openqa.selenium.devtools.v126.performance.model.Metric;
import org.openqa.selenium.devtools.v126.security.Security;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.http.Route;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.bouncycastle.oer.its.ieee1609dot2.basetypes.Duration.milliseconds;
import static org.openqa.selenium.devtools.v126.network.Network.*;
import static org.openqa.selenium.remote.http.Contents.utf8String;

@Slf4j
public class TestDevToolsNetworkInterception {

    private static final Integer PAUSE_TIME = 5000;

    public ChromeDriver driver;
    DevTools devTools;

    /**
     * Initialize the WebDriverManager and EdgeDriver.
     * Go to the website under Test and maximize the browser window.
     */
    @BeforeEach
    public void setupUrl() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        devTools = driver.getDevTools();
        devTools.createSession();
    }

    /**
     * Close the browser window.
     */
    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    /**
     * Network Interception using Selenium 4.0.
     * DevTools has a method to intercept network requests: 'NetworkInterceptor'
     * The website under test should go after the 'Route.matching' method.
     */
    @Test
    void interceptNetworkRequests() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking, network events will now be delivered to the client
        NetworkInterceptor networkInterceptor = new NetworkInterceptor(driver,
                // Intercepts all network requests.
                Route.matching(request -> true)
                        .to(() -> request -> new HttpResponse().setStatus(200).addHeader("Content-Type", "text/html")
                                .addHeader("Accept-Encoding", "gzip, deflate")
                                .setContent(utf8String("Network Intercepted!"))));
        // Go to the website
        driver.get("https://linkedin.com");
        String pageSource = driver.getPageSource();
        assertSoftly(softly -> softly.assertThat(pageSource).contains("Network Intercepted!"));
    }

    /**
     * Network Security using Selenium 4.0.
     * DevTools has a method to intercept network requests and block requests: 'Fetch.requestPaused'
     * The website under test should go after the 'Network Listener' method.
     */
    @Test
    void networkFetchTracking() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking with the 'Fetch' method, network events will now be delivered to the client
        devTools.send(Fetch.enable(Optional.empty(), Optional.empty()));
        // Add a new network request listener
        devTools.addListener(Fetch.requestPaused(), request -> {
            // Get the request URL
            String url = request.getRequest().getUrl();
            // If the url is in the list of blocked urls, block the request
            if (url.contains("linkedin.com")) {
                devTools.send(Fetch.continueRequest(request.getRequestId(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()));
            } else {
                devTools.send(Fetch.continueRequest(request.getRequestId(), Optional.of(url), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()));
            }
            // Go to the website
            driver.get("https://linkedin.com");
            assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("LinkedIn"));
        });
    }

    /**
     * Network Block Patterns using Selenium 4.0.
     * DevTools has a method to intercept network requests and block patterns: 'setBlockedURLs'
     * The website under test should go after the 'Network Listener' method.
     */
    @Test
    void networkBlockPatterns() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables security tracking with the 'Security' method, security events will now be delivered to the client
        devTools.send(Security.setIgnoreCertificateErrors(true));
        // Block all requests patterns
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        devTools.send(Network.setBlockedURLs(ImmutableList.of("*")));
        // Add a new request listener
        devTools.addListener(loadingFailed(), loadingFailed -> {
            if (loadingFailed.getType().equals(ResourceType.STYLESHEET)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            } else if (loadingFailed.getType().equals(ResourceType.IMAGE)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            } else if (loadingFailed.getType().equals(ResourceType.SCRIPT)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            } else if (loadingFailed.getType().equals(ResourceType.XHR)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            } else if (loadingFailed.getType().equals(ResourceType.MEDIA)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            } else if (loadingFailed.getType().equals(ResourceType.WEBSOCKET)) {
                log.info("Blocking reason: " + loadingFailed.getBlockedReason());
                assertSoftly(softly -> softly.assertThat(loadingFailed.getBlockedReason())
                        .isEqualTo(Optional.of(BlockedReason.INSPECTOR)));
            }
        });
        // Block Patterns - In this example: Block some IMG requests.
        devTools.send(Network.setBlockedURLs(
                List.of("https://ecommerce-playground.lambdatest.io/image/catalog/maza/svg/image2vector.svg",
                        "https://ecommerce-playground.lambdatest.io/image/catalog/opencart-logo.png",
                        "https://ecommerce-playground.lambdatest.io/catalog/view/theme/mz_poco/asset/stylesheet/megastore-2.28/combine/eba62915f06ab23a214a819a0557a58b.css")));
        // Add a listener to the 'Network' method to get the blocked request
        devTools.addListener(loadingFailed(), loadingFailed -> {
            log.info("Blocking reason final: " + loadingFailed.getBlockedReason().get());
        });
        // Go to the website
        driver.get("https://ecommerce-playground.lambdatest.io");
        assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("Your Store"));
    }

    /**
     * WebSocket Listener using Selenium 4.0.
     * DevTools has a method to intercept WebSocket requests and create a listener: 'webSocketCreated'
     * The website under test should go after the 'webSocketClosed' method.
     */
    @Test
    public void verifyWebSocketOperationTest() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking with the 'Enable' method, network events will now be delivered to the client
        devTools.send(enable(Optional.empty(), Optional.empty(), Optional.empty()));
        // Add a new WebSocket listener
        devTools.addListener(webSocketCreated(), ws -> {
            log.info("WebSocket created: " + ws.getUrl());
            log.info("WebSocket id: " + ws.getRequestId());
            log.info("WebSocket type: " + ws.getInitiator().stream().findFirst().get().getType());
        });
        // Received WebSocket listener
        devTools.addListener(webSocketFrameReceived(), e -> {
            log.info("WebSocket frame received: " + e.getRequestId());
            log.info(e.getResponse().getPayloadData());
            log.info(e.getResponse().getOpcode().toString());
            log.info(String.valueOf(e.getResponse().getMask()));
        });
        // Get WebSocket error listener
        devTools.addListener(webSocketFrameError(), e -> {
            log.info("WebSocket error: " + e.getErrorMessage());
        });
        // Close WebSocket listener
        devTools.addListener(webSocketClosed(), c -> {
            log.info("WebSocket Closed");
            log.info(String.valueOf(c.getTimestamp()));
        });
        // Go to the website and open a WebSocket connection
        driver.get("https://www.piesocket.com/websocket-tester");
        var button = driver.findElement(By.xpath("//button[@type='submit']"));
        button.click();
        setPause();
        var closeButton = driver.findElement(By.xpath("//button[normalize-space()='Disconnect']"));
        closeButton.click();
        setPause();
        assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("Online WebSocket"));
    }

    /**
     * Event Message Listener using Selenium 4.0.
     * DevTools has a method to intercept Event source and create a listener: 'eventSourceMessageReceived'
     * The website under test should go after the 'addListener' method.
     */
    @Test
    void verifyEventSourceMessagesTest() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking with the 'Enable' method, network events will now be delivered to the client
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        // Add a new Event Source listener
        devTools.addListener(eventSourceMessageReceived(), e -> {
            log.info("Event Source event data received: " + e.getData());
            log.info("Event Source event name: " + e.getEventName());
            log.info("Event Source event id: " + e.getEventId());
            log.info("Event Source message id: " + e.getRequestId());
            log.info("Event Source event time: " + e.getTimestamp());
        });
        // Go to the website and open an Event Source connection
        driver.get("https://www.w3schools.com/html/tryit.asp?filename=tryhtml5_sse");
        setPause();
        assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("Editor"));
    }

    /**
     * Get HTTP traffic using Selenium 4.0.
     * DevTools has a method to intercept HTTP requests and create a listener: 'responseReceived'
     * The website under test should go after the 'addListener' method.
     */
    @Test
    void getHttpTrafficTest() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking with the 'Enable' method, network events will now be delivered to the client
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        // Add a new HTTP listener
        devTools.addListener(responseReceived(), e -> {
            log.info("HTTP response received: " + e.getRequestId());
            log.info("HTTP response url: " + e.getResponse().getUrl());
            log.info("HTTP response status: " + e.getResponse().getStatus());
            log.info("HTTP response status text: " + e.getResponse().getStatusText());
            log.info("HTTP response headers: " + e.getResponse().getHeaders());
            log.info("HTTP response protocol: " + e.getResponse().getProtocol());
            log.info("HTTP response remote IP address: " + e.getResponse().getRemoteIPAddress());
            log.info("HTTP response remote port: " + e.getResponse().getRemotePort());
            log.info("HTTP response mime type: " + e.getResponse().getMimeType());
            log.info("HTTP response connection id: " + e.getResponse().getConnectionId());
            log.info("---");
        });
        // Go to the website
        driver.get("https://ecommerce-playground.lambdatest.io");
        assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("Your Store"));
    }

    /**
     * Get Request served from Cache using Selenium 4.0.
     * DevTools has a method to intercept network requests: 'requestServedFromCache'
     * The website under test should go after the 'addListener' method.
     */
    @Test
    void getRequestServedFromCacheTest() {
        // Get The DevTools & Create A Session with the ChromeDriver.
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        // Enables network tracking with the 'Enable' method, network events will now be delivered to the client
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        // Clear Browser Cache
        devTools.send(Network.setCacheDisabled(true));
        devTools.send(Network.clearBrowserCache());
        devTools.send(Network.clearBrowserCookies());
        // Add a new HTTP listener
        devTools.addListener(requestServedFromCache(), cacheRequest -> {
            log.info("HTTP request served from cache: " + cacheRequest);
        });
        // Go to the website
        driver.get("https://ecommerce-playground.lambdatest.io");
        assertSoftly(softly -> softly.assertThat(driver.getTitle()).contains("Your Store"));
    }

    /**
     * Sets a pause on the page load.
     */
    private void setPause() {
        Actions actions = new Actions(driver);
        actions.pause(PAUSE_TIME).perform();
    }


    @Test
    public void accessURLNormal() {
        long startTime = System.currentTimeMillis();
        driver.get("https://www.qed42.com");
        long endTime = System.currentTimeMillis();

        System.out.println("Normal Way: Page loaded in " + (endTime - startTime) + " milliseconds");
    }

    @Test
    public void captureNetworkCalls() {

        /* Monitoring HTTP Requests*/
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        /*Network.requestWillBeSent to the listener. This event is fired when the page is about to send an HTTP request*/
        devTools.addListener(Network.requestWillBeSent(),
                entry -> {
                    RequestId requestid = entry.getRequestId();
                    System.out.println("Request Method : " + entry.getRequest().getMethod());
                    System.out.println("Request URI : " + entry.getRequest().getUrl());
                    System.out.println("Request headers:");
                    entry.getRequest().getHeaders().toJson().forEach((k, v) -> System.out.println((k + ":" + v)));
                    Optional<String> postData = entry.getRequest().getPostData();
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    postData.ifPresentOrElse(p -> System.out.println("Request Body: \n" + gson.toJson(JsonParser.parseString(p)) + "\n"),
                            () -> System.out.println("Not request body found \n"));

                });
        driver.get("https://www.booking.com");
        driver.findElement(By.xpath("(//span[contains(text(), \"Search\")])[1]")).click();
    }

    @Test
    public void validateResponse() {
        final RequestId[] requestIds = new RequestId[1];
        devTools.send(Network.enable(Optional.of(100000000), Optional.empty(), Optional.empty()));
        devTools.addListener(Network.responseReceived(), responseReceived -> {
            if (responseReceived.getResponse().getUrl().contains("api.zoomcar.com")) {
                System.out.println("URL: " + responseReceived.getResponse().getUrl());
                System.out.println("Status: " + responseReceived.getResponse().getStatus());
                System.out.println("Type: " + responseReceived.getType().toJson());
                responseReceived.getResponse().getHeaders().toJson().forEach((k, v) -> System.out.println((k + ":" + v)));
                requestIds[0] = responseReceived.getRequestId();
                System.out.println("Response Body: \n" + devTools.send(Network.getResponseBody(requestIds[0])).getBody() + "\n");
            }
        });
        driver.get("https://www.zoomcar.com/bangalore");
       // driver.findElement(By.className("search")).click();
    }
    @Test
    public void blockUrl() {
        String urlToBlock = "https://medium.com/_/graphql";
        devTools.send(Network.enable(Optional.of(100000000), Optional.empty(), Optional.empty()));
        devTools.send(Network.setBlockedURLs(ImmutableList.of(urlToBlock)));

        devTools.addListener(Network.loadingFailed(), loadingFailed -> {
            System.out.println("Blocking reason: " + loadingFailed.getBlockedReason().get());
           // Assert.assertEquals(loadingFailed.getBlockedReason().get(), BlockedReason.INSPECTOR);
        });

        driver.get("https://medium.com");
    }

    @Test
    public void getAllCookies() {
        devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
        driver.get("https://google.com");

        //Getting all cookies
        List<Cookie> cookies = devTools.send(Network.getAllCookies());
        cookies.forEach(cookie -> System.out.println(cookie.getName()));

        List<String> cookieName = cookies.stream().map(cookie -> cookie.getName()).sorted().collect(Collectors.toList());
        Set<org.openqa.selenium.Cookie> seleniumCookie = driver.manage().getCookies();
        List<String> selCookieName = seleniumCookie.stream().map(selCookie -> selCookie.getName()).sorted().collect(Collectors.toList());
      //  Assert.assertEquals(cookieName, selCookieName);

        //Clearing browser cookies
        devTools.send(Network.clearBrowserCookies());
        List<Cookie> cookiesAfterClearing = devTools.send(Network.getAllCookies());
      //  Assert.assertTrue(cookiesAfterClearing.isEmpty());

    }

    @Test
    public void loadInsecureWebsite() {
        devTools.send(Security.enable());
        devTools.send(Security.setIgnoreCertificateErrors(true));
        driver.get("https://untrusted-root.badssl.com/");
    }
    @Test
    public void consoleLogs(){
        devTools.send(Log.enable());
        devTools.addListener(Log.entryAdded(),
                logEntry -> {
                    System.out.println("log: "+logEntry.getText());
                    System.out.println("level: "+logEntry.getLevel());
                });
        driver.get("https://www.zoomcar.com/bangalore");
       // driver.findElement(By.className("search")).click();
        devTools.send(Log.clear());
        devTools.send(Log.disable());
    }
    @Test
    public void simulateDeviceDimensions(){
        Map deviceMetrics = new HashMap()
        {{
            put("width", 600);
            put("height", 1000);
            put("mobile", true);
            put("deviceScaleFactor", 50);
        }};
        ((ChromeDriver)driver).executeCdpCommand("Emulation.setDeviceMetricsOverride", deviceMetrics);
        driver.get("https://www.zoomcar.com"); //set device first and then launch
    }
    @Test
    public void mockLocation(){
        devTools.send(Emulation.setGeolocationOverride(
                Optional.of(48.8584),
                Optional.of(2.2945),
                Optional.of(100)));
        driver.get("https://www.booking.com");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void getPerformanceMetrics(){
        devTools.send(Performance.enable(Optional.empty()));
        driver.get("https://www.booking.com");
        List<Metric> metrics = devTools.send(Performance.getMetrics());
        metrics.forEach(metric-> System.out.println(metric.getName() +" : "+ metric.getValue() ));
    }
}