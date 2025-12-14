# PDF Generation Guide

## Overview

The platform supports two PDF generation engines:

1. **Playwright** (Recommended) - High quality, supports modern CSS
2. **Flying Saucer** (Fallback) - Basic quality, limited CSS support

The system automatically uses Playwright if available, and falls back to Flying Saucer if not.

---

## Platform Compatibility

### ✅ Playwright Supported Platforms

| Platform | Architecture | Status |
|----------|-------------|--------|
| Windows | x64, arm64 | ✅ Fully Supported |
| macOS | x64, Apple Silicon (M1/M2) | ✅ Fully Supported |
| Linux | x64, arm64 | ✅ Fully Supported |
| Docker | All | ✅ Supported (with setup) |

### ⚠️ Limited Support

- **Serverless** (AWS Lambda, Azure Functions) - Requires custom layers
- **Shared Hosting** - May lack permissions
- **Alpine Linux** - Requires additional dependencies

---

## Installation

### Local Development (Windows/macOS/Linux)

1. **Install Playwright browsers** (one-time):
   ```bash
   cd backend
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
   ```

2. **Verify installation**:
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="--version"
   ```

3. **Start the application**:
   ```bash
   mvn spring-boot:run
   ```

### Docker Deployment

Update your `Dockerfile`:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Install Playwright dependencies
RUN apt-get update && apt-get install -y \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

# Install Playwright browsers
RUN java -cp app.jar com.microsoft.playwright.CLI install chromium

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Cloud Deployment

#### AWS EC2 / Azure VM / Google Compute Engine
Same as local development - install Playwright browsers after deployment.

#### AWS Lambda / Azure Functions
Use Flying Saucer only (set `pdf.engine=flyingsaucer` in properties).

#### Heroku / Railway / Render
Playwright works with buildpacks. Add to `Procfile`:
```
web: java -jar target/*.jar
release: java -cp target/*.jar com.microsoft.playwright.CLI install chromium
```

---

## Configuration

Edit `application.properties`:

```properties
# PDF Generation Configuration
# Options: playwright, flyingsaucer, auto
pdf.engine=auto
```

### Options:

- **`auto`** (default) - Try Playwright, fallback to Flying Saucer
- **`playwright`** - Use Playwright only (fails if unavailable)
- **`flyingsaucer`** - Use Flying Saucer only (basic quality)

---

## CSS Support Comparison

| Feature | Playwright | Flying Saucer |
|---------|-----------|---------------|
| Flexbox | ✅ Full | ❌ No |
| CSS Grid | ✅ Full | ❌ No |
| Modern Fonts | ✅ Yes | ⚠️ Limited |
| Background Images | ✅ Yes | ⚠️ Limited |
| Gradients | ✅ Yes | ❌ No |
| Shadows | ✅ Yes | ❌ No |
| Transforms | ✅ Yes | ❌ No |
| Media Queries | ✅ Yes | ❌ No |

---

## Troubleshooting

### Playwright Not Working

**Check logs for:**
```
⚠️ Playwright not available: ... - Will use fallback PDF generator
```

**Common issues:**

1. **Browsers not installed**
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
   ```

2. **Missing system dependencies (Linux)**
   ```bash
   sudo apt-get install -y libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2
   ```

3. **Permission issues**
   - Ensure the application has write access to `~/.cache/ms-playwright`

### Force Flying Saucer

If Playwright causes issues, disable it:

```properties
pdf.engine=flyingsaucer
```

---

## Performance

| Engine | Speed | Quality | Memory |
|--------|-------|---------|--------|
| Playwright | Slower (~2-3s) | Excellent | Higher (~200MB) |
| Flying Saucer | Faster (~0.5s) | Basic | Lower (~50MB) |

---

## Recommendations

### Use Playwright When:
- ✅ You need pixel-perfect PDF output
- ✅ Templates use modern CSS (flexbox, grid)
- ✅ You have control over the server environment
- ✅ Quality is more important than speed

### Use Flying Saucer When:
- ✅ Simple templates (tables, basic layout)
- ✅ Serverless/restricted environments
- ✅ Speed is critical
- ✅ Low memory requirements

---

## Support

For issues or questions:
1. Check application logs
2. Verify Playwright installation
3. Test with `pdf.engine=flyingsaucer` to isolate issues
