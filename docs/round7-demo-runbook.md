# Round 7 Demo Runbook

> Current local runtime guidance
> - Standard local startup: `scripts/start-local.ps1`
> - Anonymous-safe runtime check: `scripts/check-local.ps1`
> - Auth-aware demo precheck: `scripts/demo-precheck.ps1`
> - Main browser regression: `scripts/run-baseline-regression.ps1`
> - This document keeps the older `scripts/start-round7-demo.ps1` helper aligned with the current system

## Quick Start

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-round7-demo.ps1
```

Current stable entry points:

- Admin login: `http://127.0.0.1:5174/login`
- Public trace: `http://127.0.0.1:5173/t/orange-202603-d1`

Then run the current precheck:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-precheck.ps1
```

## Login And Roles

The admin side now requires login. Recommended demo accounts:

- `platform / 123456`
- `enterprise_admin / 123456`
- `operator / 123456`

After login, each role lands on its permitted area and keeps current permission boundaries. The log page remains role-restricted.

## Demo Baseline Used By Current Scripts

- Fixed public trace batch: `ORANGE-202603-D1`
- Fixed public trace token: `orange-202603-d1`
- Default support assignee: `现场操作员B / operator_support`
- Published baseline status: `已发布 (PUBLISHED)`
- Baseline quality status: `合格`

## Entry Relationship

- `scripts/start-local.ps1`
  - Start the standard local runtime
- `scripts/stop-local.ps1`
  - Stop the standard local runtime
- `scripts/check-local.ps1`
  - Check anonymous-safe routes and service health only
- `scripts/demo-precheck.ps1`
  - Verify the current demo baseline with a real authenticated platform login
- `scripts/backend-test.ps1 -Task test`
  - Run the full backend regression suite
- `scripts/run-baseline-regression.ps1`
  - Run the current main browser regression, including the log permission smoke
