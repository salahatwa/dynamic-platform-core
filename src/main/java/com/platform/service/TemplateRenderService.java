package com.platform.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.platform.entity.Template;
import com.platform.entity.TemplateAsset;
import com.platform.entity.TemplatePage;
import com.platform.enums.AssetType;
import com.platform.repository.TemplateAssetRepository;
import com.platform.repository.TemplateRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
@Slf4j
public class TemplateRenderService {

	public final TemplateRepository templateRepository;
	private final TemplateAssetRepository templateAssetRepository;
	private final PlaywrightPdfService playwrightPdfService;
	private final freemarker.template.Configuration freemarkerConfig;
	private final TemplatePageService templatePageService;
	private PdfGenerationService pdfGenerationService; // Lazy injection to avoid circular dependency

	public TemplateRenderService(
			TemplateRepository templateRepository,
			TemplateAssetRepository templateAssetRepository,
			PlaywrightPdfService playwrightPdfService,
			TemplatePageService templatePageService,
			@Qualifier("templateFreemarkerConfiguration") freemarker.template.Configuration freemarkerConfig) {
		this.templateRepository = templateRepository;
		this.templateAssetRepository = templateAssetRepository;
		this.playwrightPdfService = playwrightPdfService;
		this.templatePageService = templatePageService;
		this.freemarkerConfig = freemarkerConfig;
	}

	// Setter for lazy injection to avoid circular dependency
	public void setPdfGenerationService(PdfGenerationService pdfGenerationService) {
		this.pdfGenerationService = pdfGenerationService;
	}

	public String renderHtml(Long templateId, Map<String, Object> parameters) {
		Template templateEntity = templateRepository.findById(templateId)
				.orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + templateId));

		try {
			// Get all pages for this template, ordered by pageOrder
			List<TemplatePage> pages = templatePageService.getAllByTemplate(templateId);
			
			if (pages.isEmpty()) {
				// Fallback to template's own HTML content if no pages exist
				log.info("No pages found for template {}, using template's own HTML content", templateId);
				return renderSingleContent(templateEntity.getHtmlContent(), templateEntity.getName(), parameters);
			}

			log.info("Page size:{}",pages.size());
			// Prepare model
			Map<String, Object> model = parameters != null ? parameters : new HashMap<>();
			
			// Filter out empty pages before rendering
			List<TemplatePage> nonEmptyPages = pages.stream()
				.filter(this::hasContent)
				.collect(java.util.stream.Collectors.toList());
			
			if (nonEmptyPages.isEmpty()) {
				log.warn("All pages of template {} are empty, returning empty document", templateId);
				return "<html><head><title>Empty Document</title></head><body><p>No content available</p></body></html>";
			}
			
			// If there's only one page, return it directly without multi-page wrapper and page breaks
			if (nonEmptyPages.size() == 1) {
				TemplatePage singlePage = nonEmptyPages.get(0);
				String renderedContent = renderSingleContent(singlePage.getContent(), singlePage.getName(), model);
				log.info("Rendered single page for template {} without page breaks", templateId);
				return renderedContent;
			}
			
			// Render multiple non-empty pages and combine them with proper page breaks
			StringBuilder combinedHtml = new StringBuilder();
			
			// Start with a wrapper div for all pages
			combinedHtml.append("<div class=\"multi-page-document\">");
			
			for (int i = 0; i < nonEmptyPages.size(); i++) {
				TemplatePage page = nonEmptyPages.get(i);
				
				// Render this page's content
				String renderedPageContent = renderSingleContent(page.getContent(), page.getName(), model);
				
				// Wrap each page in a container with minimal styling
				combinedHtml.append("<div class=\"template-page template-page-").append(i + 1).append("\"");
				
				// Only add page break for pages after the first one
				if (i > 0) {
					combinedHtml.append(" style=\"page-break-before: always;\"");
				}
				combinedHtml.append(">");
				
				// Add page content
				combinedHtml.append(renderedPageContent);
				combinedHtml.append("</div>");
				
				// No explicit page break divs - CSS handles page breaks
			}
			
			// Close wrapper div
			combinedHtml.append("</div>");

			log.info("Rendered {} non-empty pages out of {} total pages for template {} (multi-page mode)", 
				nonEmptyPages.size(), pages.size(), templateId);
			return combinedHtml.toString();

		} catch (Exception e) {
			log.error("Failed to render template {}: {}", templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to render template", e);
		}
	}

	/**
	 * Render a specific page of a template
	 */
	public String renderSpecificPage(Long templateId, Integer pageNumber, Map<String, Object> parameters) {
		Template templateEntity = templateRepository.findById(templateId)
				.orElseThrow(() -> new IllegalArgumentException("Template not found with id: " + templateId));

		try {
			// Get all pages for this template, ordered by pageOrder
			List<TemplatePage> pages = templatePageService.getAllByTemplate(templateId);
			
			if (pages.isEmpty()) {
				// Fallback to template's own HTML content if no pages exist
				log.info("No pages found for template {}, using template's own HTML content", templateId);
				return renderSingleContent(templateEntity.getHtmlContent(), templateEntity.getName(), parameters);
			}

			// Validate page number
			if (pageNumber == null || pageNumber < 1 || pageNumber > pages.size()) {
				throw new IllegalArgumentException("Invalid page number: " + pageNumber + 
					". Template has " + pages.size() + " pages.");
			}

			// Get the specific page (pageNumber is 1-based, list is 0-based)
			TemplatePage page = pages.get(pageNumber - 1);
			
			if (!hasContent(page)) {
				log.warn("Page {} of template {} has no content, returning empty page message", pageNumber, templateId);
				return "<html><head><title>Empty Page</title></head><body><p>Page " + pageNumber + " is empty</p></body></html>";
			}

			// Prepare model
			Map<String, Object> model = parameters != null ? parameters : new HashMap<>();
			
			// Render the specific page
			String renderedContent = renderSingleContent(page.getContent(), page.getName(), model);
			
			log.info("Rendered page {} of template {}", pageNumber, templateId);
			return renderedContent;

		} catch (Exception e) {
			log.error("Failed to render page {} of template {}: {}", pageNumber, templateId, e.getMessage(), e);
			throw new RuntimeException("Failed to render page " + pageNumber + " of template", e);
		}
	}

	/**
	 * Get the number of pages in a template
	 */
	public int getPageCount(Long templateId) {
		List<TemplatePage> pages = templatePageService.getAllByTemplate(templateId);
		if (pages.isEmpty()) {
			return 1; // Return 1 if no pages (template has its own content)
		}
		
		// Count only non-empty pages
		long nonEmptyPageCount = pages.stream()
			.filter(page -> page.getContent() != null && !page.getContent().trim().isEmpty())
			.count();
		
		return (int) Math.max(1, nonEmptyPageCount); // Return at least 1 page
	}

	/**
	 * Get the number of non-empty pages in a template
	 */
	public int getNonEmptyPageCount(Long templateId) {
		List<TemplatePage> pages = templatePageService.getAllByTemplate(templateId);
		if (pages.isEmpty()) {
			return 1; // Return 1 if no pages (template has its own content)
		}
		
		// Count only non-empty pages
		long nonEmptyPageCount = pages.stream()
			.filter(page -> page.getContent() != null && !page.getContent().trim().isEmpty())
			.count();
		
		return (int) nonEmptyPageCount;
	}

	/**
	 * Check if a page has meaningful content
	 */
	private boolean hasContent(TemplatePage page) {
		if (page == null || page.getContent() == null) {
			return false;
		}
		
		String content = page.getContent().trim();
		if (content.isEmpty()) {
			return false;
		}
		
		// Check if content is just whitespace, HTML tags with no text, or common empty patterns
		String textContent = content.replaceAll("<[^>]*>", "").trim(); // Remove HTML tags
		textContent = textContent.replaceAll("&nbsp;", "").trim(); // Remove non-breaking spaces
		textContent = textContent.replaceAll("\\s+", "").trim(); // Remove all whitespace
		
		return !textContent.isEmpty();
	}

	/**
	 * Render a single piece of content (either template content or page content)
	 */
	private String renderSingleContent(String content, String name, Map<String, Object> model) {
		try {
			// Extract all variables from content and provide defaults for missing ones
			Map<String, Object> enhancedModel = enhanceModelWithDefaults(content, model);
			
			// Protect critical variables from being overridden by loop variables
			Map<String, Object> protectedModel = protectVariablesFromOverride(content, enhancedModel);
			
			// Create FreeMarker template from content string
			freemarker.template.Template freemarkerTemplate = new freemarker.template.Template(
					name, new StringReader(content), freemarkerConfig);

			// Render
			StringWriter writer = new StringWriter();
			freemarkerTemplate.process(protectedModel, writer);

			return writer.toString();

		} catch (Exception e) {
			log.error("Failed to render content for {}: {}", name, e.getMessage());
			throw new RuntimeException("Failed to render content for " + name, e);
		}
	}

	/**
	 * Enhance the model by providing default empty values for any missing variables
	 */
	private Map<String, Object> enhanceModelWithDefaults(String content, Map<String, Object> originalModel) {
		Map<String, Object> enhancedModel = new HashMap<>(originalModel != null ? originalModel : new HashMap<>());
		
		log.debug("Original model contains: {}", originalModel != null ? originalModel.keySet() : "null");
		
		// Extract all variables from the content
		Map<String, Object> extractedVars = extractParameters(content);
		log.debug("Extracted variables: {}", extractedVars.keySet());
		
		// For each extracted variable, provide a default empty value if not present in the model
		// or if the existing value is incompatible with its usage pattern
		for (String varName : extractedVars.keySet()) {
			Object currentValue = enhancedModel.get(varName);
			
			if (currentValue == null || !isValueCompatibleWithUsage(varName, currentValue, content)) {
				log.debug("Variable '{}' needs default value (current: {})", varName, currentValue);
				Object defaultValue = determineDefaultValue(varName, content);
				enhancedModel.put(varName, defaultValue);
				log.debug("Providing default value for variable '{}': {}", varName, defaultValue);
			} else {
				log.debug("Variable '{}' already exists with compatible value: {}", varName, currentValue);
			}
		}
		
		log.debug("Enhanced model contains: {}", enhancedModel.keySet());
		return enhancedModel;
	}

	/**
	 * Check if the current value is compatible with how the variable is used in the template
	 */
	private boolean isValueCompatibleWithUsage(String varName, Object currentValue, String content) {
		// Check if variable is used as an object
		Pattern objectPattern = Pattern.compile("\\$\\{" + Pattern.quote(varName) + "\\.(\\w+)\\}");
		if (objectPattern.matcher(content).find()) {
			// Variable is used as object, current value should be a Map
			boolean isCompatible = currentValue instanceof Map;
			log.debug("Variable '{}' used as object, current value compatible: {} (type: {})", 
				varName, isCompatible, currentValue != null ? currentValue.getClass().getSimpleName() : "null");
			return isCompatible;
		}
		
		// Check if variable is used in a list iteration
		Pattern listPattern = Pattern.compile("<#list\\s+" + Pattern.quote(varName) + "\\s+as\\s+\\w+>");
		if (listPattern.matcher(content).find()) {
			// Variable is used as array, current value should be a List
			boolean isCompatible = currentValue instanceof List;
			log.debug("Variable '{}' used as array, current value compatible: {} (type: {})", 
				varName, isCompatible, currentValue != null ? currentValue.getClass().getSimpleName() : "null");
			return isCompatible;
		}
		
		// For simple variables, any non-null value is compatible
		return currentValue != null;
	}

	/**
	 * Determine appropriate default value based on how the variable is used in the template
	 */
	private Object determineDefaultValue(String varName, String content) {
		log.debug("Determining default value for variable '{}' in content", varName);
		
		// Check if variable is used as an object (has property access)
		Pattern objectPattern = Pattern.compile("\\$\\{" + Pattern.quote(varName) + "\\.(\\w+)\\}");
		Matcher objectMatcher = objectPattern.matcher(content);
		
		Map<String, Object> objectValue = new HashMap<>();
		boolean isObject = false;
		
		// Find all properties accessed on this object
		while (objectMatcher.find()) {
			isObject = true;
			String property = objectMatcher.group(1);
			objectValue.put(property, ""); // Default property value
			log.debug("Found property '{}' for variable '{}'", property, varName);
		}
		
		if (isObject) {
			log.debug("Creating object default for '{}' with properties: {}", varName, objectValue.keySet());
			return objectValue;
		}
		
		// Check if variable is used in a list iteration
		Pattern listPattern = Pattern.compile("<#list\\s+" + Pattern.quote(varName) + "\\s+as\\s+\\w+>");
		if (listPattern.matcher(content).find()) {
			log.debug("Creating empty list default for '{}'", varName);
			return new ArrayList<>();
		}
		
		// Default to empty string for simple variables
		log.debug("Creating string default for '{}'", varName);
		return "";
	}

	/**
	 * Protect variables from being overridden by loop variables or other template constructs
	 */
	private Map<String, Object> protectVariablesFromOverride(String content, Map<String, Object> model) {
		Map<String, Object> protectedModel = new HashMap<>(model);
		
		// Find variables that are used as objects (e.g., exp.role)
		Pattern objectUsagePattern = Pattern.compile("\\$\\{(\\w+)\\.(\\w+)\\}");
		Matcher objectMatcher = objectUsagePattern.matcher(content);
		
		Set<String> objectVariables = new HashSet<>();
		while (objectMatcher.find()) {
			String varName = objectMatcher.group(1);
			objectVariables.add(varName);
		}
		
		// Find loop variables that might override object variables
		Pattern loopPattern = Pattern.compile("<#list\\s+(\\w+)\\s+as\\s+(\\w+)>");
		Matcher loopMatcher = loopPattern.matcher(content);
		
		while (loopMatcher.find()) {
			String arrayVar = loopMatcher.group(1);
			String loopVar = loopMatcher.group(2);
			
			// If a loop variable has the same name as an object variable, it will override it
			if (objectVariables.contains(loopVar)) {
				Object originalValue = protectedModel.get(loopVar);
				if (originalValue != null && originalValue instanceof Map) {
					// Store the original object under a protected name
					String protectedName = loopVar + "_original";
					protectedModel.put(protectedName, originalValue);
					log.warn("Variable '{}' is used both as object and loop variable. Original stored as '{}'", 
						loopVar, protectedName);
				}
			}
		}
		
		// For the specific case we're seeing, ensure exp is always an object if used as one
		if (objectVariables.contains("exp")) {
			Object expValue = protectedModel.get("exp");
			if (expValue == null || !(expValue instanceof Map)) {
				// Force exp to be an object with the properties we detected
				Map<String, Object> expObject = new HashMap<>();
				
				// Extract all exp properties from the template
				Pattern expPropsPattern = Pattern.compile("\\$\\{exp\\.(\\w+)\\}");
				Matcher expPropsMatcher = expPropsPattern.matcher(content);
				while (expPropsMatcher.find()) {
					String prop = expPropsMatcher.group(1);
					expObject.put(prop, "");
				}
				
				if (!expObject.isEmpty()) {
					protectedModel.put("exp", expObject);
					log.debug("Forced 'exp' to be object with properties: {}", expObject.keySet());
				}
			}
		}
		
		return protectedModel;
	}

	public byte[] renderToPdf(Long templateId, Map<String, Object> parameters) throws DocumentException, IOException {
		return renderToPdf(templateId, parameters, null);
	}

	public byte[] renderToPdf(Long templateId, Map<String, Object> parameters, Integer pageNumber) throws DocumentException, IOException {

		Template template = templateRepository.findById(templateId)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		log.info("Generating PDF for template {} with orientation: {}", templateId, template.getPageOrientation());

		// Use new PdfGenerationService if available, otherwise fallback to legacy implementation
		if (pdfGenerationService != null) {
			try {
				return pdfGenerationService.generatePdf(templateId, parameters, pageNumber, template.getPageOrientation());
			} catch (Exception e) {
				log.warn("PdfGenerationService failed, falling back to legacy implementation: {}", e.getMessage());
			}
		}

		// Legacy implementation (Flying Saucer only)
		log.info("Using legacy PDF generation (Flying Saucer only)");
		
		// Render HTML with parameters - either specific page or all pages
		String processedHtml;
		if (pageNumber != null) {
			// Render specific page
			processedHtml = renderSpecificPage(templateId, pageNumber, parameters);
			log.info("Rendering PDF for page {} of template {}", pageNumber, templateId);
		} else {
			// Render all pages
			processedHtml = renderHtml(templateId, parameters);
			log.info("Rendering PDF for all pages of template {}", templateId);
		}

		// Enhance HTML for better PDF rendering with page orientation
		String enhancedHtml = enhanceHtmlForPdf(processedHtml, template.getCssStyles(), template.getPageOrientation());

		// Try Playwright first (best quality), fallback to Flying Saucer if unavailable
		try {
			byte[] pdf = playwrightPdfService.generatePdfFromHtml(enhancedHtml, pageNumber, template.getPageOrientation());
			log.info("PDF generated using Playwright (high quality) - {} pages, orientation: {}", 
				pageNumber != null ? "page " + pageNumber : "all", template.getPageOrientation());
			return pdf;
		} catch (Exception e) {
			log.warn("Playwright PDF generation failed, falling back to Flying Saucer: {}", e.getMessage());

			// Fallback to Flying Saucer
			String xhtml = buildXhtmlDocument(enhancedHtml, template.getCssStyles(), template.getPageOrientation());
			byte[] pdf = convertToPdf(xhtml, templateId);
			log.info("PDF generated using Flying Saucer (fallback) - {} pages, orientation: {}", 
				pageNumber != null ? "page " + pageNumber : "all", template.getPageOrientation());
			return pdf;
		}
	}

	public String enhanceHtmlForPdf(String html, String css, com.platform.enums.PageOrientation orientation) {
		try {
			// Parse HTML with JSoup for better handling
			org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
			
			// Ensure proper page structure
			if (doc.head() == null) {
				doc.prependElement("head");
			}
			
			// Add viewport meta tag for better rendering
			if (doc.select("meta[name=viewport]").isEmpty()) {
				doc.head().appendElement("meta")
					.attr("name", "viewport")
					.attr("content", "width=device-width, initial-scale=1.0");
			}
			
			// Add CSS if not already present
			if (css != null && !css.trim().isEmpty() && doc.select("style").isEmpty()) {
				doc.head().appendElement("style")
					.attr("type", "text/css")
					.text(css);
			}
			
			// Ensure body has proper structure
			if (doc.body() == null) {
				doc.appendElement("body");
			}
			
			// Add print-friendly CSS with orientation
			String printCss = generatePrintFriendlyCss(orientation);
			log.info("Adding print CSS for orientation {}: {}", orientation, printCss.substring(0, Math.min(200, printCss.length())) + "...");
			doc.head().appendElement("style")
				.attr("type", "text/css")
				.attr("media", "print")
				.text(printCss);
			
			return doc.outerHtml();
			
		} catch (Exception e) {
			log.warn("Failed to enhance HTML, using original: {}", e.getMessage());
			return html;
		}
	}

	private String generatePrintFriendlyCss(com.platform.enums.PageOrientation orientation) {
		String pageSize = orientation.isLandscape() ? "A4 landscape" : "A4 portrait";
		
		log.info("Generating print CSS with page size: {} for orientation: {}", pageSize, orientation);
		
		return String.format("""
			@page {
				size: %s;
				margin: 1cm;
			}
			
			/* Multi-page document container */
			.multi-page-document {
				width: 100%%;
			}
			
			/* Individual template pages */
			.template-page {
				width: 100%%;
				box-sizing: border-box;
				page-break-inside: avoid;
				page-break-after: auto;
				margin-bottom: 0;
				padding-bottom: 0;
			}
			
			/* Force page breaks between template pages */
			.template-page:not(:first-child) {
				page-break-before: always !important;
			}
			
			/* Prevent page breaks inside certain elements */
			.no-page-break {
				page-break-inside: avoid;
			}
			
			/* Print-specific styles */
			@media print {
				.no-print {
					display: none !important;
				}
				
				.multi-page-document {
					width: 100%%;
				}
				
				.template-page {
					width: 100%%;
					margin: 0;
					padding: 0;
				}
				
				.template-page:not(:first-child) {
					page-break-before: always !important;
				}
			}
		""", pageSize);
	}

	private String buildXhtmlDocument(String content, String css, com.platform.enums.PageOrientation orientation) {
		try {
			// Use JSoup to parse and clean HTML
			org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);

			// Configure for XHTML output
			doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
					.escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

			// Get body elements and convert to XHTML
			org.jsoup.nodes.Element body = doc.body();

			// Process CSS to make it PDF-friendly with orientation
			String processedCss = processCssForPdf(css, orientation);

			// Build a simple, valid XHTML document
			StringBuilder xhtml = new StringBuilder();
			xhtml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			xhtml.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
			xhtml.append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
			xhtml.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
			xhtml.append("<head>\n");
			xhtml.append("<title>Document</title>\n");
			xhtml.append("<style type=\"text/css\">\n");
			xhtml.append("/*<![CDATA[*/\n");

			// Add processed CSS
			xhtml.append(processedCss);

			xhtml.append("\n/*]]>*/\n");
			xhtml.append("</style>\n");
			xhtml.append("</head>\n");
			xhtml.append("<body>\n");

			// Add body content - JSoup ensures it's well-formed XML
			for (org.jsoup.nodes.Node child : body.childNodes()) {
				xhtml.append(child.outerHtml());
			}

			xhtml.append("\n</body>\n");
			xhtml.append("</html>");

			String result = xhtml.toString();

			// Log for debugging
			log.info("Generated XHTML document, length: {} characters", result.length());
			if (log.isDebugEnabled()) {
				log.debug("XHTML content:\n{}", result);
			}

			return result;
		} catch (Exception e) {
			log.error("Error building XHTML document", e);
			throw new RuntimeException("Failed to build XHTML document: " + e.getMessage(), e);
		}
	}

	private String processCssForPdf(String css, com.platform.enums.PageOrientation orientation) {
		String pageSize = orientation.isLandscape() ? "A4 landscape" : "A4 portrait";
		
		StringBuilder processed = new StringBuilder();
		
		// Add page configuration first
		processed.append(String.format("@page { size: %s; margin: 1cm; }\n", pageSize));
		
		// Add multi-page support CSS
		processed.append("""
			/* Multi-page document container */
			.multi-page-document {
				width: 100%;
			}
			
			/* Individual template pages */
			.template-page {
				min-height: 100vh;
				height: auto;
				width: 100%;
				box-sizing: border-box;
				page-break-inside: avoid;
				page-break-after: auto;
				position: relative;
				overflow: visible;
			}
			
			/* Force page breaks between template pages */
			.template-page:not(:first-child) {
				page-break-before: always !important;
			}
			
			/* Explicit page break elements */
			.explicit-page-break,
			.page-break {
				page-break-before: always !important;
				page-break-after: avoid !important;
				height: 0 !important;
				margin: 0 !important;
				padding: 0 !important;
				border: none !important;
				display: block !important;
				clear: both !important;
				visibility: hidden;
			}
			
			""");
		
		if (css == null || css.isEmpty()) {
			processed.append("body { font-family: Arial, sans-serif; font-size: 10pt; line-height: 1.4; margin: 0; padding: 0; }\n");
		} else {
			// Process existing CSS for PDF compatibility
			String existingCss = css;

			// Replace flexbox with table-based layout for PDF compatibility
			existingCss = existingCss.replaceAll("display:\\s*flex", "display: table");
			existingCss = existingCss.replaceAll("display:\\s*inline-flex", "display: inline-table");

			// Remove flex-specific properties that aren't supported
			existingCss = existingCss.replaceAll("flex-wrap:\\s*[^;]+;", "");
			existingCss = existingCss.replaceAll("flex-direction:\\s*[^;]+;", "");
			existingCss = existingCss.replaceAll("justify-content:\\s*[^;]+;", "");
			existingCss = existingCss.replaceAll("align-items:\\s*[^;]+;", "");
			existingCss = existingCss.replaceAll("gap:\\s*[^;]+;", "");

			// Ensure min-height is converted to height for PDF
			existingCss = existingCss.replaceAll("min-height:\\s*100%", "height: auto");

			processed.append(existingCss);
		}

		// Add Flying Saucer specific page break control
		processed.append("\n* { -fs-table-paginate: paginate; }\n");
		processed.append(".resume { page-break-inside: avoid; }\n");

		log.debug("Processed CSS for PDF compatibility with multi-page support");

		return processed.toString();
	}

	private byte[] convertToPdf(String xhtml, Long templateId) throws DocumentException, IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ITextRenderer renderer = new ITextRenderer();

		// Register custom fonts
		List<TemplateAsset> fonts = templateAssetRepository.findByTemplateIdAndAssetType(templateId, AssetType.FONT);

		for (TemplateAsset font : fonts) {
			try {
				renderer.getFontResolver().addFont(font.getFilePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
				log.info("Registered font: {}", font.getName());
			} catch (Exception e) {
				log.error("Failed to register font: {}", font.getName(), e);
			}
		}

		renderer.setDocumentFromString(xhtml);
		renderer.layout();
		renderer.createPDF(outputStream);

		return outputStream.toByteArray();
	}

	public Map<String, Object> extractParameters(String htmlContent) {
		// Extract ${parameter} placeholders from FreeMarker templates
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
		java.util.regex.Matcher matcher = pattern.matcher(htmlContent);

		Map<String, Object> parameters = new java.util.HashMap<>();
		while (matcher.find()) {
			String fullExpression = matcher.group(1).trim();
			// Extract the base variable name (before any dots or brackets)
			String paramName = extractVariableName(fullExpression);
			if (paramName != null && !paramName.isEmpty()) {
				parameters.put(paramName, "");
				log.debug("Extracted FreeMarker parameter: {} from expression: {}", paramName, fullExpression);
			}
		}

		return parameters;
	}

	/**
	 * Extract the base variable name from a FreeMarker expression
	 * Examples: "user.name" -> "user", "items[0]" -> "items", "count" -> "count"
	 */
	private String extractVariableName(String expression) {
		if (expression == null || expression.trim().isEmpty()) {
			return null;
		}
		
		String trimmed = expression.trim();
		
		// Handle object property access: user.name -> user
		if (trimmed.contains(".")) {
			return trimmed.split("\\.")[0];
		}
		
		// Handle array access: items[0] -> items
		if (trimmed.contains("[")) {
			return trimmed.split("\\[")[0];
		}
		
		// Simple variable
		return trimmed;
	}
}
