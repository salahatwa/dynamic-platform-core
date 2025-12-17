package com.platform.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.platform.dto.TemplatePreviewRequest;
import com.platform.entity.App;
import com.platform.entity.Template;
import com.platform.entity.User;

import com.platform.enums.PermissionAction;
import com.platform.enums.PermissionResource;
import com.platform.repository.AppRepository;
import com.platform.repository.TemplateRepository;
import com.platform.repository.UserRepository;
import com.platform.security.RequirePermission;
import com.platform.security.UserPrincipal;
import com.platform.service.TemplateRenderService;
import com.platform.service.WordGenerationService;
import com.platform.service.PdfGenerationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/template-editor")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Template Editor", description = "Template management")
public class TemplateController {

	private final TemplateRepository templateRepository;
	private final UserRepository userRepository;
	private final AppRepository appRepository;
	private final TemplateRenderService templateRenderService;
	private final WordGenerationService wordGenerationService;
	private final PdfGenerationService pdfGenerationService;

	@GetMapping
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Get all templates")
	public ResponseEntity<?> getAllTemplates(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size, @RequestParam(required = false) String search,
			@RequestParam(required = false) String appName) {

		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Template> templates;

		if (appName != null && !appName.isEmpty()) {
			// App-centric filtering: get templates for specific app
			if (search != null && !search.isEmpty()) {
				templates = templateRepository.findByApp_NameAndCorporateIdAndNameContainingIgnoreCase(appName,
						currentUser.getCorporate().getId(), search, pageable);
			} else {
				templates = templateRepository.findByApp_NameAndCorporateId(appName, currentUser.getCorporate().getId(),
						pageable);
			}
		} else {
			// Fallback to corporate-based filtering for backward compatibility
			if (search != null && !search.isEmpty()) {
				templates = templateRepository.findByCorporateIdAndNameContainingIgnoreCase(
						currentUser.getCorporate().getId(), search, pageable);
			} else {
				templates = templateRepository.findByCorporateId(currentUser.getCorporate().getId(), pageable);
			}
		}

		return ResponseEntity.ok(templates);
	}

	@GetMapping("/{id}")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Get template by ID")
	public ResponseEntity<?> getTemplate(@PathVariable Long id, @RequestParam(required = false) String appName) {
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		// Check corporate access
		if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
			return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
		}

		// If appName is specified, also check app access
		if (appName != null && !appName.isEmpty()) {
			if (template.getApp() == null || !template.getApp().getName().equals(appName)) {
				return ResponseEntity.status(403).body("Access denied: Template does not belong to the specified app");
			}
		}

		return ResponseEntity.ok(template);
	}

	@PostMapping
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.CREATE)
	@Operation(summary = "Create template")
	public ResponseEntity<?> createTemplate(@RequestBody Template template,
			@RequestParam(required = false) String appName) {
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		template.setCorporate(currentUser.getCorporate());

		// If appName is provided, find and set the app
		if (appName != null && !appName.isEmpty()) {
			App app = appRepository.findByCorporateIdAndName(currentUser.getCorporate().getId(), appName)
					.orElseThrow(() -> new RuntimeException("App not found or does not belong to your organization"));
			template.setApp(app);
		}

		Template created = templateRepository.save(template);

		return ResponseEntity.ok(created);
	}

	@PutMapping("/{id}")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.UPDATE)
	@Operation(summary = "Update template")
	public ResponseEntity<?> updateTemplate(@PathVariable Long id, @RequestBody Template templateUpdate,
			@RequestParam(required = false) String appName) {
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		// Check corporate access
		if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
			return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
		}

		// If appName is specified, also check app access
		if (appName != null && !appName.isEmpty()) {
			if (template.getApp() == null || !template.getApp().getName().equals(appName)) {
				return ResponseEntity.status(403).body("Access denied: Template does not belong to the specified app");
			}
		}

		// Update fields
		template.setName(templateUpdate.getName());
		template.setType(templateUpdate.getType());
		template.setHtmlContent(templateUpdate.getHtmlContent());
		template.setCssStyles(templateUpdate.getCssStyles());
		template.setSubject(templateUpdate.getSubject());
		template.setPageOrientation(templateUpdate.getPageOrientation());

		Template updated = templateRepository.save(template);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.DELETE)
	@Operation(summary = "Delete template")
	public ResponseEntity<?> deleteTemplate(@PathVariable Long id, @RequestParam(required = false) String appName) {
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		// Check corporate access
		if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
			return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
		}

		// If appName is specified, also check app access
		if (appName != null && !appName.isEmpty()) {
			if (template.getApp() == null || !template.getApp().getName().equals(appName)) {
				return ResponseEntity.status(403).body("Access denied: Template does not belong to the specified app");
			}
		}

		templateRepository.delete(template);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{id}/preview-pdf")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Preview template as PDF")
	public ResponseEntity<?> previewPdf(@PathVariable Long id, @RequestBody TemplatePreviewRequest request,
			HttpServletRequest httpRequest) {

		try {
			// Validate access to template
			User currentUser = getCurrentUserWithCorporate();
			if (currentUser == null || currentUser.getCorporate() == null) {
				return ResponseEntity.badRequest().body("User not associated with any organization");
			}

			Template template = templateRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Template not found"));

			// Check corporate access
			if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
				return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
			}

			// Validate template has content (either in template itself or in pages)
			int pageCount = templateRenderService.getPageCount(id);
			boolean hasTemplateContent = template.getHtmlContent() != null && !template.getHtmlContent().trim().isEmpty();
			boolean hasPageContent = pageCount > 0;
			
			if (!hasTemplateContent && !hasPageContent) {
				return ResponseEntity.badRequest().body("Template has no content to render");
			}

			// Generate PDF with optional page number
			byte[] pdf = templateRenderService.renderToPdf(id, request.getParameters(), request.getPageNumber());

			if (pdf == null || pdf.length == 0) {
				return ResponseEntity.status(500).body("Failed to generate PDF - empty result");
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			
			// Get page count information
			int totalPages = templateRenderService.getPageCount(id);
			
			// Set filename based on template name and page number
			String filename = template.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
			if (request.getPageNumber() != null) {
				filename += "_page_" + request.getPageNumber();
			}
			filename += ".pdf";
			
			headers.setContentDispositionFormData("inline", filename);
			headers.add("X-Template-Id", id.toString());
			headers.add("X-Template-Name", template.getName());
			headers.add("X-PDF-Size", String.valueOf(pdf.length));
			headers.add("X-Total-Pages", String.valueOf(totalPages));
			
			if (request.getPageNumber() != null) {
				headers.add("X-Page-Number", request.getPageNumber().toString());
			} else {
				headers.add("X-Rendered-Pages", "all");
			}

			return ResponseEntity.ok().headers(headers).body(pdf);

		} catch (Exception e) {
			// Log the error for debugging
			log.error("Error generating PDF for template {}: {}", id, e.getMessage(), e);
			
			// Return appropriate error response
			if (e.getMessage().contains("not found")) {
				return ResponseEntity.notFound().build();
			} else if (e.getMessage().contains("Access denied")) {
				return ResponseEntity.status(403).body(e.getMessage());
			} else {
				return ResponseEntity.status(500).body("Failed to generate PDF: " + e.getMessage());
			}
		}
	}

	@PostMapping("/{id}/preview-word")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Preview template as Word document")
	public ResponseEntity<byte[]> previewWord(@PathVariable Long id, @RequestBody TemplatePreviewRequest request,
			HttpServletRequest httpRequest) throws IOException {

		// Validate access to template
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			throw new RuntimeException("User not associated with any organization");
		}

		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		// Check corporate access
		if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
			throw new RuntimeException("Access denied: Template belongs to another organization");
		}

		// Render HTML with parameters
		String processedHtml = templateRenderService.renderHtml(id, request.getParameters());

		// Convert to Word
		byte[] docx = wordGenerationService.convertHtmlToWord(processedHtml);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(
				MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
		headers.setContentDispositionFormData("inline", template.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".docx");

		return ResponseEntity.ok().headers(headers).body(docx);
	}

	@GetMapping("/{id}/info")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Get template information including page count")
	public ResponseEntity<?> getTemplateInfo(@PathVariable Long id, @RequestParam(required = false) String appName) {
		User currentUser = getCurrentUserWithCorporate();
		if (currentUser == null || currentUser.getCorporate() == null) {
			return ResponseEntity.badRequest().body("User not associated with any organization");
		}

		Template template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		// Check corporate access
		if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
			return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
		}

		// If appName is specified, also check app access
		if (appName != null && !appName.isEmpty()) {
			if (template.getApp() == null || !template.getApp().getName().equals(appName)) {
				return ResponseEntity.status(403).body("Access denied: Template does not belong to the specified app");
			}
		}

		// Get page count
		int pageCount = templateRenderService.getPageCount(id);
		
		// Get template parameters (from template content or pages)
		Map<String, Object> parameters = new HashMap<>();
		if (template.getHtmlContent() != null && !template.getHtmlContent().trim().isEmpty()) {
			parameters = templateRenderService.extractParameters(template.getHtmlContent());
		} else if (pageCount > 0) {
			// Extract parameters from all pages
			try {
				String allPagesContent = templateRenderService.renderHtml(id, new HashMap<>());
				parameters = templateRenderService.extractParameters(allPagesContent);
			} catch (Exception e) {
				log.warn("Could not extract parameters from pages for template {}: {}", id, e.getMessage());
			}
		}

		// Check if template has content (either in template itself or in pages)
		boolean hasTemplateContent = template.getHtmlContent() != null && !template.getHtmlContent().trim().isEmpty();
		boolean hasPageContent = pageCount > 0;
		boolean hasContent = hasTemplateContent || hasPageContent;

		// Create response with template info
		Map<String, Object> templateInfo = new HashMap<>();
		templateInfo.put("id", template.getId());
		templateInfo.put("name", template.getName());
		templateInfo.put("type", template.getType());
		templateInfo.put("subject", template.getSubject());
		templateInfo.put("parameters", parameters);
		templateInfo.put("pageCount", pageCount);
		templateInfo.put("hasContent", hasContent);
		templateInfo.put("hasTemplateContent", hasTemplateContent);
		templateInfo.put("hasPageContent", hasPageContent);
		templateInfo.put("hasStyles", template.getCssStyles() != null && !template.getCssStyles().trim().isEmpty());
		templateInfo.put("appName", template.getApp() != null ? template.getApp().getName() : null);
		templateInfo.put("createdAt", template.getCreatedAt());
		templateInfo.put("updatedAt", template.getUpdatedAt());

		return ResponseEntity.ok(templateInfo);
	}

	@PostMapping("/{id}/preview-pdf-with-engine")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Preview template as PDF using specific engine")
	public ResponseEntity<?> previewPdfWithEngine(@PathVariable Long id, 
			@RequestBody TemplatePreviewRequest request,
			@RequestParam(required = false) String engine,
			HttpServletRequest httpRequest) {

		try {
			// Validate access to template
			User currentUser = getCurrentUserWithCorporate();
			if (currentUser == null || currentUser.getCorporate() == null) {
				return ResponseEntity.badRequest().body("User not associated with any organization");
			}

			Template template = templateRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Template not found"));

			// Check corporate access
			if (!template.getCorporate().getId().equals(currentUser.getCorporate().getId())) {
				return ResponseEntity.status(403).body("Access denied: Template belongs to another organization");
			}

			// Validate template has content
			int pageCount = templateRenderService.getPageCount(id);
			boolean hasTemplateContent = template.getHtmlContent() != null && !template.getHtmlContent().trim().isEmpty();
			boolean hasPageContent = pageCount > 0;
			
			if (!hasTemplateContent && !hasPageContent) {
				return ResponseEntity.badRequest().body("Template has no content to render");
			}

			// Parse engine parameter
			PdfGenerationService.PdfEngine pdfEngine = PdfGenerationService.PdfEngine.fromCode(
				engine != null ? engine : "auto");

			// Generate PDF with specific engine
			byte[] pdf;
			if (pdfEngine == PdfGenerationService.PdfEngine.AUTO) {
				pdf = pdfGenerationService.generatePdf(id, request.getParameters(), 
					request.getPageNumber(), template.getPageOrientation());
			} else {
				pdf = pdfGenerationService.generatePdfWithEngine(pdfEngine, id, request.getParameters(), 
					request.getPageNumber(), template.getPageOrientation());
			}

			if (pdf == null || pdf.length == 0) {
				return ResponseEntity.status(500).body("Failed to generate PDF - empty result");
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			
			// Get page count information
			int totalPages = templateRenderService.getPageCount(id);
			
			// Set filename based on template name and page number
			String filename = template.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
			if (request.getPageNumber() != null) {
				filename += "_page_" + request.getPageNumber();
			}
			filename += "_" + pdfEngine.getCode() + ".pdf";
			
			headers.setContentDispositionFormData("inline", filename);
			headers.add("X-Template-Id", id.toString());
			headers.add("X-Template-Name", template.getName());
			headers.add("X-PDF-Size", String.valueOf(pdf.length));
			headers.add("X-Total-Pages", String.valueOf(totalPages));
			headers.add("X-PDF-Engine", pdfEngine.getDisplayName());
			
			if (request.getPageNumber() != null) {
				headers.add("X-Page-Number", request.getPageNumber().toString());
			} else {
				headers.add("X-Rendered-Pages", "all");
			}

			return ResponseEntity.ok().headers(headers).body(pdf);

		} catch (Exception e) {
			// Log the error for debugging
			log.error("Error generating PDF for template {} with engine {}: {}", id, engine, e.getMessage(), e);
			
			// Return appropriate error response
			if (e.getMessage().contains("not found")) {
				return ResponseEntity.notFound().build();
			} else if (e.getMessage().contains("Access denied")) {
				return ResponseEntity.status(403).body(e.getMessage());
			} else {
				return ResponseEntity.status(500).body("Failed to generate PDF: " + e.getMessage());
			}
		}
	}

	@GetMapping("/pdf-engines/status")
	@RequirePermission(resource = PermissionResource.TEMPLATES, action = PermissionAction.READ)
	@Operation(summary = "Get PDF engines status and availability")
	public ResponseEntity<?> getPdfEnginesStatus() {
		try {
			Map<String, Object> status = pdfGenerationService.getEngineStatus();
			return ResponseEntity.ok(status);
		} catch (Exception e) {
			log.error("Error getting PDF engines status: {}", e.getMessage(), e);
			return ResponseEntity.status(500).body("Failed to get PDF engines status: " + e.getMessage());
		}
	}

	private User getCurrentUserWithCorporate() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
			UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
			return userRepository.findByIdWithCorporate(userPrincipal.getId()).orElse(null);
		}
		return null;
	}
}
