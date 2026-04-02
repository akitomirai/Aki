# 2026-04-02 Clean Baseline Freeze

## Freeze Scope

- Freeze date: `2026-04-02`
- Baseline source verification time: `2026-04-02T09:58:23.264Z` (`2026-04-02 17:58:23` Asia/Shanghai)
- Local runtime start time: `2026-04-02T17:56:58` from `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\local-run-state.json`
- Branch: `codex/b`
- Commit: `5dac2f36b16c06a446d380d519c9dfe8aa56f62f`
- Workspace status at baseline source verification: `git status --short` returned empty
- Scope note: this freeze only adds docs/output traceability material and does not modify backend APIs, database schema, or frontend business logic

## Real Startup And Health

### Services actually started

- `backend`
  - Runner PID: `7140`
  - Listener PID: `32120` (`java.exe`)
  - URL: `http://127.0.0.1:8080`
  - Log: `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\backend.log`
- `admin-web`
  - Runner PID: `48808`
  - Listener PID: `46264` (`node.exe`)
  - URL: `http://127.0.0.1:5174`
  - Log: `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\admin-web.log`
- `trace-web`
  - Runner PID: `36436`
  - Listener PID: `18112` (`node.exe`)
  - URL: `http://127.0.0.1:5173`
  - Log: `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\trace-web.log`

### Health checks actually verified

- `http://127.0.0.1:8080/actuator/health`
  - HTTP `200`
  - Body: `{"status":"UP"}`
- `http://127.0.0.1:5174/login`
  - HTTP `200`
  - Final URI: `http://127.0.0.1:5174/login`
- `http://127.0.0.1:5173`
  - HTTP `200`
  - Final URI: `http://127.0.0.1:5173/`
- Supplementary script result
  - `powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1`
  - `backend / admin-web / trace-web` all returned `success 200`

## Baseline Regression Result

- Command: `powershell -ExecutionPolicy Bypass -File .\scripts\run-baseline-regression.ps1`
- Result: `Baseline regression PASSED`

### PASS items

- `demo seed is restored to the published baseline`
- `assignment chain, operator submit, risk chain, and public linkage all passed`
- `log permission smoke passed for platform, enterprise_admin, and operator`
- `regulator readonly smoke passed for default landing, read-only pages, and deny logging`
- `high-frequency pages do not expose English seed values or raw enum values`
- `publishedAt, task status, and risk status remain consistent`

### Expected intercepts

- `POST http://127.0.0.1:5174/api/batches/2/assignment`
  - HTTP `400`
  - Code: `ASSIGNMENT_DRAFT_BLOCK`
  - Label: `改派前草稿阻断`
  - Message: `该批次当前分配人存在未提交草稿，请先取消改派，或确认强制改派并清除原分配人的未提交草稿。`
  - Why this is not a failure: this is an expected business-rule interception. The regression intentionally verifies that reassignment is blocked before force-clearing an assignee's unsubmitted draft, so the script records it under `Expected intercepts` instead of `True failures`.

### True failures

- `none`

## Smoke Result

- `specs/log-permission-smoke.spec.mjs`
  - `3 passed`
- `specs/regulator-readonly-smoke.spec.mjs`
  - `3 passed`

## Real Artifact Paths

- Baseline JSON: `D:\Users\Lenovo\Desktop\traceability-system\output\playwright\full-regression-baseline.json`
- Baseline browser script: `D:\Users\Lenovo\Desktop\traceability-system\output\playwright\full-regression-baseline.mjs`
- Baseline screenshot directory: `D:\Users\Lenovo\Desktop\traceability-system\output\playwright`
- Lightweight baseline summary: `D:\Users\Lenovo\Desktop\traceability-system\output\playwright\latest-baseline-summary.json`
- Runtime state file: `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\local-run-state.json`
- Runtime logs:
  - `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\backend.log`
  - `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\admin-web.log`
  - `D:\Users\Lenovo\Desktop\traceability-system\output\runtime\trace-web.log`
- Related existing docs:
  - `D:\Users\Lenovo\Desktop\traceability-system\docs\demo-baseline.md`
  - `D:\Users\Lenovo\Desktop\traceability-system\docs\round8-smoke-test.md`

## Runner PID vs Listener PID

- `output\runtime\local-run-state.json` records the PowerShell runner PID launched by `scripts\local-service-runner.ps1`.
- The actual port owner is a child process such as `java.exe` or `node.exe`.
- For this freeze:
  - backend runner `7140`, listener `32120`
  - admin-web runner `48808`, listener `46264`
  - trace-web runner `36436`, listener `18112`
- Use the listener PID when checking port occupancy or process-level failures, and use the runner PID when correlating with `local-run-state.json`.

