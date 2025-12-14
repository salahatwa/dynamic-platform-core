package com.platform.service;

import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WordGenerationService {
    
    private Map<String, Map<String, String>> cssRules = new HashMap<>();
    
    public byte[] convertHtmlToWord(String html) throws IOException {
        System.out.println("=== Starting Word Generation ===");
        System.out.println("HTML length: " + html.length() + " characters");
        
        // Parse HTML
        Document doc = Jsoup.parse(html);
        
        // Extract and parse CSS
        parseCssFromDocument(doc);
        
        // Create Word document
        XWPFDocument document = new XWPFDocument();
        
        // Process body content
        Element body = doc.body();
        if (body != null) {
            System.out.println("Processing body with " + body.childNodeSize() + " child nodes");
            processElement(body, document, null);
        }
        
        // Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();
        
        System.out.println("Word document generated: " + out.size() + " bytes");
        System.out.println("=== Word Generation Complete ===");
        
        return out.toByteArray();
    }
    
    private void parseCssFromDocument(Document doc) {
        cssRules.clear();
        
        // Extract CSS from <style> tags
        Elements styleTags = doc.select("style");
        for (Element styleTag : styleTags) {
            String css = styleTag.html();
            
            // Remove CSS comments
            css = css.replaceAll("/\\*.*?\\*/", "");
            
            // Remove media queries (not supported in Word) - handle nested braces
            css = removeMediaQueries(css);
            
            // Remove @keyframes, @font-face, etc.
            css = css.replaceAll("@[a-z-]+[^{]*\\{[^}]*\\}", "");
            
            parseCss(css);
        }
        
        System.out.println("Total CSS rules loaded: " + cssRules.size());
        if (!cssRules.isEmpty()) {
            System.out.println("Sample rules: " + cssRules.keySet().stream().limit(5).toList());
        }
    }
    
    private String removeMediaQueries(String css) {
        // Remove @media blocks with proper brace matching
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < css.length()) {
            if (css.startsWith("@media", i)) {
                // Find the opening brace
                int openBrace = css.indexOf('{', i);
                if (openBrace == -1) break;
                
                // Count braces to find matching closing brace
                int braceCount = 1;
                int j = openBrace + 1;
                while (j < css.length() && braceCount > 0) {
                    if (css.charAt(j) == '{') braceCount++;
                    else if (css.charAt(j) == '}') braceCount--;
                    j++;
                }
                
                // Skip the entire @media block
                i = j;
            } else {
                result.append(css.charAt(i));
                i++;
            }
        }
        return result.toString();
    }
    
    private void parseCss(String css) {
        // Simple CSS parser - extracts selector and properties
        Pattern pattern = Pattern.compile("([^{]+)\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(css);
        
        int ruleCount = 0;
        while (matcher.find()) {
            String selector = matcher.group(1).trim();
            String properties = matcher.group(2).trim();
            
            Map<String, String> props = new HashMap<>();
            String[] propArray = properties.split(";");
            for (String prop : propArray) {
                String[] parts = prop.split(":", 2);
                if (parts.length == 2) {
                    props.put(parts[0].trim(), parts[1].trim());
                }
            }
            
            if (!props.isEmpty()) {
                cssRules.put(selector, props);
                ruleCount++;
            }
        }
        
        System.out.println("Parsed " + ruleCount + " CSS rules");
    }
    
    private Map<String, String> getComputedStyles(Element element) {
        Map<String, String> styles = new HashMap<>();
        
        // Priority order (lowest to highest): tag styles < class styles < inline styles
        
        // 1. Get tag styles (lowest priority)
        String tagName = element.tagName();
        Map<String, String> tagStyles = cssRules.get(tagName);
        if (tagStyles != null) {
            styles.putAll(tagStyles);
        }
        
        // 2. Get CSS class styles (medium priority)
        String className = element.attr("class");
        if (!className.isEmpty()) {
            // Handle multiple classes
            String[] classes = className.split("\\s+");
            for (String cls : classes) {
                Map<String, String> classStyles = cssRules.get("." + cls.trim());
                if (classStyles != null) {
                    styles.putAll(classStyles); // Override tag styles
                }
            }
        }
        
        // 3. Get inline styles (highest priority)
        String inlineStyle = element.attr("style");
        if (!inlineStyle.isEmpty()) {
            String[] props = inlineStyle.split(";");
            for (String prop : props) {
                String[] parts = prop.split(":", 2);
                if (parts.length == 2) {
                    styles.put(parts[0].trim(), parts[1].trim()); // Override everything
                }
            }
        }
        
        return styles;
    }
    
    private void processElement(Element element, XWPFDocument document, XWPFParagraph currentParagraph) {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.text().trim();
                if (!text.isEmpty()) {
                    if (currentParagraph == null) {
                        currentParagraph = document.createParagraph();
                    }
                    XWPFRun run = currentParagraph.createRun();
                    run.setText(text);
                    applyInlineStyles(run, element);
                }
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                String tagName = childElement.tagName().toLowerCase();
                
                switch (tagName) {
                    case "h1":
                        createHeading(document, childElement, 1);
                        break;
                    case "h2":
                        createHeading(document, childElement, 2);
                        break;
                    case "h3":
                        createHeading(document, childElement, 3);
                        break;
                    case "h4":
                        createHeading(document, childElement, 4);
                        break;
                    case "h5":
                        createHeading(document, childElement, 5);
                        break;
                    case "h6":
                        createHeading(document, childElement, 6);
                        break;
                    case "p":
                        createParagraph(document, childElement);
                        break;
                    case "ul":
                    case "ol":
                        createList(document, childElement, tagName.equals("ol"));
                        break;
                    case "table":
                        createTable(document, childElement);
                        break;
                    case "br":
                        if (currentParagraph != null) {
                            currentParagraph.createRun().addBreak();
                        }
                        break;
                    case "strong":
                    case "b":
                        if (currentParagraph == null) {
                            currentParagraph = document.createParagraph();
                        }
                        XWPFRun boldRun = currentParagraph.createRun();
                        boldRun.setText(childElement.text());
                        boldRun.setBold(true);
                        break;
                    case "em":
                    case "i":
                        if (currentParagraph == null) {
                            currentParagraph = document.createParagraph();
                        }
                        XWPFRun italicRun = currentParagraph.createRun();
                        italicRun.setText(childElement.text());
                        italicRun.setItalic(true);
                        break;
                    case "u":
                        if (currentParagraph == null) {
                            currentParagraph = document.createParagraph();
                        }
                        XWPFRun underlineRun = currentParagraph.createRun();
                        underlineRun.setText(childElement.text());
                        underlineRun.setUnderline(UnderlinePatterns.SINGLE);
                        break;
                    case "div":
                    case "section":
                    case "article":
                    case "header":
                    case "footer":
                    case "main":
                    case "aside":
                        // Check if div has direct text content or only block elements
                        boolean hasDirectText = false;
                        for (Node child : childElement.childNodes()) {
                            if (child instanceof TextNode && !((TextNode) child).text().trim().isEmpty()) {
                                hasDirectText = true;
                                break;
                            }
                        }
                        
                        if (hasDirectText) {
                            // Treat as paragraph
                            XWPFParagraph divPara = document.createParagraph();
                            Map<String, String> divStyles = getComputedStyles(childElement);
                            applyParagraphStyles(divPara, divStyles);
                            processInlineElements(childElement, divPara);
                        } else {
                            // Process as container
                            processElement(childElement, document, null);
                        }
                        break;
                    case "span":
                        if (currentParagraph == null) {
                            currentParagraph = document.createParagraph();
                        }
                        XWPFRun spanRun = currentParagraph.createRun();
                        spanRun.setText(childElement.text());
                        applyInlineStyles(spanRun, childElement);
                        break;
                    default:
                        // For unknown tags, just process their content
                        processElement(childElement, document, currentParagraph);
                        break;
                }
            }
        }
    }
    
    private void createHeading(XWPFDocument document, Element element, int level) {
        XWPFParagraph paragraph = document.createParagraph();
        
        // Get computed styles
        Map<String, String> styles = getComputedStyles(element);
        
        // Apply paragraph-level styles
        applyParagraphStyles(paragraph, styles);
        
        // Create run with text
        XWPFRun run = paragraph.createRun();
        run.setText(element.text());
        
        // Apply text styles from CSS
        applyStylesToRun(run, styles, element);
        
        // Default bold for headings if not specified in CSS
        if (!styles.containsKey("font-weight")) {
            run.setBold(true);
        }
        
        // Set font size based on heading level if not specified in CSS
        if (!styles.containsKey("font-size")) {
            int fontSize = 24 - (level * 2);
            run.setFontSize(fontSize);
        }
    }
    
    private void createParagraph(XWPFDocument document, Element element) {
        XWPFParagraph paragraph = document.createParagraph();
        
        // Apply paragraph-level styles
        Map<String, String> styles = getComputedStyles(element);
        applyParagraphStyles(paragraph, styles);
        
        processInlineElements(element, paragraph);
    }
    
    private void processInlineElements(Element element, XWPFParagraph paragraph) {
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.text();
                if (!text.trim().isEmpty()) {
                    XWPFRun run = paragraph.createRun();
                    run.setText(text);
                    // Apply parent element styles to text
                    Map<String, String> styles = getComputedStyles(element);
                    applyStylesToRun(run, styles, element);
                }
            } else if (node instanceof Element) {
                Element childElement = (Element) node;
                String tagName = childElement.tagName().toLowerCase();
                
                if (tagName.equals("br")) {
                    paragraph.createRun().addBreak();
                    continue;
                }
                
                XWPFRun run = paragraph.createRun();
                run.setText(childElement.text());
                
                // Apply computed styles from CSS
                Map<String, String> styles = getComputedStyles(childElement);
                applyStylesToRun(run, styles, childElement);
                
                // Apply semantic HTML styles (these override CSS if not specified)
                switch (tagName) {
                    case "strong":
                    case "b":
                        if (!styles.containsKey("font-weight")) {
                            run.setBold(true);
                        }
                        break;
                    case "em":
                    case "i":
                        if (!styles.containsKey("font-style")) {
                            run.setItalic(true);
                        }
                        break;
                    case "u":
                        if (!styles.containsKey("text-decoration")) {
                            run.setUnderline(UnderlinePatterns.SINGLE);
                        }
                        break;
                }
            }
        }
    }
    
    private void createList(XWPFDocument document, Element element, boolean ordered) {
        Elements items = element.select("> li");
        for (int i = 0; i < items.size(); i++) {
            Element item = items.get(i);
            XWPFParagraph paragraph = document.createParagraph();
            
            // Set numbering
            if (ordered) {
                paragraph.setNumID(java.math.BigInteger.valueOf(1));
            } else {
                paragraph.setNumID(java.math.BigInteger.valueOf(2));
            }
            
            // Get computed styles for list item
            Map<String, String> styles = getComputedStyles(item);
            applyParagraphStyles(paragraph, styles);
            
            XWPFRun run = paragraph.createRun();
            run.setText(item.text());
            
            // Apply text styles
            applyStylesToRun(run, styles, item);
        }
    }
    
    private void createTable(XWPFDocument document, Element element) {
        Elements rows = element.select("tr");
        if (rows.isEmpty()) return;
        
        // Count columns
        int cols = rows.first().select("th, td").size();
        
        XWPFTable table = document.createTable(rows.size(), cols);
        
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cells = row.select("th, td");
            XWPFTableRow tableRow = table.getRow(i);
            
            for (int j = 0; j < cells.size() && j < cols; j++) {
                Element cell = cells.get(j);
                XWPFTableCell tableCell = tableRow.getCell(j);
                
                // Clear default paragraph
                tableCell.removeParagraph(0);
                XWPFParagraph cellPara = tableCell.addParagraph();
                
                // Get computed styles for cell
                Map<String, String> cellStyles = getComputedStyles(cell);
                applyParagraphStyles(cellPara, cellStyles);
                
                XWPFRun cellRun = cellPara.createRun();
                cellRun.setText(cell.text());
                
                // Apply text styles
                applyStylesToRun(cellRun, cellStyles, cell);
                
                // Bold header cells if not specified in CSS
                if (cell.tagName().equals("th") && !cellStyles.containsKey("font-weight")) {
                    cellRun.setBold(true);
                }
                
                // Apply background color to cell if specified
                String bgColor = cellStyles.get("background-color");
                if (bgColor != null) {
                    String hexColor = parseColor(bgColor);
                    if (hexColor != null) {
                        tableCell.setColor(hexColor);
                    }
                }
            }
        }
    }
    
    private void applyInlineStyles(XWPFRun run, Element element) {
        Map<String, String> styles = getComputedStyles(element);
        applyStylesToRun(run, styles, element);
    }
    
    private void applyStylesToRun(XWPFRun run, Map<String, String> styles, Element element) {
        // Font weight
        String fontWeight = styles.get("font-weight");
        if (fontWeight != null && (fontWeight.equals("bold") || fontWeight.equals("700") || 
            fontWeight.equals("800") || fontWeight.equals("900") || fontWeight.equals("bolder"))) {
            run.setBold(true);
        }
        
        // Font style
        String fontStyle = styles.get("font-style");
        if (fontStyle != null && fontStyle.equals("italic")) {
            run.setItalic(true);
        }
        
        // Text decoration
        String textDecoration = styles.get("text-decoration");
        if (textDecoration != null && textDecoration.contains("underline")) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        
        // Color
        String color = styles.get("color");
        if (color != null) {
            String hexColor = parseColor(color);
            if (hexColor != null) {
                run.setColor(hexColor);
            }
        }
        
        // Background color (text highlight)
        String backgroundColor = styles.get("background-color");
        if (backgroundColor != null) {
            String hexColor = parseColor(backgroundColor);
            if (hexColor != null) {
                // POI doesn't support arbitrary highlight colors, so we set the color
                // Note: This is a limitation of Word format
                try {
                    run.getCTR().addNewRPr().addNewHighlight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STHighlightColor.YELLOW);
                } catch (Exception e) {
                    // Ignore if highlight fails
                }
            }
        }
        
        // Font size
        String fontSize = styles.get("font-size");
        if (fontSize != null) {
            int size = parseFontSize(fontSize);
            if (size > 0) {
                run.setFontSize(size);
            }
        }
        
        // Font family
        String fontFamily = styles.get("font-family");
        if (fontFamily != null) {
            // Remove quotes and get first font
            fontFamily = fontFamily.replaceAll("['\"]", "").split(",")[0].trim();
            run.setFontFamily(fontFamily);
        }
    }
    
    private void applyParagraphStyles(XWPFParagraph paragraph, Map<String, String> styles) {
        // Text alignment
        String textAlign = styles.get("text-align");
        if (textAlign != null) {
            switch (textAlign.toLowerCase()) {
                case "center":
                    paragraph.setAlignment(ParagraphAlignment.CENTER);
                    break;
                case "right":
                    paragraph.setAlignment(ParagraphAlignment.RIGHT);
                    break;
                case "justify":
                    paragraph.setAlignment(ParagraphAlignment.BOTH);
                    break;
                default:
                    paragraph.setAlignment(ParagraphAlignment.LEFT);
                    break;
            }
        }
        
        // Margins (spacing)
        String marginTop = styles.get("margin-top");
        if (marginTop != null) {
            int spacing = parseSpacing(marginTop);
            if (spacing > 0) {
                paragraph.setSpacingBefore(spacing);
            }
        }
        
        String marginBottom = styles.get("margin-bottom");
        if (marginBottom != null) {
            int spacing = parseSpacing(marginBottom);
            if (spacing > 0) {
                paragraph.setSpacingAfter(spacing);
            }
        }
        
        // Line height
        String lineHeight = styles.get("line-height");
        if (lineHeight != null) {
            try {
                if (lineHeight.endsWith("%")) {
                    int percent = Integer.parseInt(lineHeight.replace("%", "").trim());
                    paragraph.setSpacingBetween(percent / 100.0);
                } else {
                    double multiplier = Double.parseDouble(lineHeight);
                    paragraph.setSpacingBetween(multiplier);
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
    }
    
    private int parseSpacing(String spacing) {
        try {
            String numericPart = spacing.replaceAll("[^0-9.]", "");
            double size = Double.parseDouble(numericPart);
            
            // Convert to twips (1/20 of a point)
            if (spacing.contains("px")) {
                size = size * 15; // px to twips
            } else if (spacing.contains("pt")) {
                size = size * 20; // pt to twips
            } else if (spacing.contains("em")) {
                size = size * 240; // em to twips (assuming 12pt base)
            }
            
            return (int) Math.round(size);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String parseColor(String color) {
        if (color == null) return null;
        
        // Handle hex colors
        if (color.startsWith("#")) {
            return color.substring(1).toUpperCase();
        }
        
        // Handle rgb/rgba colors
        if (color.startsWith("rgb")) {
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(color);
            int[] rgb = new int[3];
            int i = 0;
            while (matcher.find() && i < 3) {
                rgb[i++] = Integer.parseInt(matcher.group());
            }
            return String.format("%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
        }
        
        // Handle named colors
        return convertNamedColor(color);
    }
    
    private String convertNamedColor(String colorName) {
        Map<String, String> namedColors = new HashMap<>();
        namedColors.put("black", "000000");
        namedColors.put("white", "FFFFFF");
        namedColors.put("red", "FF0000");
        namedColors.put("green", "008000");
        namedColors.put("blue", "0000FF");
        namedColors.put("yellow", "FFFF00");
        namedColors.put("orange", "FFA500");
        namedColors.put("purple", "800080");
        namedColors.put("pink", "FFC0CB");
        namedColors.put("gray", "808080");
        namedColors.put("grey", "808080");
        namedColors.put("silver", "C0C0C0");
        namedColors.put("maroon", "800000");
        namedColors.put("olive", "808000");
        namedColors.put("lime", "00FF00");
        namedColors.put("aqua", "00FFFF");
        namedColors.put("teal", "008080");
        namedColors.put("navy", "000080");
        namedColors.put("fuchsia", "FF00FF");
        namedColors.put("brown", "A52A2A");
        namedColors.put("gold", "FFD700");
        namedColors.put("indigo", "4B0082");
        namedColors.put("violet", "EE82EE");
        namedColors.put("cyan", "00FFFF");
        namedColors.put("magenta", "FF00FF");
        
        return namedColors.get(colorName.toLowerCase());
    }
    
    private int parseFontSize(String fontSize) {
        try {
            // Remove units and parse
            String numericPart = fontSize.replaceAll("[^0-9.]", "");
            double size = Double.parseDouble(numericPart);
            
            // Convert to points if needed
            if (fontSize.contains("px")) {
                size = size * 0.75; // px to pt conversion
            } else if (fontSize.contains("em")) {
                size = size * 12; // em to pt (assuming 12pt base)
            } else if (fontSize.contains("rem")) {
                size = size * 12; // rem to pt
            }
            
            return (int) Math.round(size);
        } catch (Exception e) {
            return 0;
        }
    }
    

}
