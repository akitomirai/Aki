# Backend Development Entry

## Goal

This note fixes the current backend development baseline into repeatable scripts so a new PowerShell session can quickly switch to the same toolchain and run package, tests, demo startup, and health checks without editing system-wide environment variables.

## Why A New Terminal Must Switch To JDK 21 First

The current machine still exposes Java 8 as the default `java` in a fresh PowerShell session. The backend does not run on that baseline:

1. The backend uses Spring Boot 3.5.x and compiles with Java 17 bytecode, so Java 8 is too old for compile and runtime.
2. A fresh shell may also make `mvn` fail early with `The JAVA_HOME environment variable is not defined correctly`.
3. Even when `mvn` is found, it must run against JDK 21 in this workspace so `package`, `test`, and demo startup behave the same way every time.

The new scripts solve that by pinning `JAVA_HOME` to `C:\Program Files\Java\jdk-21` and prepending the matching `bin` path only inside the current PowerShell process.

## Scripts

All scripts live under `scripts/`:

- `scripts/backend-env.ps1`
- `scripts/backend-test.ps1`
- `scripts/backend-run-demo.ps1`
- `scripts/backend-doctor.ps1`

### 1. Load JDK 21 And Maven In The Current Session

If you want the current terminal itself to keep the environment, dot-source the script:

```powershell
. .\scripts\backend-env.ps1
```

What it does:

1. Sets `JAVA_HOME` to `C:\Program Files\Java\jdk-21`
2. Prepends `%JAVA_HOME%\bin` to the current session `PATH`
3. Prepends Maven `bin` when `C:\Program Files\Java\apache-maven-3.8.8` exists
4. Prints `java -version` and `mvn -version`
5. Does not modify machine-level or user-level environment variables

If you call it through `powershell -File`, the environment is still isolated to that child PowerShell process, which is useful for one-shot commands.

### 2. Run Package And Tests

Package only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task package
```

Tests only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task test
```

Both in sequence:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task all
```

Actual Maven commands used by the script:

1. `mvn clean package -DskipTests`
2. `mvn test`

Before `clean package`, stop any running demo backend that was started from the packaged jar. On Windows, an active `java -jar backend\target\traceability-backend-0.0.1-SNAPSHOT.jar` process can lock the jar file and make Maven clean fail.

### 3. Start Demo Backend

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-run-demo.ps1 -ForceRestart
```

What the script does:

1. Pins the PowerShell session to JDK 21
2. Builds the backend jar when `backend\target\traceability-backend-0.0.1-SNAPSHOT.jar` is missing
3. Starts the packaged jar with `--spring.profiles.active=demo`
4. Waits for `/actuator/health`
5. Writes runtime state and logs under `output\runtime`

Outputs from the script include:

1. Health URL: `http://127.0.0.1:8080/actuator/health`
2. Stdout log: `output\runtime\backend-demo.stdout.log`
3. Stderr log: `output\runtime\backend-demo.stderr.log`
4. State file: `output\runtime\backend-demo-state.json`

Use `-ForceRestart` when you want to replace the tracked demo backend started by the same workspace. The script intentionally refuses to kill an unknown process already occupying port `8080`.

### 4. Run A Lightweight Self Check

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-doctor.ps1
```

It checks at least:

1. `JAVA_HOME` is exactly `C:\Program Files\Java\jdk-21`
2. `java -version` reports JDK 21
3. `mvn -version` runs successfully and is using Java 21
4. `http://127.0.0.1:8080/actuator/health` returns `UP`

## Runtime Demo Data Baseline

The demo backend profile uses these files:

- `backend/src/main/resources/application-demo.yml`
- `backend/src/main/resources/schema-demo.sql`
- `backend/src/main/resources/data-demo.sql`

That means restarting the backend with the `demo` profile rebuilds the in-memory H2 demo baseline from those main-resource seed files.

## Runtime Precheck Distinction

The current local toolchain now separates anonymous-safe checks from authenticated admin checks:

1. `scripts/check-local.ps1`
   - Only checks service health, the admin login route, and the public trace route
   - Does not call authenticated admin APIs
2. `scripts/demo-precheck.ps1`
   - Logs in as `platform / 123456`
   - Verifies the authenticated admin batch baseline through `/api/batches`
   - Still checks the anonymous public trace page separately

This matters because `/api/batches` is no longer treated as an anonymous precheck endpoint in the current system.

## Test Database Baseline

The backend integration tests currently depend on these test resources:

- `backend/src/test/resources/application.yml`
- `backend/src/test/resources/schema.sql`
- `backend/src/test/resources/data.sql`

`schema.sql` defines the H2 test schema used during `mvn test`. `data.sql` seeds the minimum company, user, product, batch, QR, and quality data needed by the current backend tests.

## What `AuthenticatedIntegrationTestSupport` Does

File:

- `backend/src/test/java/edu/jxust/agritrace/support/AuthenticatedIntegrationTestSupport.java`

Purpose:

1. Creates a Spring Security `SecurityContext` before each integration test
2. Injects a platform-admin-like `AuthUserSession` principal into that test context
3. Clears the context after each test

Why it exists:

1. The backend auth and permission checks are now active in tests
2. We did not want to reopen anonymous write access just to satisfy integration tests
3. This support class keeps test entry conditions stable without changing production controller or service logic

## Log Permission Regression Baseline

Backend guard:

- `backend/src/test/java/edu/jxust/agritrace/module/log/OperationLogControllerIntegrationTest.java`

This integration test now guards the full `/api/logs` permission chain:

1. `PLATFORM_ADMIN` can read cross-company logs
2. `ENTERPRISE_ADMIN` only sees company `1` scope in the seeded test baseline
3. `ENTERPRISE_ADMIN` is rejected when requesting another `companyId`
4. `OPERATOR` is rejected from the logs API
5. Rejected requests write `LOG_ACCESS_DENIED` failure records

When to run what:

1. Run the targeted test while iterating on log scope or permission code:

```powershell
. .\scripts\backend-env.ps1 -Quiet -SkipVersionOutput
mvn -Dtest=OperationLogControllerIntegrationTest test
```

2. Run the full backend baseline before handing off or merging permission-related backend changes:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task test
```

That `mvn test` entry is the main backend regression gate. `OperationLogControllerIntegrationTest` is now part of that full suite.

For runtime verification around the same area:

1. Run `scripts/demo-precheck.ps1` if you need a quick auth-aware demo baseline check
2. Run `scripts/run-baseline-regression.ps1` if you need the browser baseline, which now also executes the log permission smoke

## Recommended Daily Flow

```powershell
# package
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task package

# test
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-test.ps1 -Task test

# run demo backend
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-run-demo.ps1 -ForceRestart

# verify toolchain and health
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\backend-doctor.ps1
```
