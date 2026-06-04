# MIGFORA Sales Intelligence Platform — Backend

A Spring Boot 4.x backend for the MIGFORA internal sales team. It combines a CRM (companies, contacts, follow-ups) with a deep technical reconnaissance engine that investigates prospect infrastructure and generates AI-powered sales reports.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 25, Spring Boot 4.0.6 |
| Database | PostgreSQL 16 |
| Auth | AWS Cognito (eu-central-1) |
| Storage | AWS S3 |
| Email | AWS SES |
| AI | LangChain4j + Qubrid (OpenAI-compatible) / Bedrock (future) |
| PDF | iText html2pdf |
| HTTP clients | Java HttpClient, Jsoup, ProcessBuilder (curl) |

---

## Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL running on port 5435
- AWS credentials configured (`~/.aws/credentials` or env vars)
- `curl`, `dig`, `subfinder` available in PATH

---

## Environment Variables

Create a `.env` file or set these in your environment:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5435/sales_platform
DB_USERNAME=postgres
DB_PASSWORD=postgres

# AWS Cognito
AWS_COGNITO_USER_POOL_ID=eu-central-1_SrsV54zGw
AWS_COGNITO_CLIENT_ID=5nma73odc6qrviu08f870920jn
AWS_REGION=eu-central-1

# AWS SES
SES_FROM_EMAIL=abdalrhman@migfora.com

# AWS S3
S3_BUCKET=migfora-reports

# AI
AI_PROVIDER=qubrid
QUBRID_API_KEY=your_key_here
QUBRID_BASE_URL=https://platform.qubrid.com/v1
AI_MODEL=deepseek-ai/DeepSeek-V4-Pro

# Recon — optional
SHODAN_API_KEY=
CENSYS_API_ID=
CENSYS_API_SECRET=
WHOISXML_API_KEY=
```

---

## Running Locally

```bash
# Start PostgreSQL
docker run -d -p 5435:5432 \
  -e POSTGRES_DB=sales_platform \
  -e POSTGRES_PASSWORD=postgres \
  postgres:16

# Run
mvn spring-boot:run
```

API available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui.html`

---

## Project Structure

```
src/main/java/com/migfora/sales/
├── config/
│   ├── AiConfig.java              # AI provider config (qubrid/bedrock)
│   ├── LangChain4jConfig.java     # ChatModel bean
│   ├── SecurityConfig.java        # JWT + Cognito security
│   └── CorrelationIdFilter.java   # Request tracing
│
├── controller/
│   ├── AuthController.java        # login, refresh, me
│   ├── UserController.java        # user management
│   ├── CompanyController.java     # company CRUD
│   ├── ContactController.java     # contact CRUD + status
│   ├── FollowUpController.java    # follow-up management
│   ├── InvestigationController.java # investigation sessions + task runner
│   ├── ReportController.java      # AI report generation
│   ├── DashboardController.java   # dashboard stats
│   └── AdminController.java       # admin jobs
│
├── service/
│   ├── CognitoAdminService.java   # Cognito user operations
│   ├── UserManagementService.java # user CRUD via Cognito
│   ├── CompanyService.java
│   ├── ContactService.java
│   ├── FollowUpService.java
│   ├── InvestigationService.java  # task dispatch + context
│   ├── InvestigationContextService.java # context read/write
│   ├── ReportService.java         # async report generation
│   ├── LlmService.java            # LangChain4j wrapper
│   ├── PdfGenerationService.java  # markdown → PDF via iText
│   ├── S3Service.java             # S3 upload + presigned URLs
│   ├── EmailService.java          # SES email sending
│   └── DashboardService.java      # stats aggregation
│
├── runner/
│   ├── BaseRunner.java            # abstract base for all recon runners
│   ├── ReconTaskDispatcher.java   # auto-registers and dispatches runners
│   ├── DnsLookupRunner.java
│   ├── WhoisRunner.java
│   ├── HeadersRunner.java
│   ├── PerformanceRunner.java
│   ├── SslCertRunner.java
│   ├── IpInfoRunner.java
│   ├── TechStackRunner.java
│   ├── SubdomainFinderRunner.java
│   ├── SubdomainScanRunner.java
│   ├── ShodanRunner.java
│   ├── CensysRunner.java
│   ├── DnsHistoryRunner.java
│   └── DirectIpScanRunner.java
│
├── entity/
│   ├── Company.java
│   ├── Contact.java
│   ├── FollowUp.java
│   ├── Investigation.java
│   ├── InvestigationContext.java  # aggregated recon results
│   ├── ReconTask.java             # individual task record
│   ├── Report.java
│   └── Pipeline.java
│
├── dto/
│   ├── AuthDtos.java
│   ├── UserDtos.java
│   ├── CompanyDtos.java
│   ├── ContactDtos.java
│   ├── FollowUpDtos.java
│   ├── InvestigationDtos.java
│   ├── InvestigationContextDtos.java
│   ├── ReportDtos.java
│   └── DashboardDtos.java
│
├── repository/
│   ├── CompanyRepository.java
│   ├── ContactRepository.java
│   ├── FollowUpRepository.java
│   ├── InvestigationRepository.java
│   ├── InvestigationContextRepository.java
│   ├── ReconTaskRepository.java
│   └── ReportRepository.java
│
├── job/
│   └── FollowUpReminderJob.java   # 8 AM daily SES reminders
│
└── exception/
    ├── AuthException.java
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java
```

---

## API Overview

### Auth — `/api/v1/auth`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/login` | Public | Email + password login |
| POST | `/refresh` | Public | Refresh access token |
| POST | `/change-password` | Public | Force change password |
| GET | `/me` | Any | Get current user info |

### Users — `/api/v1/users`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/` | Admin | Create user |
| GET | `/` | Admin | List all users |
| GET | `/{sub}` | Admin or self | Get user |
| PATCH | `/{sub}` | Admin or self | Update user |
| PATCH | `/{sub}/enable` | Admin | Enable user |
| PATCH | `/{sub}/disable` | Admin | Disable user |
| DELETE | `/{sub}` | Admin | Delete user |
| POST | `/{sub}/reset-password` | Any | Reset password |

### Companies — `/api/v1/companies`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/` | Any | Create company |
| GET | `/` | Any | List + search + filter |
| GET | `/{id}` | Any | Get company |
| PATCH | `/{id}` | Any | Update company |
| DELETE | `/{id}` | Admin | Delete company |

### Contacts — `/api/v1`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/companies/{id}/contacts` | Any | Create contact |
| GET | `/companies/{id}/contacts` | Any | List contacts |
| GET | `/contacts/{id}` | Any | Get contact |
| PATCH | `/contacts/{id}` | Any | Update contact |
| PATCH | `/contacts/{id}/status` | Any | Update status |
| DELETE | `/contacts/{id}` | Any | Delete contact |

### Follow-ups — `/api/v1`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/contacts/{id}/followups` | Any | Create follow-up |
| GET | `/contacts/{id}/followups` | Any | List follow-ups |
| GET | `/followups/{id}` | Any | Get follow-up |
| PATCH | `/followups/{id}` | Any | Update follow-up |
| DELETE | `/followups/{id}` | Any | Delete follow-up |
| GET | `/followups/today` | Any | Today's scheduled |

### Investigations — `/api/v1/investigations`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/` | Any | Create investigation |
| GET | `/` | Admin | List all |
| GET | `/{id}` | Any | Get investigation |
| GET | `/company/{id}` | Any | By company |
| GET | `/tasks/lookup` | Any | Available task types |
| POST | `/{id}/run` | Any | Run specific tasks |
| POST | `/{id}/run-all` | Any | Run all tasks |
| POST | `/{id}/tasks/check` | Any | Check task readiness |
| GET | `/{id}/tasks` | Any | List tasks |
| GET | `/{id}/tasks/{taskId}` | Any | Get task result |
| GET | `/{id}/context` | Any | Full aggregated context |
| PATCH | `/{id}/close` | Any | Close investigation |
| DELETE | `/{id}` | Admin | Delete investigation |

### Reports — `/api/v1/reports`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/` | Any | Generate report (async) |
| GET | `/company/{id}` | Any | List by company |
| GET | `/{id}` | Any | Get with download URL |
| DELETE | `/{id}` | Admin | Delete + remove from S3 |

### Dashboard — `/api/v1`
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/dashboard/stats` | Any | Counts summary |
| GET | `/contacts/stats` | Admin | Pipeline breakdown |

---

## Recon Task Types

| Task | Depends On | API Key Required |
|---|---|---|
| DNS_LOOKUP | — | No |
| WHOIS | — | No |
| HEADERS | — | No |
| PERFORMANCE | — | No |
| SSL_CERT | — | No |
| IP_INFO | DNS_LOOKUP | No |
| TECH_STACK | HEADERS | No |
| SUBDOMAINS | — | No |
| SUBDOMAIN_SCAN | SUBDOMAINS | No |
| DNS_HISTORY | DNS_LOOKUP | No |
| DIRECT_IP_SCAN | DNS_HISTORY | No |
| SHODAN | DNS_LOOKUP | Yes |
| CENSYS | DNS_LOOKUP | Yes |

---

## Report Types

| Type | Description |
|---|---|
| `TECHNICAL_OVERVIEW` | Infrastructure, tech stack, security, performance analysis |
| `SALES_ROADMAP` | AWS migration plan, improvements, sales talking points |

Reports are generated async — poll `GET /api/v1/reports/{id}` until `status: COMPLETED`. PDF uploaded to S3, presigned download URL returned.

---

## AWS Setup

### Cognito
- User Pool: `eu-central-1_SrsV54zGw`
- Client ID: `5nma73odc6qrviu08f870920jn`
- Groups: `admin_group`, `sales`

### SES
- Verified sender: `abdalrhman@migfora.com`
- Used for: daily follow-up reminders (8 AM Riyadh time)
- Note: request production access to send to unverified emails

### S3
- Bucket: `migfora-reports`
- Structure: `reports/company-{id}/investigation-{id}/{reportId}-{type}-{date}.pdf`
- Presigned URL expiry: 60 minutes

---

## Scheduled Jobs

| Job | Schedule | Description |
|---|---|---|
| `FollowUpReminderJob` | 8:00 AM Asia/Riyadh | Sends daily follow-up reminder emails via SES |

Manual trigger: `POST /api/v1/admin/jobs/reminders/trigger`

---

## AI Configuration

Switch provider in `application.yml`:

```yaml
ai:
  provider: qubrid   # or: bedrock
  qubrid:
    api-key: ${QUBRID_API_KEY}
    base-url: https://platform.qubrid.com/v1
    model: deepseek-ai/DeepSeek-V4-Pro
    timeout-minutes: 5
  bedrock:
    region: eu-central-1
    model: anthropic.claude-3-5-sonnet-20241022-v2:0
```