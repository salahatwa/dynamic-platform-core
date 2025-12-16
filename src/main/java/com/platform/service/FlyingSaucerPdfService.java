package com.platform.service;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.platform.entity.TemplateAsset;
import com.platform.enums.AssetType;
import com.platform.repository.TemplateAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlyingSaucerPdfService {

    private final TemplateAssetRepository templateAssetRepository;

    public boolean isAvailable() {
        return true; // Flying Saucer is always available as it's a Java library
    }

    public byte[] generatePdfFromHtml(String html) {
        return generatePdfFromHtml(html, null, com.platform.enums.PageOrientation.PORTRAIT, null);
    }

    public byte[] generatePdfFromHtml(String html, Integer pageNumber) {
        return generatePdfFromHtml(html, pageNumber, com.platform.enums.PageOrientation.PORTRAIT, null);
    }

    public byte[] generatePdfFromHtml(String html, Integer pageNumber, com.platform.enums.PageOrientation orientation) {
        return generatePdfFromHtml(html, pageNumber, orientation, null);
    }

    public byte[] generatePdfFromHtml(String html, Integer pageNumber, 
                                     com.platform.enums.PageOrientation orientation, Long templateId) {
        try {
            log.info("🔄 Generating PDF with Flying Saucer - Page: {}, Orientation: {}", 
                pageNumber != null ? pageNumber : "all", orientation);

            // Build XHTML document
            String xhtml = buildXhtmlDocument(html, orientation);
            
            // Convert to PDF
            byte[] pdf = convertToPdf(xhtml, templateId);
            
            log.info("✅ Flying Saucer generated PDF successfully, size: {} bytes", pdf.length);
            return pdf;

        } catch (Exception e) {
            log.error("❌ Flying Saucer generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Flying Saucer generation failed: " + e.getMessage(), e);
        }
    }

    private String buildXhtmlDocument(String content, com.platform.enums.PageOrientation orientation) {
        try {
            // Use JSoup to parse and clean HTML
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(content);

            // Configure for XHTML output
            doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);

            // Get body elements and convert to XHTML
            org.jsoup.nodes.Element body = doc.body();

            // Process CSS to make it PDF-friendly with orientation
            String processedCss = processCssForPdf(orientation);

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
            log.debug("Generated XHTML document, length: {} characters", result.length());

            return result;
        } catch (Exception e) {
            log.error("Error building XHTML document", e);
            throw new RuntimeException("Failed to build XHTML document: " + e.getMessage(), e);
        }
    }

    private String processCssForPdf(com.platform.enums.PageOrientation orientation) {
        String pageSize = orientation.isLandscape() ? "A4 landscape" : "A4 portrait";
        
        StringBuilder css = new StringBuilder();
        css.append(String.format("@page { size: %s; margin: 1cm; }\n", pageSize));
        css.append("body { font-family: Arial, sans-serif; font-size: 10pt; line-height: 1.4; margin: 0; padding: 0; }\n");
        
        // Add multi-page support CSS
        css.append("""
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
            
            .no-page-break {
                page-break-inside: avoid;
            }
            
            """);
        
        // Add Flying Saucer specific page break control
        css.append("* { -fs-table-paginate: paginate; }\n");

        return css.toString();
    }

    private byte[] convertToPdf(String xhtml, Long templateId) throws DocumentException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        // Register custom fonts if templateId is provided
        if (templateId != null) {
            List<TemplateAsset> fonts = templateAssetRepository.findByTemplateIdAndAssetType(templateId, AssetType.FONT);

            for (TemplateAsset font : fonts) {
                try {
                    renderer.getFontResolver().addFont(font.getFilePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.debug("Registered font: {}", font.getName());
                } catch (Exception e) {
                    log.warn("Failed to register font: {}", font.getName(), e);
                }
            }
        }

        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }
}