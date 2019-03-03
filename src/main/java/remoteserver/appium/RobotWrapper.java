package remoteserver.appium;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

import com.bbmauto.manager.BbmLogger;
import com.bbmauto.manager.DriverEmtek;
import com.bbmauto.manager.DriverEmtekAndroid;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.Connection;

@RobotKeywords
@SuppressWarnings("rawtypes")
public class RobotWrapper {
  private static final BbmLogger logger = new BbmLogger(RobotWrapper.class.getSimpleName());
  public static HashMap<String, DriverEmtek> drivers = new HashMap<String, DriverEmtek>();
  public static ArrayList<String> driverIndex = new ArrayList<String>();
  public static DriverEmtek activeDriver = null;
  public static Integer defaultTimeout = 10;
  public static Integer currentTimeout = 10;

  /**
   * Opens a new application to given Appium server. Capabilities of appium server, Android and iOS,
   * Please check http://appium.io/slate/en/master/?python#appium-server-capabilities
   *
   * @param url URL to Appium server
   * @param args Capabilities must use alias to device alias=Myapp1 platformName=Android
   *     platformVersion=4.2.2 deviceName=192.168.56.101:5555
   *     app=${CURDIR}/demoapp/OrangeDemoApp.apk appPackage=com.netease.qa.orangedemo
   *     appActivity=MainActivity
   * @throws MalformedURLException URL
   */
  @RobotKeyword(
      "Opens a new application to given Appium server. "
          + "Capabilities of appium server, Android and iOS, Please check appium.io")
  @ArgumentNames({"url", "*args"})
  public static void openApplication(String url, String... args) throws MalformedURLException {
    logger.log(Level.INFO, "\nurl: " + url + "\nargs: " + Arrays.toString(args));

    DriverEmtek driver = null;
    DesiredCapabilities capabilities = new DesiredCapabilities();
    String udid = null;
    String alias = null;

    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      capabilities.setCapability(arguement[0], arguement[1]);
      if (arguement[0].equalsIgnoreCase("udid")) {
        udid = arguement[1];
      } else if (arguement[0].equalsIgnoreCase("alias")) {
        alias = arguement[1];
      }
    }

    // Start Activity if udid already exists
    boolean existing = false;
    for (Entry<String, DriverEmtek> entry : drivers.entrySet()) {
      if (((String) entry.getValue().getCapabilities().getCapability("udid"))
          .equalsIgnoreCase(udid)) {
        if (entry instanceof DriverEmtekAndroid) {
          alias = entry.getKey();
          existing = true;
        }
      }
    }

    if (existing) {
      logger.log(Level.INFO, "Driver already exists. Reusing by calling start activity");
      activeDriver = drivers.get(alias);
      ((DriverEmtekAndroid) activeDriver)
          .startActivity(
              (String) capabilities.getCapability("app"),
              (String) capabilities.getCapability("appActivity"));
    } else {
      // TODO base on platform
      driver = new DriverEmtekAndroid<>(new URL(url), capabilities);
      setAppiumTimeout(defaultTimeout);

      drivers.put(alias, driver);
      driverIndex.add(alias);
      activeDriver = driver;
    }
  }

  /**
   * Switches the active application by index or alias.
   *
   * @param indexOrAlias Alias based from 'Open Application' call
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Switches the active application by index or alias.")
  @ArgumentNames({"index_or_alias"})
  public static void switchApplication(String indexOrAlias) throws Exception {
    logger.log(Level.INFO, "\nindex_or_alias: " + indexOrAlias);

    try {
      Integer index = Integer.valueOf(indexOrAlias);
      activeDriver = drivers.get(driverIndex.get(index));
    } catch (Exception exception) {
      try {
        activeDriver = drivers.get(indexOrAlias);
      } catch (Exception exception2) {
        logger.exception("Unable to switch to: " + indexOrAlias);
      }
    }
  }

  /**
   * Puts the application in the background on the device for a certain duration.
   *
   * @param args Optional parameter 'seconds'
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Puts the application in the background on the device for a certain duration.")
  @ArgumentNames({"*args"})
  public static void backgroundApp(String... args) throws Exception {
    logger.log(Level.INFO, "\nargs: " + Arrays.toString(args));
    Integer seconds = 5;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("seconds")) {
        seconds = Integer.valueOf(arguement[1]);
      }
    }
    activeDriver.runAppInBackground(seconds);
  }

  /**
   * Takes a screenshot of the current page and saves to APPIUM_LOGS environment variable directory.
   *
   * @param args Optional parameter 'name' to add to filename
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Takes a screenshot of the current page and embeds it into the log.")
  @ArgumentNames({"*args"})
  public static void capturePageScreenshot(String... args) throws Exception {
    logger.log(Level.INFO, "\nargs: " + Arrays.toString(args));
    String name = "";
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("name")) {
        name = arguement[1];
      }
    }
    activeDriver.getScreenshot(name);
  }

  /**
   * Clears the text field identified by locator.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Clears the text field identified by locator.")
  @ArgumentNames({"locator"})
  public static void clearText(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    activeDriver.findElement(translateLocatorToBy(locator)).clear();
  }

  /**
   * Click on a point.
   *
   * @param args Optional parameters 'x' and 'y' and 'duration'
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Click on a point")
  @ArgumentNames({"*args"})
  public static void clickAPoint(String... args) throws Exception {
    logger.log(Level.INFO, "\nargs: " + Arrays.toString(args));
    Integer xCoord = 0;
    Integer yCoord = 0;
    Integer duration = 100;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("x")) {
        xCoord = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("y")) {
        yCoord = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("duration")) {
        duration = Integer.valueOf(arguement[1]);
      }
    }

    activeDriver.tap(1, xCoord, yCoord, duration);
  }

  /**
   * Click button.
   *
   * @param indexOrName Index or name of button
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Click button")
  @ArgumentNames({"index_or_name"})
  public static void clickButton(String indexOrName) throws Exception {
    logger.log(Level.INFO, "\nindex_or_name: " + indexOrName);
    By by = null;
    try {
      // Index
      Integer index = Integer.valueOf(indexOrName);
      by = By.xpath("//android.widget.Button[" + index + "]");
    } catch (Exception exception) {
      String name = indexOrName;
      by = By.xpath("//android.widget.Button[@resource-id='" + name + "']");
    }

    activeDriver.findElement(by).click();
  }

  /**
   * Click element identified by locator. Key attributes for arbitrary elements are index and name.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Click element identified by locator.")
  @ArgumentNames({"locator"})
  public static void clickElement(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    activeDriver.findElement(translateLocatorToBy(locator)).click();
  }

  /**
   * Click element at a certain coordinate.
   *
   * @param coordinateX x
   * @param coordinateY y
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Click element at a certain coordinate")
  @ArgumentNames({"coordinate_X, coordinate_Y"})
  public static void clickElementAtCoordinates(Integer coordinateX, Integer coordinateY)
      throws Exception {
    logger.log(Level.INFO, "\ncoordinate_X: " + coordinateX + "\ncoordinate_Y: " + coordinateY);

    activeDriver.tap(1, coordinateX, coordinateY, 1000);
  }

  /**
   * Click text identified by text. By default tries to click first text involves given text, if you
   * would like to click exactly matching text, then set exact_match to True.
   *
   * @param text The string text
   * @param args Optional parameter 'exact_match' as true or false
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Click text identified by text.")
  @ArgumentNames({"text", "*args"})
  public static void clickText(String text, String... args) throws Exception {
    logger.log(Level.INFO, "\ntext: " + text + "\nargs: " + Arrays.toString(args));

    Boolean exactMatch = false;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("exact_match")) {
        exactMatch = Boolean.valueOf(arguement[1]);
      }
    }

    if (exactMatch) {
      activeDriver.findElement(By.xpath("//*[@text='" + text + "']")).click();
    } else {
      activeDriver.findElement(By.xpath("//*[contains(@text,'" + text + "')]")).click();
    }
  }

  /**
   * Closes all open applications. This keyword is meant to be used in test or suite teardown to
   * make sure all the applications are closed before the test execution finishes.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Closes all open applications.")
  public static void closeAllApplications() throws Exception {
    for (Entry<String, DriverEmtek> entry : drivers.entrySet()) {
      if (entry.getValue().equals(activeDriver)) {
        logger.log(Level.INFO, "Closing everything for the following driver: " + activeDriver);
        drivers.remove(entry.getKey());
        driverIndex.remove(entry.getKey());
        activeDriver.quit();
        activeDriver = null;
      }
    }
  }

  /**
   * Close the app which was provided in the capabilities at session creation.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Closes the current application.")
  public static void closeApplication() throws Exception {
    logger.log(Level.INFO, "Closing the app for the following driver: " + activeDriver);
    activeDriver.closeApp();
  }

  /**
   * Verify the attribute 'name' value
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param expected The expected name
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verify the attribute 'name' value")
  @ArgumentNames({"locator", "expected"})
  public static void elementNameShouldBe(String locator, String expected) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nexpected: " + expected);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    String nameValue = element.getAttribute("name");
    if (!nameValue.contentEquals(expected)) {
      logger.exception("Element name should be '" + expected + "' but it is: " + nameValue);
    }
  }

  /**
   * Verifies that element identified with locator is disabled. Key attributes for arbitrary
   * elements are id and name.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args log level parameter ignored
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that element identified with locator is disabled.")
  @ArgumentNames({"locator", "*args"})
  public static void elementShouldBeDisabled(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    if (element.isEnabled()) {
      logger.exception("Element should be disabled but it is not.");
    }
  }

  /**
   * Verifies that element identified with locator is enabled. Key attributes for arbitrary elements
   * are id and name.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args log level parameter ignored
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that element identified with locator is enabled.")
  @ArgumentNames({"locator", "*args"})
  public static void elementShouldBeEnabled(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    if (!element.isEnabled()) {
      logger.exception("Element should be enabled but it is not.");
    }
  }

  /**
   * Verifies element identified by locator contains text expected. If you wish to assert an exact
   * (not a substring) match on the text of the element, use Element Text Should Be. Key attributes
   * for arbitrary elements are id and xpath.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param expected The expected text
   * @param args Optional parameter 'message' can be used to override the default error message.
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies element identified by locator contains " + "text expected.")
  @ArgumentNames({"locator", "expected", "*args"})
  public static void elementShouldContainText(String locator, String expected, String... args)
      throws Exception {
    logger.log(
        Level.INFO,
        "\nlocator: " + locator + "\nexpected: " + expected + "\nargs: " + Arrays.toString(args));

    String message = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("message")) {
        message = arguement[1];
      }
    }

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    if (!element.getText().contains(expected)) {
      if (message == null) {
        logger.exception(
            "Element should have contained text. '"
                + expected
                + "' but found: '"
                + element.getText()
                + "'");
      } else {
        logger.exception(message);
      }
    }
  }

  /**
   * Verifies element identified by locator does not contain text expected.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param expected The expected text not to have
   * @param args Optional parameter 'message' can be used to override the default error message.
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies element identified by locator " + "does not contain text expected.")
  @ArgumentNames({"locator", "expected", "*args"})
  public static void elementShouldNotContainText(String locator, String expected, String... args)
      throws Exception {
    logger.log(
        Level.INFO,
        "\nlocator: " + locator + "\nexpected: " + expected + "\nargs: " + Arrays.toString(args));

    String message = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("message")) {
        message = arguement[1];
      }
    }

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    if (element.getText().contains(expected)) {
      if (message == null) {
        logger.exception(
            "Element should not have contained text. '"
                + expected
                + "' but found: '"
                + element.getText()
                + "'");
      } else {
        logger.exception(message);
      }
    }
  }

  /**
   * Verifies element identified by locator contains text expected. If you wish to assert an exact
   * (not a substring) match on the text of the element, use Element Text Should Be.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param expected The expected text
   * @param args Optional parameter 'message' can be used to override the default error message.
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies element identified by locator exactly contains text expected.")
  @ArgumentNames({"locator", "expected", "*args"})
  public static void elementTextShouldBe(String locator, String expected, String... args)
      throws Exception {
    logger.log(
        Level.INFO,
        "\nlocator: " + locator + "\nexpected: " + expected + "\nargs: " + Arrays.toString(args));

    String message = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("message")) {
        message = arguement[1];
      }
    }

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    if (element.getText().contentEquals(expected)) {
      if (message == null) {
        logger.exception(
            "The text of element should have been: '"
                + expected
                + "' but found: '"
                + element.getText()
                + "'");
      } else {
        logger.exception(message);
      }
    }
  }

  /**
   * Verifies that element 'value' attribute is equal to expected
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param expected The expected value
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that element 'value' attribute is equal to expected")
  @ArgumentNames({"locator", "expected"})
  public static void elementValueShouldBe(String locator, String expected) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nexpected: " + expected);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    String nameValue = element.getAttribute("value");
    if (!nameValue.contentEquals(expected)) {
      logger.exception("Element value should be '" + expected + "' but it is: " + nameValue);
    }
  }

  /**
   * Gets the timeout in seconds that is used by various keywords.
   *
   * @return The current timeout
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Gets the timeout in seconds that is used by various keywords.")
  public static Integer getAppiumTimeout() throws Exception {
    logger.log(Level.INFO, "The Appium Timout is: " + currentTimeout);
    return currentTimeout;
  }

  /**
   * Get available contexts.
   *
   * @return String array of contexts
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Get available contexts.")
  public static String[] getContexts() throws Exception {
    String[] values = (String[]) activeDriver.getContextHandles().toArray();
    logger.log(Level.INFO, "Returning: " + Arrays.toString(values));
    return values;
  }

  /**
   * Get current context.
   *
   * @return THe current conxtex
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Get current context.")
  public static String getCurrentContext() throws Exception {
    String value = activeDriver.getContext();
    logger.log(Level.INFO, "Returning: " + value);
    return value;
  }

  /**
   * Get element attribute using given attribute: name, value,...
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param attribute The attribute
   * @return The attribute value
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Get element attribute using given attribute: name, value,...")
  @ArgumentNames({"locator", "attribute"})
  public static String getElementAttribute(String locator, String attribute) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nattribute: " + attribute);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    String value = element.getAttribute(attribute);
    logger.log(Level.INFO, "Returning: " + value);
    return value;
  }

  /**
   * Get element location Key attributes for arbitrary elements are id and name.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return the x,y location as string list
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Get Element Location")
  @ArgumentNames({"locator"})
  public static String getElementLocation(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    Point value = element.getLocation();
    String returnValue = "{'y': " + value.getY() + ", 'x': " + value.getX() + "}";
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Get element size Key attributes for arbitrary elements are id and name.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return THe size dimension of first element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Get element size")
  @ArgumentNames({"locator"})
  public static String getElementSize(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    MobileElement element = activeDriver.findElement(translateLocatorToBy(locator));
    Dimension value = element.getSize();
    String returnValue = "{'width': " + value.getWidth() + ", 'height': " + value.getHeight() + "}";
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Returns number of elements matching xpath One should not use the xpath= prefix for 'xpath'.
   * XPath is assumed.
   *
   * @param xpath The xpath query
   * @return THe total count
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Returns number of elements matching xpath")
  @ArgumentNames({"xpath"})
  public static Integer getMatchingXpathCount(String xpath) throws Exception {
    logger.log(Level.INFO, "\nxpath: " + xpath);

    Integer returnValue = activeDriver.findElements(translateLocatorToBy(xpath)).size();
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Returns an integer bitmask specifying the network connection type. Android only.
   *
   * @return Returns an integer bitmask specifying the network connection type
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Returns an integer bitmask specifying the network connection type.")
  public static String getNetworkConnectionStatus() throws Exception {
    Connection connection = ((DriverEmtekAndroid) activeDriver).getConnection();
    logger.log(Level.INFO, "Returning: " + connection);
    return connection.toString();
  }

  /**
   * Returns the entire source of the current page.
   *
   * @return Returns the entire source of the current page.
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Returns the entire source of the current page.")
  public static String getSource() throws Exception {
    String returnValue = activeDriver.getPageSource();
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Get element text (for hybrid and mobile browser use xpath locator, others might cause problem)
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return The text
   * @throws Exception Logged to logger
   */
  @RobotKeyword(
      "Get element text (for hybrid and mobile browser use xpath locator, others might "
          + "cause problem)")
  @ArgumentNames({"locator"})
  public static String getText(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    String returnValue = activeDriver.findElement(translateLocatorToBy(locator)).getText();
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Returns the first WebElement object matching locator.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return First web element matching locator
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Returns the first WebElement object matching locator.")
  @ArgumentNames({"locator"})
  public static String getWebelement(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    MobileElement returnValue = activeDriver.findElement(translateLocatorToBy(locator));
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue.toString();
  }

  /**
   * Returns list of WebElement objects matching locator.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return List of web elements
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Returns the first WebElement object matching locator.")
  @ArgumentNames({"locator"})
  public static List<MobileElement> getWebelements(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    List<MobileElement> returnValue = activeDriver.findElements(translateLocatorToBy(locator));
    logger.log(Level.INFO, "Returning: " + returnValue);
    return returnValue;
  }

  /**
   * Goes one step backward.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Goes one step backward in the browser history.")
  public static void goBack() throws Exception {
    logger.log(Level.INFO, "");
    activeDriver.navigate().back();
  }

  /**
   * Hides the software keyboard on the device. (optional) In iOS, use key_name to press a
   * particular key, ex. Done. In Android, no parameters are used.
   *
   * @param args Parameters ignored
   * @throws Exception Logged to logger
   */
  @RobotKeyword(
      "Hides the software keyboard on the device. (optional) "
          + "In iOS, use key_name to press a particular key, ex. Done. "
          + "In Android, no parameters are used.")
  @ArgumentNames({"*args"})
  public static void hideKeyboard(String... args) throws Exception {
    logger.log(Level.INFO, "hiding keyboard");
    if (activeDriver.isVirtualKeyboardPresent()) {
      activeDriver.hideKeyboard();
    }
  }

  /**
   * Types the given password into text field identified by locator. Difference between this keyword
   * and Input Text is that this keyword does not log the given password.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param text The string text
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Types the given password into text field identified by locator.")
  @ArgumentNames({"locator", "text"})
  public static void inputPassword(String locator, String text) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    activeDriver.findElement(translateLocatorToBy(locator)).sendKeys(text);
  }

  /**
   * Types the given text into text field identified by locator.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param text The string text
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Types the given text into text field identified by locator.")
  @ArgumentNames({"locator", "text"})
  public static void inputText(String locator, String text) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\ntext: " + text);
    activeDriver.findElement(translateLocatorToBy(locator)).sendKeys(text);
  }

  /**
   * Set the device orientation to LANDSCAPE.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Set the device orientation to LANDSCAPE")
  public static void landscape() throws Exception {
    logger.log(Level.INFO, "Set the device orientation to LANDSCAPE");
    activeDriver.rotate(ScreenOrientation.LANDSCAPE);
  }

  /**
   * Set the device orientation to PORTRAIT.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Set the device orientation to PORTRAIT")
  public static void portrait() throws Exception {
    logger.log(Level.INFO, "Set the device orientation to PORTRAI");
    activeDriver.rotate(ScreenOrientation.PORTRAIT);
  }

  /**
   * Logs and returns the entire html source of the current page or frame.
   *
   * @param args loglevel parameter ignored
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Logs and returns the entire html source of the current page or frame.")
  @ArgumentNames({"*args"})
  public static void logSource(String... args) throws Exception {
    logger.log(Level.INFO, "Logging page source: " + activeDriver.getPageSource());
  }

  /**
   * Long press the element
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Long press the element")
  @ArgumentNames({"locator"})
  public static void longPress(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    activeDriver.tap(1, activeDriver.findElement(translateLocatorToBy(locator)), 1000);
  }

  /**
   * Sends a long press of keycode to the device. Android only. See press keycode for more details.
   *
   * @param keycode The integer key
   * @param args Optional parameter metastate
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Sends a long press of keycode to the device.")
  @ArgumentNames({"keycode", "*args"})
  public static void longPressKeycode(Integer keycode, String... args) throws Exception {
    logger.log(Level.INFO, "\nkeycode: " + keycode + "\nargs: " + Arrays.toString(args));

    Integer metastate = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("metastate")) {
        metastate = Integer.valueOf(arguement[1]);
      }
    }

    if (metastate == null) {
      ((DriverEmtekAndroid) activeDriver).longPressKeyCode(keycode);
    } else {
      ((DriverEmtekAndroid) activeDriver).longPressKeyCode(keycode, metastate);
    }
  }

  /**
   * Verifies that current page contains locator element.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args Parameter ignored loglevel
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that current page contains locator element.")
  @ArgumentNames({"locator", "*args"})
  public static void pageShouldContainElement(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nargs: " + Arrays.toString(args));

    if (!activeDriver.isElementPresent(translateLocatorToBy(locator))) {
      logger.exception("Page should have contained element: " + locator);
    }
  }

  /**
   * Verifies that current page contains text.
   *
   * @param text The string text
   * @param args Parameter ignored loglevel
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that current page contains text.")
  @ArgumentNames({"text", "*args"})
  public static void pageShouldContainText(String text, String... args) throws Exception {
    logger.log(Level.INFO, "\ntext: " + text + "\nargs: " + Arrays.toString(args));

    if (!activeDriver.isElementPresent(By.xpath("*[contains(@text," + text + ")]"))) {
      logger.exception("Page should have contained text: " + text);
    }
  }

  /**
   * Verifies that current page not contains locator element.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args Parameter ignored loglevel
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that current page not contains locator element.")
  @ArgumentNames({"locator", "*args"})
  public static void pageShouldNotContainElement(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nargs: " + Arrays.toString(args));

    if (activeDriver.isElementPresent(translateLocatorToBy(locator))) {
      logger.exception("Page should not have contained element: " + locator);
    }
  }

  /**
   * Verifies that current page not contains text.
   *
   * @param text The string text
   * @param args Parameter ignored loglevel
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Verifies that current page not contains text.")
  @ArgumentNames({"text", "*args"})
  public static void pageShouldNotContainText(String text, String... args) throws Exception {
    logger.log(Level.INFO, "\ntext: " + text + "\nargs: " + Arrays.toString(args));

    if (activeDriver.isElementPresent(By.xpath("*[contains(@text," + text + ")]"))) {
      logger.exception("Page should not have contained text: " + text);
    }
  }

  /**
   * Pinch in on an element a certain amount.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args Optional parameters ignored. percent=200%, steps=1
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Pinch in on an element a certain amount.")
  @ArgumentNames({"locator", "*args"})
  public static void pinch(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    logger.log(Level.INFO, "Note following parameters are ignored args: " + Arrays.toString(args));

    activeDriver.pinch(activeDriver.findElement(translateLocatorToBy(locator)));
  }

  /**
   * Sends a press of keycode to the device. Android only. Possible keycodes & meta states can be
   * found in http://developer.android.com/reference/android/view/KeyEvent.html Meta state describe
   * the pressed state of key modifiers such as Shift, Ctrl & Alt keys. The Meta State is an integer
   * in which each bit set to 1 represents a pressed meta key. For example META_SHIFT_ON = 1
   * META_ALT_ON = 2
   *
   * @param keycode the keycode to be sent to the device
   * @param args Optional metastate- - status of the meta keys
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Sends a press of keycode to the device.")
  @ArgumentNames({"keycode", "*args"})
  public static void pressKeycode(Integer keycode, String... args) throws Exception {
    logger.log(Level.INFO, "\nkeycode: " + keycode + "\nargs: " + Arrays.toString(args));

    Integer metastate = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("metastate")) {
        metastate = Integer.valueOf(arguement[1]);
      }
    }

    if (metastate == null) {
      ((DriverEmtekAndroid) activeDriver).pressKeyCode(keycode);
    } else {
      ((DriverEmtekAndroid) activeDriver).pressKeyCode(keycode, metastate);
    }
  }

  /**
   * Retrieves the file at path and return it's content. Android only.
   *
   * @param path the path to the file on the device
   * @param args Ignored - encode the data as base64 before writing it to the file (default=False)
   * @return byte array of file base 64 encoded
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Retrieves the file at path and return it's content")
  @ArgumentNames({"path", "*args"})
  public static byte[] pullFile(String path, String... args) throws Exception {
    logger.log(Level.INFO, "\npath: " + path);
    logger.log(Level.INFO, "Note following parameters are ignored. args: " + Arrays.toString(args));

    return activeDriver.pullFile(path);
  }

  /**
   * Retrieves a folder at path. Returns the folder's contents zipped. Android only.
   *
   * @param path the path to the folder on the device
   * @param args Ignored - encode the data as base64 before writing it to the file (default=False)
   * @return The byte representing the zip file
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Retrieves a folder at path. Returns the folder's contents zipped.")
  @ArgumentNames({"path", "*args"})
  public static byte[] pullFolder(String path, String... args) throws Exception {
    logger.log(Level.INFO, "\npath: " + path);
    logger.log(Level.INFO, "Note following parameters are ignored. args: " + Arrays.toString(args));

    return activeDriver.pullFolder(path);
  }

  /**
   * Puts the data in the file specified as path. Android only.
   *
   * @param path the path on the device encode
   * @param data data to be written to the file
   * @param args Ignored - encode the data as base64 before writing it to the file (default=False)
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Retrieves a folder at path. Returns the folder's contents zipped.")
  @ArgumentNames({"path", "data", "*args"})
  public static void pushFile(String path, byte[] data, String... args) throws Exception {
    logger.log(Level.INFO, "\npath: " + path + "\ndata: " + data);
    logger.log(Level.INFO, "Note following parameters are ignored. args: " + Arrays.toString(args));

    ((DriverEmtekAndroid) activeDriver).pushFile(path, data);
  }

  /**
   * Removes the application that is identified with an application id Example: Remove Application
   * com.netease.qa.orangedemo
   *
   * @param applicationId Package
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Remove Application")
  @ArgumentNames({"application_id"})
  public static void removeApplication(String applicationId) throws Exception {
    logger.log(Level.INFO, "\napplication_id: " + applicationId);

    activeDriver.removeApp(applicationId);
  }

  /**
   * Reset application.
   *
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Reset application")
  public static void resetApplication() throws Exception {
    logger.log(Level.INFO, "resetting application");

    activeDriver.resetApp();
  }

  /**
   * Scrolls from one element to another Key attributes for arbitrary elements are id and name. See
   * introduction for details about locating elements.
   *
   * @param startLocator By default, when a locator is provided, it is matched against the key
   *     attributes of the particular element type. For iOS and Android, key attribute is id for all
   *     elements and locating elements is easy using just the id. For example: Click Element
   *     id=my_element
   * @param endLocator By default, when a locator is provided, it is matched against the key
   *     attributes of the particular element type. For iOS and Android, key attribute is id for all
   *     elements and locating elements is easy using just the id. For example: Click Element
   *     id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword(
      "Scrolls from one element to another Key attributes for arbitrary "
          + "elements are id and name. See introduction for details about locating elements.")
  @ArgumentNames({"start_locator", "end_locator"})
  public static void scroll(String startLocator, String endLocator) throws Exception {
    logger.log(Level.INFO, "\nstart_locator: " + startLocator + "\nend_locator: " + endLocator);

    activeDriver.scroll(translateLocatorToBy(startLocator), translateLocatorToBy(endLocator));
  }

  /**
   * Scrolls down to element.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Scrolls down to element")
  @ArgumentNames({"locator"})
  public static void scrollDown(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    long timeout = System.currentTimeMillis() + defaultTimeout * 1000;
    while (!activeDriver.isElementPresent(translateLocatorToBy(locator))) {
      activeDriver.verticalScrollDown();

      if (System.currentTimeMillis() > timeout) {
        logger.exception("Timed out before scrolling down to the following element: " + locator);
      }
    }
  }

  /**
   * Scrolls up to element.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Scrolls up to element")
  @ArgumentNames({"locator"})
  public static void scrollUp(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);

    long timeout = System.currentTimeMillis() + currentTimeout * 1000;
    while (!activeDriver.isElementPresent(translateLocatorToBy(locator))) {
      activeDriver.verticalScrollUp();

      if (System.currentTimeMillis() > timeout) {
        logger.exception("Timed out before scrolling down to the following element: " + locator);
      }
    }
  }

  /**
   * Sets the timeout in seconds used by various keywords. There are several Wait ... keywords that
   * take timeout as an argument. All of these timeout arguments are optional. The timeout used by
   * all of them can be set globally using this keyword. The previous timeout value is returned by
   * this keyword and can be used to set the old value back later. The default timeout is 5 seconds,
   * but it can be altered in importing.
   *
   * @param seconds The seconds
   */
  @RobotKeyword("Sets the timeout in seconds used by various keywords.")
  @ArgumentNames({"seconds"})
  public static void setAppiumTimeout(Integer seconds) {
    logger.log(Level.INFO, "\nseconds: " + seconds);
    activeDriver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
    currentTimeout = seconds;
  }

  /**
   * Sets the network connection Status. Android only. Possible values: Value Alias Data Wifi
   * Airplane Mode 0 (None) 0 0 0 1 (Airplane Mode) 0 0 1 2 (Wifi only) 0 1 0 4 (Data only) 1 0 0 6
   * (All network on) 1 1 0.
   *
   * @param connectionStatus Integer value from description
   */
  @RobotKeyword("Sets the network connection Status. \n Android Only")
  @ArgumentNames({"connectionStatus"})
  public static void setNetworkConnectionStatus(Integer connectionStatus) {
    logger.log(Level.INFO, "\nconnectionStatus: " + connectionStatus);

    Connection con = null;
    if (connectionStatus == 0) {
      con = Connection.NONE;
    } else if (connectionStatus == 1) {
      con = Connection.AIRPLANE;
    } else if (connectionStatus == 2) {
      con = Connection.WIFI;
    } else if (connectionStatus == 4) {
      con = Connection.DATA;
    } else if (connectionStatus == 6) {
      con = Connection.ALL;
    }

    ((DriverEmtekAndroid) activeDriver).setConnection(con);
  }

  /**
   * Swipe from one point to another point, for an optional duration.
   *
   * @param startx start_x - x-coordinate at which to start
   * @param starty start_y - y-coordinate at which to start
   * @param offsetx offset_x - x-coordinate distance from start_x at which to stop
   * @param offsety offset_y - y-coordinate distance from start_y at which to stop
   * @param args duration - (optional) time to take the swipe, in ms.
   */
  @RobotKeyword("Swipe from one point to another point, for an optional duration.")
  @ArgumentNames({"start_x", "start_y", "offset_x", "offset_y", "*args"})
  public static void swipe(
      Integer startx, Integer starty, Integer offsetx, Integer offsety, String... args) {
    logger.log(
        Level.INFO,
        "\nstart_x: "
            + startx
            + "\nstart_y: "
            + starty
            + "\noffset_x: "
            + offsetx
            + "\noffset_y: "
            + offsety
            + "\nargs: "
            + Arrays.toString(args));

    Integer duration = 1000;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("duration")) {
        duration = Integer.valueOf(arguement[1]);
      }
    }

    activeDriver.swipe(startx, starty, offsetx, offsety, duration);
  }

  /**
   * Switch to a new context.
   *
   * @param contextName Name from Get Context
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Switch to a new context")
  @ArgumentNames({"context_name"})
  public static void switchToContext(String contextName) throws Exception {
    logger.log(Level.INFO, "\ncontext_name: " + contextName);
    activeDriver.context(contextName);
  }

  /**
   * Tap on element
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Tap on element")
  @ArgumentNames({"locator"})
  public static void tap(String locator) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    activeDriver.findElement(translateLocatorToBy(locator)).click();
  }

  /**
   * Waits until text appears on current page. Fails if timeout expires before the text appears. See
   * introduction for more information about timeout and its default value. error can be used to
   * override the default error message.
   *
   * @param text The text to appear on page
   * @param args Optional parameters 'timeout' and 'error' message
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Waits until text appears on current page.")
  @ArgumentNames({"locator", "*args"})
  public static void waitUntilPageContains(String text, String... args) throws Exception {
    logger.log(Level.INFO, "\ntext: " + text + "\nargs: " + Arrays.toString(args));

    boolean containsElement = false;
    Integer timeout = currentTimeout;
    String error = "Page does not contain text: " + text;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("timeout")) {
        timeout = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("error")) {
        error = arguement[1];
      }
    }

    containsElement =
        activeDriver.isElementPresent(
            By.xpath(
                "//*[contains(@content-desc,'" + text + "') or contains(@text,'" + text + "')]"),
            timeout);

    if (!containsElement) {
      logger.exception(error);
    }
  }

  /**
   * Waits until element specified with locator appears on current page. Fails if timeout expires
   * before the element appears. See introduction for more information about timeout and its default
   * value. error can be used to override the default error message.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args Optional parameters 'timeout' and 'error' message
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Waits until element specified with locator appears on current page.")
  @ArgumentNames({"locator", "*args"})
  public static void waitUntilPageContainsElement(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nargs: " + Arrays.toString(args));

    boolean containsElement = false;
    Integer timeout = currentTimeout;
    String error = "Page does not contain element: " + locator;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("timeout")) {
        timeout = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("error")) {
        error = arguement[1];
      }
    }

    containsElement = activeDriver.isElementPresent(translateLocatorToBy(locator), timeout);

    if (!containsElement) {
      logger.exception(error);
    }
  }

  /**
   * Waits until text disappears from current page. Fails if timeout expires before the text
   * disappears. See introduction for more information about timeout and its default value. error
   * can be used to override the default error message.
   *
   * @param text The string text
   * @param args Optional parameters 'timeout' and 'error' message
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Waits until text disappears from current page.")
  @ArgumentNames({"text", "*args"})
  public static void waitUntilPageDoesNotContains(String text, String... args) throws Exception {
    logger.log(Level.INFO, "\ntext: " + text + "\bargs: " + Arrays.toString(args));

    Integer timeout = currentTimeout;
    String error = "Page contains text: " + text;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("timeout")) {
        timeout = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("error")) {
        error = arguement[1];
      }
    }

    long totalTimeout = System.currentTimeMillis() + timeout * 1000;
    while (activeDriver.isElementPresent(
        By.xpath(
            "//*[contains(@content-desc,'" + text + "') or contains(@text,'" + text + "')]"))) {
      if (System.currentTimeMillis() > totalTimeout) {
        logger.exception(error);
      }
    }
  }

  /**
   * Waits until element specified with locator disappears from current page. Fails if timeout
   * expires before the element disappears. See introduction for more information about timeout and
   * its default value. error can be used to override the default error message.
   *
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args Optional parameters 'timeout' and 'error' message
   * @throws Exception Logged to logger
   */
  @RobotKeyword("Waits until element specified with locator disappears from current page.")
  @ArgumentNames({"locator", "*args"})
  public static void waitUntilPageDoesNotContainsElement(String locator, String... args)
      throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator + "\nargs: " + Arrays.toString(args));

    Integer timeout = currentTimeout;
    String error = "Page contains element: " + locator;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("timeout")) {
        timeout = Integer.valueOf(arguement[1]);
      } else if (arguement[0].equalsIgnoreCase("error")) {
        error = arguement[1];
      }
    }

    long totalTimeout = System.currentTimeMillis() + timeout * 1000;
    while (activeDriver.isElementPresent(translateLocatorToBy(locator))) {
      if (System.currentTimeMillis() > totalTimeout) {
        logger.exception(error);
      }
    }
  }

  /**
   * Verifies that the page contains the given number of elements located by the given xpath. One
   * should not use the xpath= prefix for 'xpath'. XPath is assumed.
   *
   * @param xpath The xpath
   * @param count The expected count
   * @param args Optional error message.
   * @throws Exception Logged to logger
   */
  @RobotKeyword(
      "Verifies that the page contains the given number of elements "
          + "located by the given xpath.")
  @ArgumentNames({"xpath", "count", "*args"})
  public static void xpathShouldMatchXTimes(String xpath, Integer count, String... args)
      throws Exception {
    logger.log(
        Level.INFO, "\nxpath: " + xpath + "\ncount: " + count + "\nargs: " + Arrays.toString(args));

    String error = null;
    for (int i = 0; i < args.length; ++i) {
      String[] arguement = args[i].split("=");
      if (arguement[0].equalsIgnoreCase("error")) {
        error = arguement[1];
      }
    }

    Integer deviceCount = activeDriver.findElements(translateLocatorToBy(xpath)).size();

    if (deviceCount != count) {
      if (error == null) {
        logger.exception(
            "Xpath: '"
                + xpath
                + "' should have matched '"
                + count
                + "' times but matched '"
                + deviceCount
                + "' times");
      } else {
        logger.exception(error);
      }
    }
  }

  /**
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @param args These additional parameters are ignored
   * @throws Exception Logged to logger \n logged to logger
   */
  @RobotKeyword("Zooms in on an element a certain amount.")
  @ArgumentNames({"locator", "*args"})
  public static void zoom(String locator, String... args) throws Exception {
    logger.log(Level.INFO, "\nlocator: " + locator);
    logger.log(Level.INFO, "The followin are ignored. args: " + Arrays.toString(args));

    activeDriver.zoom(activeDriver.findElement(translateLocatorToBy(locator)));
  }

  /**
   * @param locator By default, when a locator is provided, it is matched against the key attributes
   *     of the particular element type. For iOS and Android, key attribute is id for all elements
   *     and locating elements is easy using just the id. For example: Click Element id=my_element
   * @return The By representation of locator
   */
  private static By translateLocatorToBy(String locator) {
    logger.log(Level.INFO, "\nlocator: " + locator);

    By returnBy = null;
    if (locator.startsWith("xpath=")) {
      returnBy = By.xpath(locator.replace("xpath=", ""));
    } else if (locator.startsWith("//")) {
      returnBy = By.xpath(locator);
    } else if (locator.startsWith("class=")) {
      returnBy = By.className(locator.replace("class=", ""));
    } else if (locator.startsWith("css=")) {
      returnBy = By.cssSelector(locator.replace("css=", ""));
    } else if (locator.startsWith("id=")) {
      returnBy = By.id(locator.replace("id=", ""));
    } else {
      returnBy = By.id(locator);
    }

    logger.log(Level.INFO, "Returning " + returnBy.toString());
    return returnBy;
  }
}
