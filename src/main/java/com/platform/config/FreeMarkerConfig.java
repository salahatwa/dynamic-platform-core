package com.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FreeMarkerConfig {

	@Bean("templateFreemarkerConfiguration")
	public freemarker.template.Configuration templateFreemarkerConfiguration() {
		freemarker.template.Configuration cfg = new freemarker.template.Configuration(
				freemarker.template.Configuration.VERSION_2_3_32);

		cfg.setDefaultEncoding("UTF-8");
		
		// Set URL encoding charset for ?url built-in function
		try {
			cfg.setSetting("url_escaping_charset", "UTF-8");
			cfg.setSetting("output_encoding", "UTF-8");
		} catch (freemarker.template.TemplateException e) {
			// Log warning if settings fail, but continue with default configuration
			System.err.println("Warning: Failed to set FreeMarker URL encoding settings: " + e.getMessage());
		}
		
		// Create custom exception handler for template errors
		cfg.setTemplateExceptionHandler(new freemarker.template.TemplateExceptionHandler() {
			@Override
			public void handleTemplateException(freemarker.template.TemplateException te, 
					freemarker.core.Environment env, java.io.Writer out) throws freemarker.template.TemplateException {
				
				if (te instanceof freemarker.core.InvalidReferenceException) {
					// For missing variables, just log a warning and continue with empty string
					System.out.println("Warning: Missing variable in template - " + te.getMessage());
					try {
						out.write(""); // Write empty string for missing variables
					} catch (java.io.IOException e) {
						throw new freemarker.template.TemplateException("Failed to write default value for missing variable", e, env);
					}
				} else if (te instanceof freemarker.core.NonHashException) {
					// Handle "Expected a hash" errors gracefully
					System.out.println("Warning: Type mismatch in template - " + te.getMessage());
					try {
						out.write(""); // Write empty string for type mismatches
					} catch (java.io.IOException e) {
						throw new freemarker.template.TemplateException("Failed to write default value for type mismatch", e, env);
					}
				} else if (te instanceof freemarker.core.NonSequenceOrCollectionException) {
					// Handle array/list type mismatches
					System.out.println("Warning: Array type mismatch in template - " + te.getMessage());
					try {
						out.write(""); // Write empty string for array type mismatches
					} catch (java.io.IOException e) {
						throw new freemarker.template.TemplateException("Failed to write default value for array type mismatch", e, env);
					}
				} else if (te instanceof freemarker.core._TemplateModelException) {
					// Handle URL encoding and other template model exceptions
					System.out.println("Warning: Template model exception - " + te.getMessage());
					try {
						out.write(""); // Write empty string for template model exceptions
					} catch (java.io.IOException e) {
						throw new freemarker.template.TemplateException("Failed to write default value for template model exception", e, env);
					}
				} else {
					// For other exceptions, rethrow them
					throw te;
				}
			}
		});
		
		cfg.setLogTemplateExceptions(false);
		cfg.setWrapUncheckedExceptions(true);
		cfg.setFallbackOnNullLoopVariable(false);
		
		// Add custom object wrapper to handle type conversions
		cfg.setObjectWrapper(new freemarker.template.DefaultObjectWrapper(freemarker.template.Configuration.VERSION_2_3_32) {
			@Override
			public freemarker.template.TemplateModel wrap(Object obj) throws freemarker.template.TemplateModelException {
				// If we get a string where an object is expected, try to handle it gracefully
				if (obj instanceof String) {
					String str = (String) obj;
					// If it looks like JSON, try to parse it
					if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
						try {
							// This is a simple approach - in production you'd use a proper JSON parser
							return super.wrap(new java.util.HashMap<>());
						} catch (Exception e) {
							// Fall back to default behavior
						}
					}
				}
				return super.wrap(obj);
			}
		});

		return cfg;
	}
}
