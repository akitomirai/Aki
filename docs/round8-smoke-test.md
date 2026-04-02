# Round 8 Smoke Test

> Current recommendation
> - Main browser regression entry: `scripts/run-baseline-regression.ps1`
> - Auth-aware runtime precheck: `scripts/demo-precheck.ps1`
> - Full backend regression entry: `scripts/backend-test.ps1 -Task test`
> - This document keeps the older `scripts/run-round8-smoke.ps1` helper aligned with the current system

## Legacy Helper Entry

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-round8-smoke.ps1
```

Keep services running after the smoke if needed:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-round8-smoke.ps1 -KeepServices
```

## Current Stable URLs

- Admin login: `http://127.0.0.1:5174/login`
- Public trace: `http://127.0.0.1:5173/t/orange-202603-d1`

## Current Regression Relationship

- `scripts/check-local.ps1`
  - Lightweight runtime check for anonymous-safe routes and service health
- `scripts/demo-precheck.ps1`
  - Logs in as `platform / 123456` and verifies the authenticated admin batch baseline through `/api/batches`
- `scripts/backend-test.ps1 -Task test`
  - Runs the full backend test suite, including the operation-log permission chain
- `scripts/run-baseline-regression.ps1`
  - Runs the current main browser regression and appends `specs/log-permission-smoke.spec.mjs`
- `scripts/run-round8-smoke.ps1`
  - Preserves the older standalone smoke helper flow

## Artifacts From `run-round8-smoke.ps1`

- HTML report: `artifacts/playwright-report`
- Playwright raw results: `artifacts/test-results`
- Screenshots: `artifacts/screenshots`
- Service logs: `artifacts/logs/services`
- Process state: `artifacts/logs/round8-processes.json`
- Playwright JSON result: `artifacts/logs/playwright-results.json`

## Failure Triage Order

1. `artifacts/logs/services/backend-demo.log`
2. `artifacts/logs/services/admin-web.log`
3. `artifacts/logs/services/trace-web.log`
4. `artifacts/logs/playwright-results.json`
5. `artifacts/playwright-report/index.html`
