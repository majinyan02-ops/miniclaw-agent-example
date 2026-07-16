/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.time.Duration;
import java.util.Base64;
import java.util.function.BiFunction;

/**
 * 使用 Selenium WebDriver 的浏览器自动化工具。
 * 
 * 该工具提供网页浏览器自动化能力：
 * - 导航到URL
 * - 点击元素
 * - 提取文本内容
 * - 截取页面截图
 * - 获取页面HTML
 * - 填写输入框
 */
public class BrowserTool implements BiFunction<BrowserTool.BrowserRequest, ToolContext, String> {

	public static final String DESCRIPTION = """
			使用 Selenium WebDriver 执行浏览器自动化任务。
			
			使用说明：
			- action: 要执行的操作（navigate、click、extract、screenshot、get_html、fill、scroll）
			- url: 要访问的URL（navigate操作必须）
			- selector: CSS选择器，用于元素交互（click/extract/fill操作必须）
			- value: 要输入的文本值（fill操作必须）
			- timeout: 最大等待时间，单位毫秒（默认：30000）
			
			支持的操作：
			- navigate: 导航到指定URL（需要url参数）
			- click: 点击页面元素（需要selector参数）
			- extract: 提取元素文本内容（需要selector参数）
			- screenshot: 截取页面截图（返回base64编码图片）
			- get_html: 获取当前页面完整HTML内容
			- fill: 在输入框中填写文本（需要selector和value参数）
			- scroll: 滚动到指定元素（需要selector参数）
			
			示例：
			- 导航：action="navigate", url="https://www.google.com"
			- 点击按钮：action="click", selector="#search-button"
			- 提取标题：action="extract", selector="h1"
			- 填写表单：action="fill", selector="#username", value="john_doe"
			- 截图：action="screenshot"
			""";
	
	private static final Logger log = LoggerFactory.getLogger(BrowserTool.class);
	
	private WebDriver driver;
	private boolean headless;

	/**
	 * 创建新的 BrowserTool 实例。
	 * @param headless 是否以无头模式运行浏览器（无GUI）
	 */
	public BrowserTool(boolean headless) {
		this.headless = headless;
		initializeDriver();
	}
	
	/**
	 * 创建启用无头模式的 BrowserTool 实例。
	 */
	public BrowserTool() {
		this(true); // Default to headless mode
	}

	private void initializeDriver() {
		try {
			log.info("Initializing Chrome WebDriver...");
			
			// Setup WebDriver Manager for automatic driver download
			WebDriverManager.chromedriver().setup();
			
			// Configure Chrome options
			ChromeOptions options = new ChromeOptions();
			if (headless) {
				options.addArguments("--headless=new");
			}
			options.addArguments("--disable-gpu");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--window-size=1920,1080");
			
			// Create driver instance
			this.driver = new ChromeDriver(options);
			this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			
			log.info("Chrome WebDriver initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize WebDriver", e);
			throw new RuntimeException("Failed to initialize WebDriver", e);
		}
	}

	/**
	 * 为浏览器工具创建 ToolCallback。
	 */
	public static ToolCallback createBrowserToolCallback(String description) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool())
				.description(description)
				.inputType(BrowserRequest.class)
				.build();
	}
	
	/**
	 * 为浏览器工具创建带有自定义无头模式设置的 ToolCallback。
	 */
	public static ToolCallback createBrowserToolCallback(String description, boolean headless) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool(headless))
				.description(description)
				.inputType(BrowserRequest.class)
				.build();
	}

	@Override
	public String apply(BrowserRequest request, ToolContext toolContext) {
		if (request.action == null || request.action.trim().isEmpty()) {
			return "Error: Browser action cannot be empty";
		}

		try {
			log.debug("Executing browser action: {} | URL: {} | Selector: {}", 
					request.action, request.url, request.selector);

			String result = switch (request.action.toLowerCase()) {
				case "navigate" -> handleNavigate(request);
				case "click" -> handleClick(request);
				case "extract" -> handleExtract(request);
				case "screenshot" -> handleScreenshot(request);
				case "get_html" -> handleGetHtml(request);
				case "fill" -> handleFill(request);
				case "scroll" -> handleScroll(request);
				default -> "Error: Unknown action '" + request.action + 
						"'. Supported: navigate, click, extract, screenshot, get_html, fill, scroll";
			};

			return result;
		}
		catch (Exception e) {
			log.error("Error executing browser action: " + request.action, e);
			return "Error executing browser action: " + e.getMessage();
		}
	}

	private String handleNavigate(BrowserRequest request) {
		if (request.url == null || request.url.trim().isEmpty()) {
			return "Error: URL is required for navigate action";
		}
		
		try {
			long timeout = request.timeout != null ? request.timeout : 30000;
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeout));
			
			log.info("Navigating to: {}", request.url);
			driver.get(request.url);
			
			// Wait for page to load
			wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
			
			String title = driver.getTitle();
			return "Successfully navigated to: " + request.url + "\nPage Title: " + title;
		}
		catch (Exception e) {
			return "Failed to navigate to " + request.url + ": " + e.getMessage();
		}
	}

	private String handleClick(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for click action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			element.click();
			return "Successfully clicked element: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to click element " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleExtract(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for extract action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			String text = element.getText();
			return "Extracted from " + request.selector + ":\n" + text;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to extract from " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleScreenshot(BrowserRequest request) {
		try {
			TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
			byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
			String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
			// 不要返回完整的 data URI 格式，避免 AI 误点
			return "Screenshot captured successfully!\n" +
					"Size: " + screenshotBytes.length + " bytes\n" +
					"Format: PNG (Base64 encoded)\n" +
					"Note: Screenshot data is available but too large to display. " +
					"You can save it by encoding the binary data to base64.";
		}
		catch (Exception e) {
			return "Failed to take screenshot: " + e.getMessage();
		}
	}

	private String handleGetHtml(BrowserRequest request) {
		try {
			String html = driver.getPageSource();
			int length = html.length();
			if (length > 5000) {
				return "HTML retrieved (" + length + " chars)\nFirst 500 chars:\n" + 
						html.substring(0, 500) + "\n...[truncated]";
			}
			return "HTML (" + length + " chars):\n" + html;
		}
		catch (Exception e) {
			return "Failed to get HTML: " + e.getMessage();
		}
	}

	private String handleFill(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for fill action";
		}
		if (request.value == null) {
			return "Error: Value is required for fill action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			element.clear();
			element.sendKeys(request.value);
			return "Successfully filled '" + request.value + "' into: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to fill element " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleScroll(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for scroll action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
			return "Successfully scrolled to element: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to scroll to element " + request.selector + ": " + e.getMessage();
		}
	}

	private WebElement findElement(String selector) {
		By by = parseSelector(selector);
		return driver.findElement(by);
	}

	private By parseSelector(String selector) {
		// Support various selector formats
		if (selector.startsWith("#")) {
			return By.id(selector.substring(1));
		} else if (selector.startsWith(".")) {
			return By.className(selector.substring(1));
		} else if (selector.matches("^[a-zA-Z][\\w-]*$")) {
			return By.tagName(selector);
		} else {
			return By.cssSelector(selector);
		}
	}

	/**
	 * 关闭浏览器并清理资源。
	 */
	public void close() {
		if (driver != null) {
			log.info("Closing browser...");
			driver.quit();
		}
	}

	/**
	 * 浏览器工具的请求结构。
	 */
	public static class BrowserRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("要执行的浏览器操作")
		public String action;

		@JsonProperty(required = false)
		@JsonPropertyDescription("要导航到的URL（用于navigate操作）")
		public String url;

		@JsonProperty(required = false)
		@JsonPropertyDescription("用于元素交互的CSS选择器")
		public String selector;

		@JsonProperty(required = false)
		@JsonPropertyDescription("要输入的文本值（用于fill操作）")
		public String value;

		@JsonProperty(required = false)
		@JsonPropertyDescription("超时时间，单位毫秒（默认：30000）")
		public Long timeout;

		public BrowserRequest() {
		}

		public BrowserRequest(String action, String url, String selector, String value, Long timeout) {
			this.action = action;
			this.url = url;
			this.selector = selector;
			this.value = value;
			this.timeout = timeout;
		}
	}
}
