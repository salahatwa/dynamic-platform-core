# Word Generation CSS Testing Guide

## Quick Test Steps

### 1. Create a Test Template

In the Template Editor, create a new template with the following:

**HTML Content:**
```html
<h1>Test Document</h1>
<p>This is a <span class="highlight">highlighted</span> paragraph.</p>
<p class="important">This is important text.</p>
```

**CSS Styles:**
```css
h1 {
  color: #2C3E50;
  font-size: 24pt;
  text-align: center;
}

p {
  font-size: 12pt;
  color: #333333;
}

.highlight {
  background-color: yellow;
  font-weight: bold;
}

.important {
  color: red;
  font-weight: bold;
  font-size: 14pt;
}
```

### 2. Generate Word Document

1. Click "Preview Word" button
2. Word document will download automatically
3. Open the .docx file in Microsoft Word

### 3. Verify CSS Styles

Check that the following styles are applied:

- ✓ H1 is blue (#2C3E50), 24pt, centered
- ✓ Paragraphs are 12pt, dark gray (#333333)
- ✓ "highlighted" text is bold (background color may not show)
- ✓ "important" text is red, bold, 14pt

## What to Look For

### ✅ Should Work
- Font colors (hex, rgb, named)
- Font sizes (pt, px, em, rem)
- Font families
- Bold, italic, underline
- Text alignment (left, center, right, justify)
- Heading styles
- Table cell colors
- Margins and spacing

### ⚠️ Limitations
- Background colors on text (Word has limited highlight support)
- Flexbox/Grid layouts (use tables instead)
- Borders (limited support)
- Gradients (not supported)
- Shadows (not supported)

## Debugging

### Check Backend Logs

When you generate a Word document, look for these console messages:

```
=== Starting Word Generation ===
HTML length: 1234 characters
Parsed 5 CSS rules
Total CSS rules loaded: 5
Sample rules: [h1, p, .highlight, .important]
Processing body with 3 child nodes
Word document generated: 12345 bytes
=== Word Generation Complete ===
```

### Common Issues

**Issue: No styles applied**
- Check that CSS is in `<style>` tags
- Verify CSS syntax (no errors)
- Ensure class names match exactly (case-sensitive)

**Issue: Colors not showing**
- Use hex format: `#FF0000` instead of `red`
- Check color format is valid
- Try rgb format: `rgb(255, 0, 0)`

**Issue: Font sizes wrong**
- Use `pt` units for best results
- Check conversion: 16px = 12pt
- Avoid very small or very large sizes

**Issue: Styles partially applied**
- Check CSS specificity (inline > class > tag)
- Verify selector syntax
- Look for CSS syntax errors

## Test Template

Use the provided `TEST_TEMPLATE.html` file to verify all CSS features:

1. Copy content from `backend/TEST_TEMPLATE.html`
2. Split into HTML and CSS sections
3. Paste into Template Editor
4. Generate Word document
5. Verify all styles are applied

## Expected Results

The test template should produce a Word document with:

- Centered blue heading (24pt)
- Blue subheadings (18pt)
- Justified paragraphs
- Highlighted text (bold, may not show yellow background)
- Red important text (bold, 14pt)
- Colored status messages (green, orange, red)
- Styled table with blue header
- Formatted lists

## API Testing

### Using cURL

```bash
# Get template
curl -X GET http://localhost:8080/api/template-editor/1 \
  -H "Authorization: Bearer YOUR_TOKEN"

# Generate Word
curl -X POST http://localhost:8080/api/template-editor/1/preview-word \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"parameters": {}}' \
  --output test.docx
```

### Using Postman

1. POST to `/api/template-editor/{id}/preview-word`
2. Headers: `Authorization: Bearer YOUR_TOKEN`
3. Body (JSON): `{"parameters": {}}`
4. Send as Binary
5. Save response as .docx file

## Troubleshooting Checklist

- [ ] Backend is running (port 8080)
- [ ] Template has both HTML and CSS
- [ ] CSS is in `<style>` tags
- [ ] No CSS syntax errors
- [ ] Class names match exactly
- [ ] Using supported CSS properties
- [ ] Check backend console logs
- [ ] Try test template first
- [ ] Verify Word document opens
- [ ] Check styles in Word

## Support

If styles still don't work:

1. Check backend logs for errors
2. Verify CSS rules are being parsed (count should be > 0)
3. Try simpler CSS first (just colors and sizes)
4. Test with TEST_TEMPLATE.html
5. Ensure Word document opens without errors
6. Check that you're using supported CSS properties

## Next Steps

Once basic CSS works:

1. Test with your actual templates
2. Add more complex styles gradually
3. Use tables for layout if needed
4. Test with different fonts
5. Verify colors in Word
6. Check spacing and margins
7. Test with parameters ({{placeholder}})
