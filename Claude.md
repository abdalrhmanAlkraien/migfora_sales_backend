# CLAUDE.md — MIGFORA Sales Intelligence Platform

This file gives Claude full context about this codebase so it can assist effectively without re-explaining the project each session.

---

## What This Project Is

**MIGFORA Sales Intelligence Platform** — an internal tool for the MIGFORA sales team targeting GCC/MENA cloud and software clients. It combines:

1. A **CRM** — companies, contacts, follow-ups, pipelines
2. A **Recon Engine** — automated technical investigation of prospect domains (DNS, headers, SSL, tech stack, subdomains, CDN bypass, etc.)
3. An **AI Report Generator** — LLM-powered sales reports (technical overview + sales roadmap) exported as PDFs to S3

---

## Stack

- **Java 25**, **Spring Boot 4.0.6**
- **PostgreSQL 16** on `localhost:5435/sales_platform`
- **AWS Cognito** `eu-central-1_SrsV54zGw` — auth, groups: `admin_group`, `sales`
- **AWS SES** — daily follow-up reminder emails
- **AWS S3** — PDF report storage
- **LangChain4j 1.x** + **Qubrid** (OpenAI-compatible) — AI report generation
- **iText html2pdf** — markdown → PDF
- **Jsoup** — HTML scraping for tech stack detection
- **ProcessBuilder + curl/dig** — recon tasks that need raw network access

---

## Critical Jackson Note

Spring Boot 4.x uses **Jackson 3.x** under `tools.jackson.*` — NOT `com.fasterxml.jackson.*`.

```java
// CORRECT
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
// node.properties().forEach(...)   ← Jackson 3.x (not .fields())

// WRONG — will cause type conflicts
import com.fasterxml.jackson.databind.ObjectMapper;
```

---

## Package Structure

Base package: `com.migfora.sales`

```
config/       AiConfig, LangChain4jConfig, SecurityConfig
controller/   AuthController, UserController, CompanyController,
              ContactController, FollowUpController,
              InvestigationController, ReportController,
              DashboardController, AdminController
service/      CognitoAdminService, UserManagementService,
              CompanyService, ContactService, FollowUpService,
              InvestigationService, InvestigationContextService,
              ReportService, LlmService, PdfGenerationService,
              S3Service, EmailService, DashboardService
runner/       BaseRunner, ReconTaskDispatcher,
              DnsLookupRunner, WhoisRunner, HeadersRunner,
              PerformanceRunner, SslCertRunner, IpInfoRunner,
              TechStackRunner, SubdomainFinderRunner, SubdomainScanRunner,
              ShodanRunner, CensysRunner,
              DnsHistoryRunner, DirectIpScanRunner
entity/       Company, Contact, FollowUp, Investigation,
              InvestigationContext, ReconTask, Report, Pipeline
dto/          AuthDtos, UserDtos, CompanyDtos, ContactDtos,
              FollowUpDtos, InvestigationDtos, InvestigationContextDtos,
              ReportDtos, DashboardDtos
repository/   (one per entity)
job/          FollowUpReminderJob
exception/    AuthException, ResourceNotFoundException, GlobalExceptionHandler
```

---

## Key Design Decisions

### Recon Architecture

Every recon task is a `ReconTask` entity with status `PENDING → RUNNING → COMPLETED/FAILED`.

`ReconTaskDispatcher` auto-discovers all `BaseRunner` subclasses via Spring `@Component` and dispatches them in a thread pool (`recon-N` threads).

Each runner:
1. Calls `markRunning(task)`
2. Does its work
3. Calls `markCompleted(task, resultJson, rawOutputJson)` or `markFailed(task, error)`
4. Writes structured data to `InvestigationContext` via `InvestigationContextService`

`InvestigationContext` is a single row (one per investigation) that accumulates all recon findings. `GET /investigations/{id}/context` returns a fully structured response parsed from this row.

### Task Result Parsing

`ReconTask.result` is stored as a JSON string in the DB. When returning to the frontend it is parsed via:

```java
private Object parseAsMap(String json) {
    if (json == null) return null;
    try { return objectMapper.readValue(json, Map.class); }
    catch (Exception e) { return json; }
}

// rawOutput can be array, object, or plain text:
private Object parseAsAny(String json) {
    if (json == null) return null;
    try { return objectMapper.readValue(json, Object.class); }
    catch (Exception e) { return json; }
}
```

### CDN Bypass Flow

```
DNS_LOOKUP     → resolves Cloudflare IP (public)
DNS_HISTORY    → HackerTarget + ViewDNS → finds real origin IP
DIRECT_IP_SCAN → curl -H "Host: domain.com" http://REAL_IP
               → returns real Nginx/Apache/PHP without Cloudflare masking
```

`DirectIpScanRunner` uses `ProcessBuilder` + curl because Java's `HttpClient` blocks the `Host` header (security restriction).

### Report Generation Flow

```
POST /reports → creates report with status=PENDING
              → CompletableFuture.runAsync() kicks off generation
              → returns immediately to client

Background:
  1. buildPrompt(type, domain, ctx)   → single merged prompt
  2. llmService.generate(prompt)       → LangChain4j → Qubrid
  3. pdfService.generatePdf(...)       → markdown → HTML → iText PDF
  4. s3Service.uploadPdf(...)          → upload to S3
  5. report.status = COMPLETED         → save with s3Key

GET /reports/{id} → poll until COMPLETED, returns presigned S3 URL
```

On app restart, `@PostConstruct recoverStuckReports()` marks any `GENERATING` or `PENDING` reports as `FAILED`.

### TechStackRunner Sources

Analyzes in order: headers → HTML → JS bundles → robots.txt → DNS → infrastructure detection.

Detects: web server, framework, CDN, cloud provider, deployment platform, analytics tools, payment gateways, CRM, support tools, Saudi-specific platforms (Salla, Zid, Tap Payments, Moyasar).

### SubdomainScanRunner

For each subdomain found by `SubdomainFinderRunner`:
1. Resolves IP with `dig`
2. Checks if CDN IP (Cloudflare ranges hardcoded)
3. `curl -I` for HTTPS then HTTP fallback
4. If non-CDN IP → also runs direct IP scan (`curl -H "Host:"`)
5. Deep analysis from headers (security score, CORS policy, framework hints)
6. Flags: `API_ENDPOINT`, `DEV_ENVIRONMENT`, `REAL_IP_EXPOSED`, `ACCESSIBLE`, etc.

---

## Entity Relationships

```
Company (1) ──< Contact (many)
Company (1) ──< Investigation (many)
Company (1) ──< Report (many)
Contact (1) ──< FollowUp (many)
Investigation (1) ── InvestigationContext (1)   ← one-to-one
Investigation (1) ──< ReconTask (many)
Investigation (1) ──< Report (many)
```

---

## InvestigationContext Fields

All fields written by their respective runners:

```
DNS_LOOKUP:     resolvedIp, realIp, cdnDetected, cdnProvider, dnsRecords, nameservers
HEADERS:        serverHeader, poweredByHeader, httpStatusCode, httpsRedirect, allHeaders,
                xFrameOptions, contentSecurityPolicy
PERFORMANCE:    ttfb, dnsResolveTime, connectTime, tlsTime, totalTime,
                performanceHttpCode, performanceSizeBytes
SSL_CERT:       sslIssuer, sslExpiry, sslValid
IP_INFO:        ipCountry, ipCity, ipRegion, ipOrg, ipAsn, ipHosting, ipTimezone, ipHostname
TECH_STACK:     techDetected (JSON array), techInferred (JSON object), techSources (JSON object)
SUBDOMAINS:     subdomains (JSON array of strings)
SUBDOMAIN_SCAN: subdomainScanData (JSON — full per-subdomain analysis)
DNS_HISTORY:    dnsHistory (JSON array), nonCdnIps (JSON array)
DIRECT_IP_SCAN: directScanFindings, realServer, realPoweredBy, realRuntime,
                loadBalanced, orchestration
WHOIS:          whoisData (JSON)
SHODAN:         openPorts, exposedServices
CENSYS:         (parsed into ShodanInfo/CensysInfo in context response)
```

---

## Enums

### `ReconTaskType`
```
DNS_LOOKUP, WHOIS, SHODAN, CENSYS, IP_INFO, TECH_STACK,
SUBDOMAINS, SUBDOMAIN_SCAN, SSL_CERT, PERFORMANCE, HEADERS,
DNS_HISTORY, DIRECT_IP_SCAN
```

### `Contact.ContactStatus`
```
NEW, CONTACTED, INTERESTED, MEETING_SET, PROPOSAL_SENT,
NEGOTIATING, WON, LOST, ON_HOLD
```

### `FollowUp.FollowUpType`
```
CALL, VISIT, MEETING, EMAIL, WHATSAPP
```

### `FollowUp.FollowUpStatus`
```
SCHEDULED, DONE, MISSED
```

### `Company.CompanyStatus`
```
PROSPECT, CONTACTED, QUALIFIED, PROPOSAL, CLOSED_WON, CLOSED_LOST
```

### `Report.ReportType`
```
TECHNICAL_OVERVIEW, SALES_ROADMAP
```

### `Report.ReportStatus`
```
PENDING, GENERATING, COMPLETED, FAILED
```

---

## Auth & Security

- JWT via Cognito JWKS (auto-fetched from well-known endpoint)
- Roles extracted from `cognito:groups` claim → `ROLE_ADMIN_GROUP`, `ROLE_SALES`
- Swagger/OpenAPI excluded from security chain
- `@PreAuthorize("hasRole('ADMIN_GROUP')")` for admin-only operations
- `@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")` for general access
- `@PreAuthorize("hasRole('ADMIN_GROUP') or #sub == authentication.name")` for self-access

**Note:** Access token does NOT contain email/name — only `sub`, `username`, `cognito:groups`. Use ID token or call `getUserBySub(sub)` to get user details.

---

## Common Patterns

### Adding a new recon runner

```java
@Component
@Slf4j
public class MyNewRunner extends BaseRunner {

    @Override
    public ReconTaskType supports() {
        return ReconTaskType.MY_NEW_TASK;
    }

    @Override
    public void run(ReconTask task, InvestigationContext ctx) {
        String domain = task.getInvestigation().getDomain();
        markRunning(task);
        try {
            // do work
            String resultJson = toJson(myResultMap);
            contextService.writeMyData(task.getInvestigation().getId(), ...);
            markCompleted(task, resultJson, rawOutput);
        } catch (Exception ex) {
            markFailed(task, ex.getMessage());
        }
    }
}
```

### Adding a new context field

1. Add field to `InvestigationContext.java`
2. Add SQL: `ALTER TABLE investigation_contexts ADD COLUMN IF NOT EXISTS my_field ...`
3. Add write method to `InvestigationContextService`
4. Add record to `InvestigationContextDtos`
5. Add parse method to `InvestigationContextService`
6. Add to `InvestigationContextResponse` record
7. Add to `getContext()` method call

---

## Known Issues / Gotchas

- `crt.sh` (SSL_CERT runner) goes down frequently — retries with 5s delay, fails gracefully
- `ViewDNS` blocks automated requests (Cloudflare challenge) — falls back to HackerTarget
- `wkhtmltopdf` removed from Homebrew — use iText (already configured)
- `BuiltWith`/`Wappalyzer` block scraping — tech detection relies on header analysis + JS bundles
- WhoisXML DNS history endpoint uses `domain=` not `domainName=` parameter
- Java `HttpClient` blocks `Host` header — use `ProcessBuilder` + curl for direct IP scans
- Report generation takes 1-2 min (LLM) — always async, poll for completion
- SES sandbox mode: both sender AND recipient must be verified until production access granted

---

## Pending / Next Steps

- [ ] SES production access request
- [ ] SQS for async recon dispatch (currently local ThreadPool)
- [ ] Switch to Bedrock (`ai.provider=bedrock` in yml)
- [ ] ECS Fargate deployment + Terraform IaC
- [ ] CodePipeline CI/CD
- [ ] Google Calendar integration for follow-ups
- [ ] Arabic report generation (`language=ar`)
- [ ] MIGFORA RAG integration for report generation
- [ ] WhoisXML API key for better DNS history (500 free/month)