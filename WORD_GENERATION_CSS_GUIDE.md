# Word Document Generation with CSS Support

## Overview

The Word generation feature converts HTML templates with CSS styling into Microsoft Word (.docx) documents with high accuracy. The system parses CSS from `<style>` tags and applies styles to the generated Word document.

## Supported CSS Properties

### Text Styling

| CSS Property | Word Equivalent | Supported Values |
|--------------|----------------|------------------|
| `font-weight` | Bold | `bold`, `700`, `800`, `900`, `bolder` |
| `font-style` | Italic | `italic` |
| `text-decoration` | Underline | `underline` |
| `color` | Text Color | Hex (#FF0000), RGB (rgb(255,0,0)), Named colors |
| `font-size` | Font Size | `px`, `pt`, `em`, `rem` |
| `font-family` | Font Family | Any font name |

### Paragraph Styling

| CSS Property | Word Equivalent | Supported Values |
|--------------|----------------|------------------|
| `text-align` | Alignment | `left`, `center`, `right`, `justify` |
| `margin-top` | Spacing Before | `px`, `pt`, `em` |
| `margin-bottom` | Spacing After | `px`, `pt`, `em` |
| `line-height` | Line Spacing | Numeric multiplier or percentage |

### Table Styling

| CSS Property | Word Equivalent | Supported Values |
|--------------|----------------|------------------|
| `background-color` | Cell Background | Hex, RGB, Named colors |
| All text properties | Cell Text | Same as text styling |

## Color Support

### Hex Colors
```css
color: #FF0000;
background-color: #00FF00;
```

### RGB Colors
```css
color: rgb(255, 0, 0);
background-color: rgba(0, 255, 0, 0.5);
```

### Named Colors
Supported: black, white, red, green, blue, yellow, orange, purple, pink, gray, grey, silver, maroon, olive, lime, aqua, teal, navy, fuchsia, brown, gold, indigo, violet, cyan, magenta

```css
color: red;
background-color: lightblue;
```

## Font Size Conversion

The system automatically converts CSS units to Word points:

- **px to pt**: `16px` → `12pt` (multiply by 0.75)
- **em to pt**: `1.5em` → `18pt` (multiply by 12)
- **rem to pt**: `1.5rem` → `18pt` (multiply by 12)
- **pt**: Used directly

## CSS Specificity

Styles are applied in order of specificity (lowest to highest):

1. **Tag selectors** (e.g., `p { color: blue; }`)
2. **Class selectors** (e.g., `.highlight { color: red; }`)
3. **Inline styles** (e.g., `<p style="color: green;">`)

Higher specificity styles override lower ones.

## Example Template

```html
<!DOCTYPE html>
<html>
<head>
<style>
  body {
    font-family: Arial, sans-serif;
    font-size: 12pt;
    line-height: 1.5;
  }
  
  h1 {
    color: #2C3E50;
    font-size: 24pt;
    font-weight: bold;
    text-align: center;
    margin-bottom: 20px;
  }
  
  h2 {
    color: #34495E;
    font-size: 18pt;
    font-weight: bold;
    margin-top: 15px;
    margin-bottom: 10px;
  }
  
  p {
    color: #333333;
    text-align: justify;
    margin-bottom: 10px;
  }
  
  .highlight {
    background-color: #FFFF00;
    font-weight: bold;
  }
  
  .important {
    color: #E74C3C;
    font-weight: bold;
  }
  
  table {
    width: 100%;
    border-collapse: collapse;
  }
  
  th {
    background-color: #3498DB;
    color: white;
    font-weight: bold;
    text-align: left;
    padding: 10px;
  }
  
  td {
    padding: 8px;
    text-align: left;
  }
  
  .center {
    text-align: center;
  }
</style>
</head>
<body>
  <h1>Professional Report</h1>
  
  <h2>Executive Summary</h2>
  <p>This is a <span class="highlight">highlighted text</span> with proper styling.</p>
  <p class="important">This is an important notice in red.</p>
  
  <h2>Data Table</h2>
  <table>
    <tr>
      <th>Name</th>
      <th>Value</th>
      <th class="center">Status</th>
    </tr>
    <tr>
      <td>Item 1</td>
      <td>$100</td>
      <td class="center">Active</td>
    </tr>
  </table>
</body>
</html>
```

## HTML Elements Support

### Block Elements
- `<h1>` to `<h6>` - Headings with automatic sizing
- `<p>` - Paragraphs
- `<div>`, `<section>`, `<article>`, `<header>`, `<footer>`, `<main>`, `<aside>` - Containers
- `<ul>`, `<ol>`, `<li>` - Lists
- `<table>`, `<tr>`, `<th>`, `<td>` - Tables

### Inline Elements
- `<strong>`, `<b>` - Bold text
- `<em>`, `<i>` - Italic text
- `<u>` - Underlined text
- `<span>` - Inline container with styling
- `<br>` - Line break

## API Endpoint

### Generate Word Preview

```http
GET /api/template-editor/{id}/preview-word
Authorization: Bearer {token}
```

**Response**: Binary Word document (.docx)

**Example**:
```javascript
// Frontend code
previewWord(templateId: number) {
  const url = `${environment.apiUrl}/api/template-editor/${templateId}/preview-word`;
  window.open(url, '_blank');
}
```

## Limitations

1. **Background Colors**: Word has limited support for text highlighting. Background colors on text may not render exactly as in HTML.

2. **Complex Layouts**: Flexbox, Grid, and absolute positioning are not supported. Use tables for complex layouts.

3. **Borders**: Border styling is limited. Use table borders for best results.

4. **Gradients**: Not supported. Use solid colors instead.

5. **Shadows**: Text and box shadows are not supported.

6. **Animations**: CSS animations and transitions are ignored.

## Best Practices

1. **Use Standard Fonts**: Stick to common fonts (Arial, Times New Roman, Calibri) for best compatibility.

2. **Simple Layouts**: Keep layouts simple with headings, paragraphs, lists, and tables.

3. **Test Colors**: Test color rendering in Word as some colors may appear differently.

4. **Use Points for Font Sizes**: Use `pt` units for most accurate font sizing.

5. **Avoid Complex Selectors**: Use simple class and tag selectors. Pseudo-classes and complex selectors are not supported.

6. **Table Layouts**: Use tables for structured data and complex layouts.

## Troubleshooting

### Colors Not Appearing
- Ensure colors are in supported formats (hex, rgb, or named)
- Check that color values are valid
- Verify CSS syntax is correct

### Font Sizes Wrong
- Use `pt` units for most accurate results
- Check unit conversion (px × 0.75 = pt)
- Verify font-size property is spelled correctly

### Styles Not Applied
- Check CSS specificity order
- Verify class names match exactly (case-sensitive)
- Ensure CSS is in `<style>` tags, not external files
- Check for CSS syntax errors

### Table Issues
- Use proper table structure (`<table>`, `<tr>`, `<th>`, `<td>`)
- Apply styles to individual cells for best results
- Use background-color on `<th>` and `<td>` elements

## Performance

- **Generation Time**: ~1-2 seconds per document
- **Memory Usage**: ~50MB per generation
- **Concurrent Requests**: Unlimited (thread-safe)
- **File Size**: Typically 20-100KB for text documents

## Audit Logging

All Word generation actions are logged with:
- Action: `GENERATE_WORD`
- Entity Type: `TEMPLATE`
- Entity ID: Template ID
- User: Authenticated user
- Timestamp: Generation time
- IP Address: Client IP

View audit logs in the Dashboard or Templates page.
