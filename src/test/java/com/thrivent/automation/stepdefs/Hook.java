package com.thrivent.automation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigObject;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.testng.annotations.AfterClass;


public class Hook {
    private static AppiumDriver<MobileElement> driver;
    private AppiumServiceBuilder builder;
    private AppiumDriverLocalService service;
    private boolean startWithoutHub = false;

    // We're only supporting one set of configs per device at the moment.s
    // The sections below map to the sections in a single config file.
    private HashMap<String,Object> phoneCapability;
    private HashMap<String,Object> phoneConfiguration;

    // configuration settings
    private String useHub = "true";
    private String app = "someapp.notdefined";

    private String platformName = "";
    private String deviceName = "";
    private String udid = "";
    private String platformVersion = "";
    private String appiumServerIP = "127.0.0.1";
    private int appiumServerPort = 4723;

    @Before
    public void setUp() throws MalformedURLException {
        Config config = ConfigFactory.load();
        phoneCapability = getPhoneCapability(config);
        phoneConfiguration = getPhoneConfiguration(config);


        setUserProperties();
        setConfigSettings();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("platformName", platformName);
        capabilities.setCapability("browserName", deviceName);
        capabilities.setCapability("deviceName", deviceName);
        capabilities.setCapability("app", app);

        // These can't be empty, but can be unset
        if (!udid.isEmpty()) capabilities.setCapability("udid", udid);
        if (!platformVersion.isEmpty()) capabilities.setCapability("platformVersion", platformVersion);

        // TODO: Consider reading from config file.
        if (platformName.equals("iOS")) {
            capabilities.setCapability("xcodeOrgId", "UTURYJS693");
            capabilities.setCapability("xcodeSigningId", "iPhone Developer");
            capabilities.setCapability("updatedWDABundleId", "com.xevo.WebDriverAgentRunner");
            capabilities.setCapability("usePrebuiltWDA", false);
            capabilities.setCapability("automationName", "XCUITest");
        } else {
            capabilities.setCapability("automationName", "UiAutomator2");
        }

        if (startWithoutHub) {
            System.out.println("Starting without hub ... ");
            builder = new AppiumServiceBuilder();
            builder.usingPort(appiumServerPort);
            builder.withIPAddress(appiumServerIP);
            service= AppiumDriverLocalService.buildService(builder);
            service.start();
        }

        // TODO: Document these settings. Why we have them.
        // Why do we need a full reset if we're debugging, we need to not make this the
        // default
        capabilities.setCapability("fullReset", false);
        capabilities.setCapability("noReset", true);

        String hubUri = String.format("http://%s:%s/wd/hub", appiumServerIP, appiumServerPort);
        URL serviceUrl = (startWithoutHub) ? service.getUrl() : new URL(hubUri);

        driver = new AppiumDriver<MobileElement>(serviceUrl, capabilities);

        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
    }

    @After
    public void tearDown(){
        System.out.print("Restarting app");
        driver.resetApp();
        driver.quit();
        if(startWithoutHub) {
            service.stop();
        }
    }

    public static AppiumDriver<MobileElement> getDriver(){
        return driver;
    }

    private HashMap<String,Object> getPhoneCapability(Config config) {
        ConfigList capabilities = config.getList("capabilities");
        HashMap<String,Object> capability = (HashMap<String,Object>) capabilities.get(0).unwrapped();
        return capability;
    }

    private HashMap<String,Object> getPhoneConfiguration(Config config) {
        ConfigObject jsonPhoneConfig = config.getObject("configuration");
        HashMap<String,Object> configuration = (HashMap<String,Object>) jsonPhoneConfig.unwrapped();
        return configuration;
    }

    private void setUserProperties() {
        // Get the test properties
        useHub = System.getProperty("useHub", "true");
        app = System.getProperty("app", "someapp.notdefined");
        if (useHub!=null) {
            startWithoutHub = ! Boolean.parseBoolean(useHub);
        } else {
            startWithoutHub = true;
        }
    }

    // Get config settings
    private void setConfigSettings() {
        // UUID must be specified otherwise simulator will try to be created for IOS
        platformName = (String) phoneCapability.get("platformName");
        deviceName = (String) phoneCapability.get("deviceName");
        udid = (String) phoneCapability.get("udid");
        platformVersion = (String) phoneCapability.get("platformVersion");

        // Todo do we still need the real ip address?
        if (!startWithoutHub) {
            appiumServerIP = (String) phoneConfiguration.get("host");
            appiumServerPort = (int) phoneConfiguration.get("port");
        }
    }
}