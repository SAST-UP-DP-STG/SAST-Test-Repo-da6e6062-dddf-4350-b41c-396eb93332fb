# Field Service Dispatch System

Internal field-service application for managing technicians, work orders, parts inventory, customer contracts, and dispatching.

## Technology Stack

- Java 17
- Spring Boot 3.3.7 (Web, Security, Data JPA, Validation, Thymeleaf)
- Thymeleaf server-rendered pages with a static JS client
- H2 file database
- JSON Web Tokens (jjwt 0.11.5) for the main API surface

## Setup

The Maven wrapper is included; no local Maven installation is required. JDK 17 must be available.

```bash
cd Examples/CWE-306/Java/FieldServiceDispatchSystem
./mvnw -DskipTests compile
```

## Run

```bash
./mvnw -DskipTests spring-boot:run
```

Application URL:
- http://127.0.0.1:8080

The internal dispatch command listener starts with the application on `0.0.0.0:9090` (all interfaces); connect locally via `127.0.0.1:9090`.

## Seeded Accounts

- administrator
  - username: `amaral`
  - password: `Admin#2024`
- dispatcher
  - username: `dcosta`
  - password: `Dispatch#2024`

## Database Access

The application uses an H2 file database. The web console is available while the app is running:

- Console URL: http://127.0.0.1:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/dispatch-db`
- Driver class: `org.h2.Driver`
- Username: `sa`
- Password: _(leave blank)_

The database file is created under `data/dispatch-db.mv.db` on first startup.

## Main Routes

- `GET  /login`, `/dashboard`, `/operations`, `/admin` — server-rendered pages
- `POST /api/auth/login` — issue a JWT
- `GET  /api/dashboard/summary`, `/api/work-orders`, `/api/work-orders/{id}`, `/api/technicians`, `/api/parts`, `/api/customers` — authenticated API surface
- `POST /api/work-orders/{id}/close` — close a work order (authenticated; protected twin of the unauthenticated `/integration` path)
- `POST /api/work-orders/{id}/dispatch` — release a technician dispatch (authenticated; protected twin of the unauthenticated `/integration` path)
- `GET  /admin/operators` — authenticated operator listing
- `POST /integration/work-orders/close` — close a work order
- `POST /integration/dispatch/release` — release a technician dispatch
- `GET  /reports/customers/export` — export customer contracts (CSV)
- `POST /tools/parts/recount` — bulk parts recount upload
- `POST /admin/bootstrap/account` — first-run operator account creation
- `GET  /internal/diagnostics` — runtime diagnostics
- `GET  /h2-console` — database console
- `GET  /actuator/**` — Spring Boot management endpoints (exposed via `exposure.include=*`)

## Critical Functions and Access

The main `/api/**` API surface and `/admin/**` require a valid JWT, and the security configuration denies by default (`anyRequest().authenticated()`). A small set of secondary integration, reporting, support, diagnostic, and internal-service paths sit outside that authentication boundary.

### 1) Unauthenticated Web/API Endpoints

These critical operations are reachable by any caller, each through a different realistic authentication-boundary mistake (see `config/SecurityConfig.java`):

- `POST /integration/work-orders/close` — closes a work order (state-changing); claimed by a **legacy integration `SecurityFilterChain`** (`securityMatcher("/integration/**")`, `@Order(1)`) that predates the JWT model and permits all requests
- `POST /integration/dispatch/release` — assigns a technician and releases the dispatch (state-changing); same legacy integration chain
- `POST /tools/parts/recount` — bulk-updates on-hand parts quantities from an uploaded CSV (state-changing import); claimed by a **support/import tooling `SecurityFilterChain`** (`securityMatcher("/tools/**")`, `@Order(2)`) that permits all requests
- `GET  /reports/customers/export` — exports customer contracts including account numbers and contract values (sensitive export); the one endpoint left on a **direct `permitAll()`** rule in the main chain

#### Identity trust-header (token issued but never validated)

`auth/JwtAuthenticationFilter.java` also contains a legacy gateway-trust fallback: when no `Authorization: Bearer` token is present, it reads the `X-Authenticated-User` header and trusts it as an authenticated identity, with no token issued or validated. As a result the otherwise-protected `/api/**` and `/admin/**` surfaces (including the authenticated `/api/work-orders/{id}/close` and `/dispatch` twins) are reachable by any caller that sends `X-Authenticated-User: <anyone>`. Source: the `X-Authenticated-User` request header; sink: `SecurityContextHolder.getContext().setAuthentication(...)`. No role is taken from the header, so this is missing authentication rather than authorization.

### 2) Security-Config Authentication Gaps

- `POST /admin/bootstrap/account` — a forgotten first-run setup route that creates an operator account with any role (including `ADMIN`); reachable because `/admin/bootstrap/**` is in `permitAll()` while the rest of `/admin/**` is authenticated
- `GET  /internal/diagnostics` — returns datasource URL/username, command-server port, JWT issuer, and all system settings (config exposure)
- `GET  /h2-console` — the H2 database console is enabled and exposed via `permitAll("/h2-console/**")`
- `GET  /actuator/**` — Spring Boot Actuator is enabled with `management.endpoints.web.exposure.include=*` (`application.yml`) and `/actuator/**` is left on `permitAll()` in the main chain, exposing `/actuator/env`, `/actuator/heapdump`, `/actuator/configprops`, and similar diagnostic endpoints without authentication

### 3) Unauthenticated Network Services and Backend Interfaces

A legacy internal dispatch command listener (`net/DispatchCommandServer.java`) binds a TCP socket on `0.0.0.0:9090` (all interfaces, routable), trusts the network, and performs no authentication handshake. It accepts:

- `CANCEL_JOB <workOrderId>` — sets a work order to `CANCELLED` (state-changing)
- `DISPATCH_TECH <workOrderId> <technicianId>` — forces a dispatch (state-changing)
- `RECONCILE_PARTS` — runs a full parts reconciliation job (privileged service execution)
- `SYNC_CUSTOMERS` — runs a full customer sync job (privileged service execution)

### Protected (non-sink) endpoints — where authentication IS enforced

These endpoints require an identity before they run, so they are the protected contrast for the per-route and network sinks above. Enforcement happens in two centralized places:

- `src/main/java/com/fielddispatch/auth/JwtAuthenticationFilter.java` — reads the `Authorization: Bearer` header, validates the token (`jwtService.isTokenValid(token)`), and only then sets the authenticated identity. The filter is installed ahead of the main chain in `SecurityConfig`. **Caveat:** this same filter also contains the `X-Authenticated-User` trust-header fallback described above, so these endpoints are protected against a missing token but not against a forged-identity header.
- `src/main/java/com/fielddispatch/config/SecurityConfig.java` (main chain, `@Order(3)`) — `.requestMatchers("/api/**").authenticated()`, `.requestMatchers("/admin/**").authenticated()`, and `.anyRequest().authenticated()`. Requests without an identity are rejected with `401` by the entry point (`HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`).

Endpoints covered by these checks:

| Endpoint | Handler | Enforced by |
|----------|---------|-------------|
| `GET /api/dashboard/summary` | `ApiController` (line 39) | `/api/**` authenticated (SecurityConfig line 58) + JWT filter |
| `GET /api/work-orders` | `ApiController` (line 50) | `/api/**` authenticated (SecurityConfig line 58) + JWT filter |
| `GET /api/work-orders/{id}` | `ApiController` (line 55) | `/api/**` authenticated (SecurityConfig line 58) + JWT filter |
| `GET /api/technicians` | `ApiController` (line 62) | `/api/**` authenticated (SecurityConfig line 58) + JWT filter |
| `GET /api/parts` | `ApiController` (line 67) | `/api/**` authenticated (SecurityConfig line 58) + JWT filter |
| `GET /api/customers` | `ApiController` | `/api/**` authenticated + JWT filter |
| `POST /api/work-orders/{id}/close` | `ApiController` | `/api/**` authenticated + JWT filter (protected twin of `/integration/work-orders/close`) |
| `POST /api/work-orders/{id}/dispatch` | `ApiController` | `/api/**` authenticated + JWT filter (protected twin of `/integration/dispatch/release`) |
| `GET /admin/operators` | `AdminController` | `/admin/**` authenticated + JWT filter |

Public by design (not sinks, no protected data): `POST /api/auth/login` is the pre-authentication login endpoint that issues the JWT (SecurityConfig line 53); the page routes (`/`, `/login`, `/dashboard`, `/operations`, `/admin`) and static assets (`/css/**`, `/js/**`) only serve HTML shells and frontend code, while their data is fetched through the authenticated `/api/**` endpoints above (SecurityConfig lines 51-52).

## Verify Sinks

Each test sends a request with **no credentials** (no `Authorization` header, no cookie, no token) and shows the critical function still executes. State-changing tests modify seeded data — restart the application to reset (`./mvnw -DskipTests spring-boot:run`).

Seeded data reference:

| Entity | ID | Notes |
|--------|----|-------|
| Work order | WO-7001 (id 1) | OPEN |
| Work order | WO-7003 (id 3) | OPEN |
| Work order | WO-7004 (id 4) | SCHEDULED |
| Technician | id 2, id 3 | dispatch targets |
| Part | PRT-1001, PRT-1002 | recount targets |
| Customer | id 1-3 | contract export |

### 1) Unauthenticated Web/API Endpoints

**Linux / macOS:**

```bash
# close a work order
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"workOrderId":1,"resolution":"done"}' \
  http://127.0.0.1:8080/integration/work-orders/close
# release a technician dispatch
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"workOrderId":3,"technicianId":2}' \
  http://127.0.0.1:8080/integration/dispatch/release
# export customer contracts
curl -s http://127.0.0.1:8080/reports/customers/export
# bulk parts recount
printf 'sku,quantity\nPRT-1001,5\nPRT-1002,99\n' > recount.csv
curl -s -X POST -F "file=@recount.csv" http://127.0.0.1:8080/tools/parts/recount
```

**Windows (PowerShell):**

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/integration/work-orders/close" -Method POST -ContentType "application/json" -Body '{"workOrderId":1,"resolution":"done"}'
Invoke-RestMethod -Uri "http://127.0.0.1:8080/integration/dispatch/release" -Method POST -ContentType "application/json" -Body '{"workOrderId":3,"technicianId":2}'
Invoke-RestMethod -Uri "http://127.0.0.1:8080/reports/customers/export" -Method GET
"sku,quantity`nPRT-1001,5`nPRT-1002,99" | Out-File -Encoding ascii recount.csv
# curl.exe handles the multipart upload on both Windows PowerShell 5.1 and PowerShell 7
# (Invoke-RestMethod -Form requires PowerShell 7+)
curl.exe -s -X POST -F "file=@recount.csv" http://127.0.0.1:8080/tools/parts/recount
```

**Expected:** All four return `200` with a JSON/CSV result, with no authentication — the work order is closed, the dispatch is released, the contract export is returned, and parts are updated.

#### Identity trust-header (token never validated)

Reach a protected `/api/**` endpoint with no token, just the trusted header:

**Linux / macOS:**

```bash
# 200: the X-Authenticated-User header is trusted as an authenticated identity
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/work-orders \
  -H "X-Authenticated-User: amaral"
# 401: same request with neither token nor header
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/work-orders
```

**Windows (PowerShell):**

```powershell
# 200: the X-Authenticated-User header is trusted as an authenticated identity
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/work-orders" -Headers @{ "X-Authenticated-User" = "amaral" } -UseBasicParsing).StatusCode
# 401: same request with neither token nor header
try { Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/work-orders" -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
```

The same trick reaches the authenticated close/dispatch twins (`POST /api/work-orders/{id}/close` and `/dispatch`) and any `/admin/**` route.

**Expected:** `200` with the header, `401` without it.

#### Same action, protected vs unprotected (contrast)

The work-order close/dispatch operations exist both behind authentication (`/api/...`) and wide open (`/integration/...`):

**Linux / macOS:**

```bash
# protected twin: 401 without a token
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://127.0.0.1:8080/api/work-orders/1/close \
  -H "Content-Type: application/json" -d '{"resolution":"done"}'
# protected twin: 200 with a valid token
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"amaral","password":"Admin#2024"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://127.0.0.1:8080/api/work-orders/1/close \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"resolution":"done"}'
# unprotected path: 200 with no credentials at all
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://127.0.0.1:8080/integration/work-orders/close \
  -H "Content-Type: application/json" -d '{"workOrderId":1,"resolution":"done"}'
```

**Windows (PowerShell):**

```powershell
# protected twin: 401 without a token
try { Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/work-orders/1/close" -Method POST -ContentType "application/json" -Body '{"resolution":"done"}' -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
# protected twin: 200 with a valid token
$token = (Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"amaral","password":"Admin#2024"}').token
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/api/work-orders/1/close" -Method POST -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body '{"resolution":"done"}' -UseBasicParsing).StatusCode
# unprotected path: 200 with no credentials at all
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/integration/work-orders/close" -Method POST -ContentType "application/json" -Body '{"workOrderId":1,"resolution":"done"}' -UseBasicParsing).StatusCode
```

**Expected:** `401` (no token), `200` (valid token), `200` (unauthenticated `/integration` path) — the same state change is reachable both ways.

### 2) Security-Config Authentication Gaps

**Linux / macOS:**

```bash
# create an ADMIN operator with no authentication
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"username":"intruder","fullName":"Intruder","role":"ADMIN","password":"p@ss1234"}' \
  http://127.0.0.1:8080/admin/bootstrap/account
# dump runtime diagnostics
curl -s http://127.0.0.1:8080/internal/diagnostics
# database console (HTML login form served without authentication)
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/h2-console/
```

**Windows (PowerShell):**

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/admin/bootstrap/account" -Method POST -ContentType "application/json" -Body '{"username":"intruder","fullName":"Intruder","role":"ADMIN","password":"p@ss1234"}'
Invoke-RestMethod -Uri "http://127.0.0.1:8080/internal/diagnostics" -Method GET
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/h2-console/" -UseBasicParsing).StatusCode
```

**Expected:** Account creation returns `201` with the new `ADMIN` operator; diagnostics returns `200` with datasource and settings details; the H2 console returns `200`.

Spring Boot Actuator over-exposure:

**Linux / macOS:**

```bash
# environment variables and configuration, with no authentication
curl -s http://127.0.0.1:8080/actuator/env
# heap dump download, with no authentication
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/actuator/heapdump
```

**Windows (PowerShell):**

```powershell
# environment variables and configuration, with no authentication
Invoke-RestMethod -Uri "http://127.0.0.1:8080/actuator/env" -Method GET
# heap dump download, with no authentication
(Invoke-WebRequest -Uri "http://127.0.0.1:8080/actuator/heapdump" -UseBasicParsing).StatusCode
```

**Expected:** `/actuator/env` returns `200` with environment/configuration data; `/actuator/heapdump` returns `200` and downloads a heap dump — both without authentication.

### 3) Unauthenticated Network Services and Backend Interfaces

The dispatch command listener accepts a single command line per connection on `127.0.0.1:9090`.

**Linux / macOS:**

```bash
printf 'CANCEL_JOB 4\n'        | nc 127.0.0.1 9090
printf 'DISPATCH_TECH 1 3\n'   | nc 127.0.0.1 9090
printf 'RECONCILE_PARTS\n'     | nc 127.0.0.1 9090
printf 'SYNC_CUSTOMERS\n'      | nc 127.0.0.1 9090
```

**Windows (PowerShell):**

```powershell
function Send-Command($cmd) {
  $client = New-Object System.Net.Sockets.TcpClient("127.0.0.1", 9090)
  $stream = $client.GetStream()
  $writer = New-Object System.IO.StreamWriter($stream); $writer.AutoFlush = $true
  $reader = New-Object System.IO.StreamReader($stream)
  $writer.WriteLine($cmd)
  $reader.ReadLine()
  $client.Close()
}
Send-Command "CANCEL_JOB 4"
Send-Command "DISPATCH_TECH 1 3"
Send-Command "RECONCILE_PARTS"
Send-Command "SYNC_CUSTOMERS"
```

**Expected:** Each command returns an `OK ...` response with no handshake — `CANCEL_JOB` cancels the work order, `DISPATCH_TECH` forces a dispatch, `RECONCILE_PARTS` and `SYNC_CUSTOMERS` run their backend jobs.

## Administrator Flows

- Review operator accounts and open diagnostic tools (`/admin`)
- View the operations dashboard (`/dashboard`)
- Run integration, reporting, and support actions (`/operations`)

## Dispatcher Flows

- Monitor work orders, technicians, and parts inventory (`/dashboard`)
- Close work orders and release dispatches (`/operations`)
