/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.getstarted.basicactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.datastore.Batch;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.service.DriverService;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Iterator;
import java.util.List;

@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class UserJourneyTestIT {

  private static final String TITLE = "mytitle";
  private static final String AUTHOR = "myauthor";
  private static final String PUBLISHED_DATE = "1984-02-27";
  private static final String DESCRIPTION = "mydescription";

  private static final String APP_ID = System.getProperty("appengine.appId");
  private static final String APP_VERSION = System.getProperty("appengine.version");
  private static final boolean LOCAL_TEST = null == APP_ID || null == APP_VERSION;

  private static DriverService service;
  private WebDriver driver;

  @BeforeClass
  public static void createAndStartService() throws Exception {
    service = ChromeDriverService.createDefaultService();
    service.start();
  }

  @AfterClass
  public static void createAndStopService() {
    service.stop();

    // Clear the datastore if we're not using the local emulator
    if (!LOCAL_TEST) {
      Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
      Batch batch = datastore.newBatch();
      StructuredQuery<Key> query = Query.newKeyQueryBuilder()
          .setKind("Book2").build();
      for (QueryResults<Key> keys = datastore.run(query); keys.hasNext(); ) {
        batch.delete(keys.next());
      }
      batch.submit();
    }
  }

  @Before
  public void createDriver() {
    driver = new RemoteWebDriver(service.getUrl(), DesiredCapabilities.chrome());
  }

  @After
  public void quitDriver() {
    driver.quit();
  }

  private WebElement checkLandingPage() throws Exception {
    WebElement button = driver.findElement(By.cssSelector("a.btn"));
    assertEquals("Add book", button.getText().trim());

    WebElement heading = driver.findElement(By.cssSelector("body>.container h3"));
    assertEquals("Books", heading.getText());

    WebElement list = driver.findElement(By.cssSelector("body>.container p"));
    assertEquals("No books found", list.getText());

    return button;
  }

  private void checkAddBookPage() throws Exception {
    List<WebElement> inputContainers = driver.findElements(By.cssSelector("form .form-group"));
    assertTrue("Should have more than 4 inputs", inputContainers.size() > 4);
    assertEquals("First input should be Title",
        "Title", inputContainers.get(0).findElement(By.tagName("label")).getText());
    assertEquals("Second input should be Author",
        "Author", inputContainers.get(1).findElement(By.tagName("label")).getText());
    assertEquals("Third input should be Date Published",
        "Date Published", inputContainers.get(2).findElement(By.tagName("label")).getText());
    assertEquals("Fourth input should be Description",
        "Description", inputContainers.get(3).findElement(By.tagName("label")).getText());

    // The rest should be hidden
    for (Iterator<WebElement> iter = inputContainers.listIterator(4); iter.hasNext();) {
      WebElement el = iter.next();
      assertTrue(el.getAttribute("class").indexOf("hidden") >= 0);
    }
  }

  private void submitForm(String title, String author, String datePublished, String description)
      throws Exception {
    WebElement titleEl = driver.findElement(By.cssSelector("[name=title]"));
    titleEl.sendKeys(title);
    WebElement authorEl = driver.findElement(By.cssSelector("[name=author]"));
    authorEl.sendKeys(author);
    WebElement datePublishedEl = driver.findElement(By.cssSelector("[name=publishedDate]"));
    datePublishedEl.sendKeys(datePublished);
    WebElement descriptionEl = driver.findElement(By.cssSelector("[name=description]"));
    descriptionEl.sendKeys(description);

    driver.findElement(By.cssSelector("button[type=submit]")).submit();
  }

  private void checkReadPage(String title, String author, String datePublished, String description)
      throws Exception {
    WebElement heading = driver.findElement(By.cssSelector("h3"));
    assertEquals("Book", heading.getText());

    List<WebElement> buttons = driver.findElements(By.cssSelector("a.btn"));
    assertEquals(2, buttons.size());
    assertEquals("Edit book", buttons.get(0).getText());
    assertEquals("Delete book", buttons.get(1).getText());

    // Should be a cat thumbnail
    assertTrue(driver.findElement(By.cssSelector("img.book-image")).getAttribute("src")
        .indexOf("placekitten") > 0);
    assertTrue("Should show title",
        driver.findElement(By.cssSelector(".book-title")).getText()
        .startsWith(title));
    assertEquals("Should show author",
        "By " + author, driver.findElement(By.cssSelector(".book-author")).getText());
    assertEquals("Should show description",
        description, driver.findElement(By.cssSelector(".book-description")).getText());

    assertTrue(driver.findElement(By.cssSelector(".book-added-by")).getText()
        .indexOf("Anonymous") > 0);
  }

  private void checkBookList(String title, String author, String datePublished, String description)
      throws Exception {
    List<WebElement> media = driver.findElements(By.cssSelector("div.media"));
    assertEquals(1, media.size());

    WebElement book = media.get(0);

    assertEquals(title, book.findElement(By.tagName("h4")).getText());
    assertEquals(author, book.findElement(By.tagName("p")).getText());
  }

  @Test
  public void userJourney() throws Exception {
    // Do selenium tests on the deployed version, if applicable
    String endpoint = "http://localhost:8080";
    if (!LOCAL_TEST) {
      endpoint = String.format("https://%s-dot-%s.appspot.com", APP_VERSION, APP_ID);
    }
    System.out.println("Testing endpoint: " + endpoint);
    driver.get(endpoint);

    try {
      WebElement button = checkLandingPage();

      button.click();
      (new WebDriverWait(driver, 10)).until(ExpectedConditions.urlMatches(".*/create$"));

      checkAddBookPage();

      submitForm(TITLE, AUTHOR, PUBLISHED_DATE, DESCRIPTION);
      (new WebDriverWait(driver, 10)).until(ExpectedConditions.urlMatches(".*/read\\?id=[0-9]+$"));

      checkReadPage(TITLE, AUTHOR, PUBLISHED_DATE, DESCRIPTION);

      // Now check the list of books for the one we just submitted
      driver.findElement(By.linkText("Books")).click();
      (new WebDriverWait(driver, 10)).until(ExpectedConditions.urlMatches(".*/$"));

      checkBookList(TITLE, AUTHOR, PUBLISHED_DATE, DESCRIPTION);
    } catch (Exception e) {
      System.err.println(driver.getPageSource());
      throw e;
    }
  }
}
