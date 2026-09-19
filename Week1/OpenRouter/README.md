# OpenRouter API Client

A Java command-line tool to query OpenRouter API key information and account details.

## Prerequisites

- Java 21 or later
- Maven 3.9 or later

## Setup

### 1. Configure API Key

Create a `.env` file in the project root with your OpenRouter API key:

```bash
echo "OPENROUTER_API_KEY=sk-or-v1-your-key-here" > .env
```

Replace `sk-or-v1-your-key-here` with your actual OpenRouter API key.

## Build

```bash
mvn clean package
```

This generates `target/openrouter-client-1.0-SNAPSHOT.jar`

## Run

### Display Formatted Report

```bash
java -jar target/openrouter-client-1.0-SNAPSHOT.jar
```

**Output:**
```
Info: Only management keys can fetch credits for an account
Key identity
  Label:                    sk-or-v1-e3b...6b5
  Is management key:       false
  Is provisioning key:      false

Limits and balance
  Limit:                    $40.0000
  Limit remaining:          $39.8439
  Limit reset:              —

Usage
  Total:                    $0.1561
  Daily:                    $0.1561
  Weekly:                   $0.1561
  Monthly:                  $0.1561
  BYOK total:               $0.0000
  BYOK daily:               $0.0000
  BYOK weekly:              $0.0000
  BYOK monthly:             $0.0000

Free-tier allowance
  Used:                     $53.0000
  Limit:                    $1000.0000
  Remaining:                $947.0000

Account credits
  Balance:                  (requires management key)
```

### Display Raw JSON

```bash
java -jar target/openrouter-client-1.0-SNAPSHOT.jar --json
```

**Output:**
```json
{"label":"sk-or-v1-e3b...6b5","is_management_key":false,"is_provisioning_key":false,"limit":40,"limit_remaining":39.7955815,...}
```

## Quick Commands

```bash
# Full workflow: build and run
mvn clean package && java -jar target/openrouter-client-1.0-SNAPSHOT.jar

# Build only
mvn clean package

# Run formatted report
java -jar target/openrouter-client-1.0-SNAPSHOT.jar

# Run raw JSON output
java -jar target/openrouter-client-1.0-SNAPSHOT.jar --json

# Compile without packaging
mvn compile

# Clean build artifacts
mvn clean
```

## Exit Codes

- `0` - Success
- `1` - Configuration error (missing or invalid .env)
- `2` - API failure (invalid key, rate limit, server error)

## Error Messages

### Configuration Errors (Exit 1)
```
Error: OPENROUTER_API_KEY not configured
```

### API Errors (Exit 2)
```
Error: Invalid API key                    # HTTP 401
Error: Access denied                      # HTTP 403
Error: Rate limit exceeded                # HTTP 429
Error: Server error (HTTP 5xx)            # HTTP 5xx
Error: Connection failed - <reason>       # Network issues
```

### Expected Messages (Exit 0)
```
Info: Only management keys can fetch credits for an account
```
This is expected when using a regular (non-management) API key. The account credits balance requires a management key.

## Features

- **Formatted Report**: Human-readable sections with aligned text
- **Raw JSON Output**: Machine-readable output with `--json` flag
- **Secure**: API key never printed in full (masked by OpenRouter API)
- **15-second timeout**: HTTP requests timeout after 15 seconds
- **Proper exit codes**: Different codes for config vs API errors

## Report Sections

1. **Key identity** - Label, key type (management/provisioning)
2. **Limits and balance** - Usage limits and remaining balance
3. **Usage** - Spending breakdown (daily, weekly, monthly, BYOK variants)
4. **Free-tier allowance** - Free model request quotas
5. **Account credits** - Account-wide balance (requires management key)

## Money Formatting

All monetary values are displayed as USD to 4 decimal places (e.g., `$0.1561`).
Null/missing values are shown as em dash (`—`).

## API Endpoints Used

- `GET https://openrouter.ai/api/v1/auth/key` - Key information
- `GET https://openrouter.ai/api/v1/credits` - Account credits (requires management key)

Both endpoints use:
- Header: `Authorization: Bearer <api-key>`
- Timeout: 15 seconds
