# Git Rules

## Branch Naming

| Type | Format | Example |
|------|--------|---------|
| Feature | `feature/AND-xxx_short_description` | `feature/AND-13391_balance_fetcher` |
| Bugfix | `bugfix/AND-xxx_short_description` | `bugfix/AND-14000_fix_crash` |
| Pre-release | `x.x_pre_release` | `5.36_pre_release` |

**Key branches:**
- `develop` — main integration branch, all feature/bugfix branches merge here
- `x.x_pre_release` — branched from `develop` on the last day of sprint for the upcoming release; receives regression bugfixes and additional release items
- `release` — merging into this branch triggers appTester build and production artifacts; PRs come from `x.x_pre_release`

## Commit Messages

Format: `AND-xxx Description`

- Start with the Jira task number (AND-xxx)
- Followed by a space and a short description in English
- Example: `[REDACTED_TASK_KEY] Finalize CryptoCurrencyBalanceFetcher refactoring`