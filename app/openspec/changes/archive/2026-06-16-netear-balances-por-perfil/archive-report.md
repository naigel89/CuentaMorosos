# Archive Report

**Change**: netear-balances-por-perfil
**Archived at**: 2026-06-16
**Verdict**: PASS WITH WARNINGS
**Mode**: openspec

## Specs Synced

| Domain | Action | Details |
|--------|--------|---------|
| dashboard-debt-calculations | Updated | 3 requirements added (profile debt netting, negative amounts for youOwe, event sign-based coloring) |

## Archive Contents

| Artifact | Location | Status |
|----------|----------|--------|
| Delta specs | `specs/dashboard-debt-calculations/spec.md` | ✅ |
| Verify report | `verify-report.md` | ✅ (PASS WITH WARNINGS — 21/21 tests, 9/9 scenarios compliant) |

## Source of Truth Updated

- `openspec/specs/dashboard-debt-calculations/spec.md` — 3 new requirements merged, 12 scenarios added across them

## Verification Summary

- **Tests**: 21 passed / 0 failed
- **Spec compliance**: 9/9 scenarios compliant
- **Issues**: 2 warnings (function not placed in calculator as designed, old private impl coexists)
- **SDD Cycle**: Complete — all phases executed (propose → spec → design → tasks → apply → verify → archive)

## Notes

- No `config.yaml` exists at `openspec/` root, so no project-specific archive rules were applied.
- The change had no proposal, design, or tasks artifacts in the filesystem (only Engram). The verify-report and spec are the only filesystem artifacts.
