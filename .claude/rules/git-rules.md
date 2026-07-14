# Git Rules

## Branch Naming

| Type    | Format                              | Example                             |
|---------|-------------------------------------|-------------------------------------|
| Feature   | `feature/AND-xxx_short_description` | `feature/AND-13391_balance_fetcher` |
| Bugfix    | `bugfix/AND-xxx_short_description`  | `bugfix/AND-14000_fix_crash`        |
| Technical | `tech/short_description`            | `tech/update_ci_scripts`            |
| Release   | `releases/x.xx`                     | `releases/5.36`                     |
| Hotfix    | `releases/x.xx.x`                   | `releases/5.36.1`                   |

**Technical (`tech/`) branches** are for chore / tooling work with **no Jira task** — CI, scripts,
build/config, docs, repo tooling. They carry **no `AND-xxx`** in the branch name, commit subject, or
PR title.

**Key branches:**

- `develop` — main integration branch, all feature/bugfix branches merge here
- `releases/x.xx` — release branches, branched from `develop`
- `releases/x.xx.x` — hotfix branches, branched from `releases/x.xx`

## Commit Messages

Format: `AND-xxx Description`

- Start with the Jira task number (AND-xxx)
- Followed by a space and a short description in English
- Example: `[REDACTED_TASK_KEY] Finalize CryptoCurrencyBalanceFetcher refactoring`
- **Technical (`tech/`) branches** have no Jira task, so their commit subject (and PR title) is just
  the English description, with **no `AND-xxx` prefix** — e.g. `Update CI scripts`.