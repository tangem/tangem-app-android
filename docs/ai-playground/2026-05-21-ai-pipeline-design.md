# AI-Pipeline для автоматизации простых задач — Design Spec


---

## 1. Контекст и проблема

В команде есть набор повторяющихся, «механических» задач, которые AI способен выполнять автономно: фиксы Crashlytics-крашей, чистка релизнутых feature toggles, синхронизация документации, добавление feature toggles по шаблону, простой UI по Figma, mechanical refactor'ы, и т.п.

Цель фичи — автоматизировать такие задачи через интеграцию **Jira + GitHub Actions + Claude Code**, опираясь на пул специализированных агентов (см. связанный документ). Разработчик помечает задачу тегом `ai-pipeline` и перемещает в `In Progress` — дальше pipeline сам:

1. Запускает Claude Code с контекстом задачи
2. Через `tangem-orchestrator` определяет, какие репозитории и агенты нужны
3. Выполняет работу, создаёт ветку, открывает PR, обновляет Jira

Если требуется доработка — комментарий в Jira или в PR триггерит follow-up pipeline, который патчит ту же ветку.

## 2. Цели и не-цели

### Цели

- **G1.** Автоматизировать запуск Claude Code на простой задаче по тегу `ai-pipeline` в Jira
- **G2.** Поддерживать **single-repo и cross-repo** задачи через auto-detect (Claude/orchestrator сам определяет нужные репо)
- **G3.** Follow-up через комментарии (Jira или GitHub PR) — патч той же ветки, тот же PR
- **G4.** Stateless архитектура: связь Jira ↔ branch ↔ PR через naming convention, без внешнего хранилища
- **G5.** Безопасность: refuse в red zone (crypto, NFC, key storage, payments), отказ с понятным комментарием
- **G6.** Reviewers PR назначаются автоматически: assignee Jira-задачи + GitHub CODEOWNERS

### Не-цели

- Не пишем универсальный AI-агент для любых задач — only для категорий из zone-карты (§10)
- Не делаем дашборд/UI поверх pipeline'а — наблюдаемость только через GitHub Actions UI и Jira-комментарии
- Не интегрируемся с метриками (Datadog/Grafana) — это можно нарастить позже
- Не делаем budget enforcement (cost-cap по $) на старте — только timeout hard stop

## 3. High-level архитектура

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Jira: AND-XXXX                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ Label: ai-pipeline                                                    │  │
│  │ Status: To Do → In Progress           ──┐                             │  │
│  │ Comments: ...                          │ или новый comment           │  │
│  └────────────────────────────────────────┼──────────────────────────────┘  │
│                                            │                                 │
│                                            │ Automation Rules                │
│                                            │ (Jira)                          │
│                                            ▼                                 │
│                                  POST /repos/.../dispatches                  │
│                                  event_type:                                 │
│                                  • ai-task-start                             │
│                                  • ai-task-comment                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                            │
                                            ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  GitHub: tangem-app (entry point)                                            │
│  .github/workflows/                                                          │
│  ┌─────────────────────────────────┐     ┌────────────────────────────────┐ │
│  │  ai-pipeline-initial.yml        │     │  ai-pipeline-followup.yml      │ │
│  │  on: repository_dispatch        │     │  on: repository_dispatch       │ │
│  │      types: [ai-task-start]     │     │      types: [ai-task-comment]  │ │
│  │                                 │     │  on: issue_comment             │ │
│  │                                 │     │      (PR comments)             │ │
│  └─────────────────────────────────┘     └────────────────────────────────┘ │
│                  │                                       │                   │
│                  ▼                                       ▼                   │
│         Checkout app + submodule (tangem-claude-agents)                      │
│         git submodule update --remote (свежие агенты)                        │
│         Получить контекст Jira (gajira-get)                                  │
│                  │                                       │                   │
│                  ▼                                       ▼                   │
│         Запустить Claude Code (headless)                                     │
│         с системой агентов из .claude/agents/                                │
│         orchestrator решает: какие репо нужны, какие L3-агенты звать         │
│                  │                                       │                   │
│                  ▼                                       ▼                   │
│         Создать ветку feature/AND-XXXX_AI_<short>                            │
│         (в нужных репо)                                                      │
│         Push, открыть PR                Найти существующий PR,               │
│                                         force push с правками                │
│                  │                                       │                   │
│                  ▼                                       ▼                   │
│         Jira: In Progress → In Review   PR: комментарий со сводкой           │
│         gajira-transition + gajira-comment                                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 4. Trigger flow

### 4.1. Initial trigger (`ai-task-start`)



- **When:** Issue Transitioned → To `In Progress`
- **If:** Issue has label `ai-pipeline`
- **And:** Issue has NO label `ai-pipeline-active` (защита от повторного запуска)
- **Then:**
  1. Add label `ai-pipeline-active` (lock)
  2. Send web request:
     ```
     POST https://api.github.com/repos/tangem-developments/tangem-app/dispatches
     Authorization: Bearer ${AI_BOT_GH_TOKEN}
     Content-Type: application/json
     {
       "event_type": "ai-task-start",
       "client_payload": {
         "ticket_id": "{{issue.key}}",
         "ticket_url": "{{issue.url}}",
         "summary": "{{issue.summary}}",
         "assignee_email": "{{issue.assignee.emailAddress}}"
       }
     }
     ```

GitHub Action подтягивает полный контекст задачи через Jira API (description, AC, labels, comments, attachments, linked tickets), не полагаясь на webhook payload — это надёжнее.

### 4.2. Follow-up trigger (`ai-task-comment`)



- **When:** Issue Commented
- **If:** Issue has label `ai-pipeline` AND status = `In Review`
- **And:** Comment author is NOT the AI-bot
- **Then:** POST `repository_dispatch` с event_type `ai-task-comment`, payload включает:
  ```json
  {
    "ticket_id": "{{issue.key}}",
    "comment_body": "{{comment.body}}",
    "comment_author": "{{comment.author.displayName}}",
    "source": "jira"
  }
  ```



```yaml
on:
  issue_comment:
    types: [created]
```

В первом step фильтр: `if: github.event.issue.pull_request && startsWith(github.event.issue.head.ref, 'feature/AND-') && contains(github.event.issue.head.ref, '_AI_')`. И ещё фильтр: автор комментария ≠ `tangem-ai-bot`.

В обоих случаях (Jira и PR comment) дальше работает один и тот же workflow — он лишь различает источник через payload/event.

## 5. Pipeline 1: Initial — детальные шаги

`tangem-app/.github/workflows/ai-pipeline-initial.yml`:

```yaml
name: AI Pipeline — Initial

on:
  repository_dispatch:
    types: [ai-task-start]

concurrency:
  group: ai-pipeline-${{ github.event.client_payload.ticket_id }}
  cancel-in-progress: false   # ждать предыдущий запуск, не отменять

jobs:
  run:
    runs-on: ubuntu-latest
    timeout-minutes: 30   # hard timeout (см. Q6.3 → A: hard stop)

    steps:
      # 1. Checkout app + agents submodule
      - uses: actions/checkout@v4
        with:
          submodules: recursive
          token: ${{ secrets.AI_BOT_GH_TOKEN }}
          fetch-depth: 0   # для нормального git log/diff

      - name: Refresh agents submodule to latest main
        run: git submodule update --remote tangem-claude-agents

      # 2. Получить контекст Jira-задачи
      - name: Fetch Jira issue context
        id: jira
        uses: atlassian/gajira-find-issue-key@v3
        with:
          string: ${{ github.event.client_payload.ticket_id }}

      - name: Get Jira full issue
        id: jira_full
        env:
          JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}
          JIRA_USER_EMAIL: ${{ secrets.JIRA_USER_EMAIL }}
          JIRA_API_TOKEN: ${{ secrets.JIRA_API_TOKEN }}
        run: |
          # GET /rest/api/3/issue/${ticket_id}?expand=renderedFields,names,attachment
          # Сохранить description, AC, labels, comments, attachments, linked tickets в файл
          ./.github/scripts/fetch-jira-issue.sh \
            "${{ github.event.client_payload.ticket_id }}" \
            > /tmp/jira-issue.json

      # 3. Запустить Claude Code в headless mode
      - name: Run Claude Code
        id: claude
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
          AI_BOT_GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
          JIRA_CONTEXT_FILE: /tmp/jira-issue.json
        run: |
          # См. open question Q-RUNTIME (§17): один из вариантов claude-code-action@v1, CLI, Agent SDK
          # Промпт: вход через tangem-orchestrator
          # Команда orchestrator: «Выполни задачу из /tmp/jira-issue.json, используй кластер агентов»
          ./.github/scripts/run-claude-initial.sh

      # 4. Обработка результата Claude
      - name: Process Claude result
        id: result
        run: |
          # Claude пишет результат в /tmp/ai-result.json:
          # { "status": "completed" | "refused" | "incomplete", "branch": "...", "repos": ["app","bsdk"], "summary": "..." }
          STATUS=$(jq -r '.status' /tmp/ai-result.json)
          echo "status=$STATUS" >> $GITHUB_OUTPUT

      # 5a. Если completed — push + PR
      - name: Push branches and create PRs
        if: steps.result.outputs.status == 'completed'
        env:
          GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
        run: |
          ./.github/scripts/push-and-create-prs.sh

      # 5b. Транзишн Jira в In Review
      - name: Move Jira to In Review
        if: steps.result.outputs.status == 'completed'
        uses: atlassian/gajira-transition@v3
        with:
          issue: ${{ github.event.client_payload.ticket_id }}
          transition: "In Review"
        env:
          JIRA_BASE_URL: ${{ secrets.JIRA_BASE_URL }}
          JIRA_USER_EMAIL: ${{ secrets.JIRA_USER_EMAIL }}
          JIRA_API_TOKEN: ${{ secrets.JIRA_API_TOKEN }}

      - name: Comment Jira with PR links
        if: steps.result.outputs.status == 'completed'
        uses: atlassian/gajira-comment@v3
        with:
          issue: ${{ github.event.client_payload.ticket_id }}
          comment: |
            🤖 AI-pipeline завершил задачу. Pull Request(s):
            ${{ steps.result.outputs.pr_urls }}
            
            Reviewers: assignee + CODEOWNERS назначены.

      # 6a. Если refused — Jira → To Do + комментарий + снять label ai-pipeline
      - name: Handle refusal
        if: steps.result.outputs.status == 'refused'
        uses: atlassian/gajira-transition@v3
        with:
          issue: ${{ github.event.client_payload.ticket_id }}
          transition: "To Do"
        env:
          # ...

      - name: Comment Jira with refusal reason
        if: steps.result.outputs.status == 'refused'
        uses: atlassian/gajira-comment@v3
        with:
          issue: ${{ github.event.client_payload.ticket_id }}
          comment: |
            🤖 AI-pipeline отказался от задачи.
            Причина: ${{ steps.result.outputs.reason }}
            
            Требуется human-in-loop. Снят label `ai-pipeline`.

      - name: Remove ai-pipeline labels
        if: steps.result.outputs.status == 'refused'
        env:
          # ...
        run: |
          ./.github/scripts/remove-jira-labels.sh \
            "${{ github.event.client_payload.ticket_id }}" \
            "ai-pipeline" "ai-pipeline-active"

      # 6b. Если incomplete (cost/time превышен) — draft PR + Jira comment
      - name: Handle incomplete
        if: steps.result.outputs.status == 'incomplete'
        env:
          GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
        run: |
          ./.github/scripts/push-and-create-draft-pr.sh

      - name: Comment Jira about incomplete
        if: steps.result.outputs.status == 'incomplete'
        uses: atlassian/gajira-comment@v3
        with:
          # ... draft PR url, объяснение ...

      # 7. Всегда снять lock-label ai-pipeline-active в конце
      - name: Always unlock
        if: always()
        env:
          # ...
        run: |
          ./.github/scripts/remove-jira-labels.sh \
            "${{ github.event.client_payload.ticket_id }}" \
            "ai-pipeline-active"
```

### Подробности шага «Run Claude Code» (3)

Промпт-шаблон для orchestrator:

```
Тебе передана Jira-задача в /tmp/jira-issue.json (поля: title, description, AC,
labels, comments, attachments, linked_tickets).

Твоя задача:
1. Классифицировать задачу: green-zone / yellow-zone / red-zone (см. zone-карту).
2. Если red-zone → выйти с status=refused, объяснить причину в /tmp/ai-result.json.
3. Определить целевые репо (auto-detect):
   - tangem-app — уже в workspace (cwd)
   - blockchain-sdk-kotlin — клонировать в /tmp/bsdk если нужен
   - tangem-sdk-android — клонировать в /tmp/card-sdk если нужен
   - tangem-hot-sdk-kotlin — клонировать в /tmp/hot-sdk если нужен
4. Создать ветку feature/{TICKET_ID}_AI_{short_desc} в каждом нужном репо.
5. Делегировать соответствующим L3/L2 агентам.
6. После завершения работы — гонять тесты (./gradlew unitTest, detekt).
7. Если тесты упали — 1-2 итерации фикса. Если не помогло → status=completed, но PR пометить `requires-human-fix`.
8. Записать результат в /tmp/ai-result.json:
   {
     "status": "completed" | "refused" | "incomplete",
     "reason": "...",                       // для refused/incomplete
     "branch": "feature/{TICKET_ID}_AI_...",
     "repos": ["tangem-app", "blockchain-sdk-kotlin"],  // где созданы ветки
     "summary": "...",                      // краткое описание для Jira/PR
     "pr_descriptions": { "tangem-app": "...", "blockchain-sdk-kotlin": "..." }
   }
```

## 6. Pipeline 2: Follow-up — детальные шаги

`tangem-app/.github/workflows/ai-pipeline-followup.yml`:

```yaml
name: AI Pipeline — Follow-up

on:
  repository_dispatch:
    types: [ai-task-comment]
  issue_comment:
    types: [created]

concurrency:
  group: ai-pipeline-${{ github.event.client_payload.ticket_id || github.event.issue.number }}
  cancel-in-progress: false

jobs:
  run:
    if: |
      (github.event_name == 'repository_dispatch') ||
      (github.event_name == 'issue_comment' &&
       github.event.issue.pull_request != null &&
       github.event.comment.user.login != 'tangem-ai-bot')
    runs-on: ubuntu-latest
    timeout-minutes: 20

    steps:
      # 1. Нормализовать вход (Jira-comment или GitHub-comment) → переменные
      # Для issue_comment branch нужно получить через GitHub API,
      # т.к. в payload issue_comment поле head.ref отсутствует.
      - name: Resolve ticket and source
        id: ctx
        env:
          GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
        run: |
          if [ "${{ github.event_name }}" = "repository_dispatch" ]; then
            echo "ticket_id=${{ github.event.client_payload.ticket_id }}" >> $GITHUB_OUTPUT
            echo "comment_body=${{ github.event.client_payload.comment_body }}" >> $GITHUB_OUTPUT
            echo "source=jira" >> $GITHUB_OUTPUT
          else
            # Получаем head.ref через API, парсим ticket_id
            PR_NUMBER="${{ github.event.issue.number }}"
            BRANCH=$(gh pr view "$PR_NUMBER" --json headRefName -q '.headRefName')
            if [[ ! "$BRANCH" =~ _AI_ ]]; then
              echo "Branch $BRANCH is not an AI branch — skip"
              exit 78  # neutral exit
            fi
            TICKET=$(echo "$BRANCH" | grep -oE 'AND-[0-9]+')
            echo "ticket_id=$TICKET" >> $GITHUB_OUTPUT
            echo "comment_body=${{ github.event.comment.body }}" >> $GITHUB_OUTPUT
            echo "source=github" >> $GITHUB_OUTPUT
          fi

      # 2. Checkout app + agents
      - uses: actions/checkout@v4
        with:
          submodules: recursive
          token: ${{ secrets.AI_BOT_GH_TOKEN }}
          fetch-depth: 0

      - run: git submodule update --remote tangem-claude-agents

      # 3. Найти существующую ветку и PR
      - name: Locate branch and PR
        id: locate
        env:
          GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
        run: |
          ./.github/scripts/locate-ai-branch.sh \
            "${{ steps.ctx.outputs.ticket_id }}" \
            > /tmp/locate.json
          # На выходе: { "branches": [...], "prs": [...], "repos": ["tangem-app", ...] }

      # 4. Получить полный контекст: Jira задача + диф PR + последние комментарии
      - name: Fetch context bundle
        run: |
          ./.github/scripts/fetch-followup-context.sh \
            "${{ steps.ctx.outputs.ticket_id }}" \
            "${{ steps.ctx.outputs.comment_body }}" \
            "${{ steps.ctx.outputs.source }}" \
            > /tmp/followup-context.json

      # 5. Запустить Claude
      - name: Run Claude Code (follow-up)
        env:
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
          AI_BOT_GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
          CONTEXT_FILE: /tmp/followup-context.json
        run: |
          ./.github/scripts/run-claude-followup.sh

      # 6. Обработать результат
      - name: Process result
        id: result
        run: |
          STATUS=$(jq -r '.status' /tmp/ai-result.json)
          echo "status=$STATUS" >> $GITHUB_OUTPUT
          # status: "patched" | "no_action" | "failed"

      # 7a. Если patched — force push, комментарий в PR + Jira
      - name: Force-push patches
        if: steps.result.outputs.status == 'patched'
        env:
          GH_TOKEN: ${{ secrets.AI_BOT_GH_TOKEN }}
        run: |
          ./.github/scripts/push-followup-patches.sh

      - name: Comment Jira about patch
        if: steps.result.outputs.status == 'patched'
        uses: atlassian/gajira-comment@v3
        with:
          # ...

      # 7b. Если no_action — Claude посчитал, что комментарий не запрос на доработку
      - name: Reply in source
        if: steps.result.outputs.status == 'no_action'
        run: |
          # Ответить в источнике (Jira или GitHub) текстом из ai-result.json
          ./.github/scripts/reply-no-action.sh

      # 7c. Если failed — лог + Jira-комментарий
      - name: Handle failure
        if: steps.result.outputs.status == 'failed'
        # ...
```

### Промпт для follow-up Claude

```
Тебе передан контекст в /tmp/followup-context.json:
- ticket: { id, title, description, ac, all_comments, labels }
- pr_state: { number, url, head, base, current_diff }
- trigger_comment: { body, author, source: jira|github }

Шаги:
1. Прочитать trigger_comment и решить: это запрос на доработку или обычная заметка/вопрос?
2. Если не запрос → status=no_action, в reply положить вежливый ответ автору в стиле «Я думаю, это просто обсуждение. Если ты хочешь, чтобы я что-то изменил — напиши явно "пожалуйста, обнови X"».
3. Если запрос → применить правки в существующих ветках (в всех релевантных репо). Использовать tangem-orchestrator → нужных L3.
4. Гонять тесты, фиксить если упали.
5. status=patched, в summary — что изменилось.
6. Записать в /tmp/ai-result.json.
```

## 7. Branch и PR naming

### Branch

Формат: `feature/AND-XXXX_AI_<short_description>`

- `AND-XXXX` — Jira ticket ID
- `_AI_` — маркер AI-ветки (для regex-фильтра в workflow)
- `<short_description>` — Claude генерирует слаг из summary, snake_case, ≤ 30 символов

Пример: `feature/AND-15500_AI_add_foochain_evm`

### PR

- **Title:** `AND-XXXX <Original task summary>` (формат проекта)
- **Body:** Markdown с разделами:
  - 🤖 AI Pipeline summary
  - Ссылка на Jira ticket
  - Что сделано (bulletpoint список от Claude)
  - Список проверок: тесты, Detekt
  - При красноте тестов — пометка `requires-human-fix`
- **Labels:** `ai-generated` (PR-side label, для фильтрации в GitHub UI)
- **Reviewers:** assignee из Jira + GitHub CODEOWNERS
- **Draft:** только если status=incomplete или есть `requires-human-fix`

## 8. State management

Полностью **stateless**, привязка через naming convention.

| Связь | Как находится |
|---|---|
| Ticket ID ↔ branch | Regex `feature/(AND-\d+)_AI_.*` |
| Branch ↔ PR | `gh pr list --head <branch> --state open --json url,number` |
| Ticket ↔ repos | Имя ветки одинаковое во всех релевантных репо; pipeline ищет в каждом из 4 |

В Jira ничего custom не хранится. PR URL'ы попадают в комментарии Jira, но это для людей, а не для парсинга.

## 9. Доступы и секреты

### 9.1. GitHub Actions secrets (в `tangem-app`)

| Secret | Назначение | Значение / как получить |
|---|---|---|
| `ANTHROPIC_API_KEY` | Аутентификация Claude API | Получить на console.anthropic.com под бот-аккаунтом |
| `AI_BOT_GH_TOKEN` | PAT GitHub-бота | Создать в settings бот-аккаунта; scopes: `repo` (все 4 рабочих репо), `read:org`, `read:public_key`. Read доступ к `tangem-claude-agents` |
| `AI_BOT_GH_USER` | Username бота | `tangem-ai-bot` (создать, см. §9.4) |
| `JIRA_BASE_URL` | URL Jira instance | `https://tangem.atlassian.net` |
| `JIRA_USER_EMAIL` | Email бот-аккаунта Jira | `ai-bot@tangem.com` (создать) |
| `JIRA_API_TOKEN` | API token Jira-бота | id.atlassian.com → Manage account → Security → API tokens |
| `JIRA_PROJECT_KEY` | Проект в Jira | `AND` |

### 9.2. GitHub permissions for AI bot

Бот-аккаунт `tangem-ai-bot` должен быть:
- **Member** в `tangem-developments` org
- **Write** доступ на: `tangem-app`, `blockchain-sdk-kotlin`, `tangem-sdk-android`, `tangem-hot-sdk-kotlin`
- **Read** доступ на: `tangem-claude-agents`, `tangem-android-tools`

PAT с этими правами (Personal Access Token classic или Fine-grained):
- `repo` scope (полный)
- `workflow` scope (для запуска workflow с другого workflow, если понадобится)

Альтернатива — **GitHub App** вместо PAT (производительнее по rate-limit'ам, токены обновляются автоматически). Решение по этому — отдельный план реализации (см. open question Q-AUTH).

### 9.3. Jira permissions for AI bot

Бот-аккаунт `ai-bot@tangem.com` в Jira должен иметь:
- **Browse Project**, **Add Comments**, **Edit Issues**, **Transition Issues** для проекта `AND`
- **API access** включён

### 9.4. Создаваемые аккаунты

| Аккаунт | Где | Зачем |
|---|---|---|
| `tangem-ai-bot` (GitHub) | github.com | Идентичность для коммитов, PR, комментариев в PR |
| `ai-bot@tangem.com` (Jira) | tangem.atlassian.net | Идентичность для комментариев и transitions в Jira |

Email для GitHub-бота — например, `tangem-ai-bot@users.noreply.github.com` (стандартный noreply от GitHub) или тот же `ai-bot@tangem.com`. Имя коммитов: `Tangem AI Bot <noreply>`.

### 9.5. Jira Automation Rules

В Jira project settings → Automation создаются 2 правила:

**Rule 1 — Trigger initial:**
- Trigger: Issue Transitioned
- Conditions:
  - To status: `In Progress`
  - JQL: `labels = "ai-pipeline" AND labels != "ai-pipeline-active"`
- Actions:
  1. Edit issue → Add label `ai-pipeline-active`
  2. Send web request → POST `https://api.github.com/repos/tangem-developments/tangem-app/dispatches` (см. §4.1)

**Rule 2 — Trigger follow-up:**
- Trigger: Issue Commented
- Conditions:
  - JQL: `labels = "ai-pipeline" AND status = "In Review"`
  - Comment author ≠ `ai-bot@tangem.com`
- Actions:
  1. Send web request → POST `repository_dispatch` с event_type `ai-task-comment`

## 10. Категории задач (zone-карта)

### Green zone — выполнить автономно

Примеры: фиксы Crashlytics, чистка релизнутых feature toggles, sync CLAUDE.md, refresh api-docs, sync translations, добавление feature toggle / analytics event / currency icon, mechanical refactor (rename, deprecated → new API), простые UI по Figma с чёткими AC, add new blockchain в bsdk (по Assembly-шаблону).

### Yellow zone — выполнить, но draft PR + `requires-human-review`

Примеры: UI без чёткого AC, refactor > N файлов, миграции v1→v2 фичи, добавление API endpoint.

### Red zone — отказ + Jira-комментарий

Примеры:
- Архитектурные решения, новые DI scopes
- `tangem-sdk-android`: NFC-команды, CardSession-логика
- `tangem-hot-sdk-kotlin`: key storage, signing
- Cryptography: derivation, ECDSA, ED25519
- `libs/auth/`, `libs/visa/` (payments / smart contracts)
- Build system, CI/CD, release config


Точные критерии границ — см. **Q-CRITERIA** в §17.

## 11. Safety и error handling

| Сценарий | Поведение | Источник решения |
|---|---|---|
| Claude отказался (red zone / неясные AC) | Jira → `To Do`, комментарий с причиной, снять label `ai-pipeline` | Q6.1: A |
| Тесты упали | Pipeline даёт Claude 1-2 итерации фикса. Если не помогло → PR создаётся с пометкой `requires-human-fix`, Jira-комментарий | Q6.2: A модифицированный |
| Cost/timeout превышен | Hard stop, текущее состояние коммитится в **draft PR** с пометкой `incomplete`, Jira-комментарий | Q6.3: A |
| Бесконечный follow-up loop | Без лимитов; soft warning при N follow-up'ах в день (см. §13) | Q6.4: B |
| Конфликт concurrency (два дисptch'а на один ticket) | GitHub Actions `concurrency.group` по ticket_id, `cancel-in-progress: false` — ждать предыдущий | Решено в дизайне |
| Failure внутри pipeline (script error) | `if: always()` снимает `ai-pipeline-active` lock, оставляет Jira-комментарий с ссылкой на Actions run | Решено в дизайне |
| Подмена комментария (не от автора) | Filter: в Jira automation `comment.author ≠ ai-bot`; в GitHub `comment.user.login ≠ ai-bot` | Решено в дизайне |

## 12. Concurrency и rate limits

- **GitHub Actions concurrency:** `group: ai-pipeline-${ticket_id}`, `cancel-in-progress: false`. Параллельные запуски разных тикетов — OK. Параллельные запуски одного — сериализованы.
- **Jira-side lock:** label `ai-pipeline-active` ставится при старте initial, снимается в `always()`-шаге pipeline. Защищает от повторного триггера, если pipeline ещё идёт.
- **Anthropic API rate limit:** обрабатывается на стороне Claude Code (retry с backoff). При hard fail — pipeline идёт в `incomplete`.
- **Jira API rate limit:** `gajira-*` actions имеют ретраи; для custom scripts — wrapper с retry on 429.
- **GitHub API rate limit:** PAT даёт 5000 req/h; для бота этого хватает с запасом. При проблемах — мигрировать на GitHub App.

## 13. Связь с tangem-claude-agents

Pipeline опирается на систему агентов из связанного дизайна:

- `tangem-orchestrator` — точка входа Claude в pipeline'е
- L2 repo-experts (5 шт.) — общая структура соответствующего репо
- L3 specialists (30 шт.) — выполняют конкретные задачи по доменам/семействам
- Cross-cutting роли (7 шт.) — `tangem-bug-investigator` (для Crashlytics), `tangem-test-writer`, `tangem-code-reviewer` (для self-review перед PR), `tangem-ci-cd-expert` (если задача про CI)

Pipeline всегда:
1. Делает `git submodule update --remote tangem-claude-agents` для свежих агентов
2. Передаёт первый prompt именно `tangem-orchestrator`, не специалистам напрямую (он решает, кого звать)

## 14. Что нужно для реализации (чеклист)

### 14.1. Аккаунты и доступы

- [ ] Создать GitHub-аккаунт `tangem-ai-bot`, добавить в organization
- [ ] Дать ему write-доступ к `tangem-app`, `blockchain-sdk-kotlin`, `tangem-sdk-android`, `tangem-hot-sdk-kotlin`
- [ ] Дать read-доступ к `tangem-claude-agents`, `tangem-android-tools`
- [ ] Сгенерировать PAT (classic, scopes: `repo`, `workflow`, `read:org`)
- [ ] Создать Jira-аккаунт `ai-bot@tangem.com`
- [ ] Дать ему права Browse/Comment/Edit/Transition в проекте `AND`
- [ ] Сгенерировать Jira API token

### 14.2. Anthropic

- [ ] Получить `ANTHROPIC_API_KEY` под рабочую квоту (см. open question Q-COST)
- [ ] Решить вопрос billing/limit (см. open questions)

### 14.3. Jira

- [ ] Создать label `ai-pipeline` (управляемый, видимый команде)
- [ ] Создать label `ai-pipeline-active` (внутренний, для lock; не показывать в UI фильтрах, если возможно)
- [ ] Настроить Automation Rule «Trigger initial» (см. §9.5)
- [ ] Настроить Automation Rule «Trigger follow-up» (см. §9.5)
- [ ] Проверить, что у бота-аккаунта Jira есть права на edit labels и transitions

### 14.4. GitHub (в репо `tangem-app`)

- [ ] В Secrets and variables → Actions добавить все секреты из §9.1
- [ ] Создать `.github/workflows/ai-pipeline-initial.yml` (по §5)
- [ ] Создать `.github/workflows/ai-pipeline-followup.yml` (по §6)
- [ ] Создать вспомогательные скрипты в `.github/scripts/`:
  - `fetch-jira-issue.sh` — тянет полный issue из Jira API
  - `run-claude-initial.sh` — запускает Claude, формирует промпт
  - `run-claude-followup.sh` — то же для follow-up
  - `push-and-create-prs.sh` — push в нужные репо, `gh pr create`
  - `push-and-create-draft-pr.sh` — то же, но draft
  - `push-followup-patches.sh` — force-push в существующие ветки
  - `locate-ai-branch.sh` — ищет ветки `feature/AND-XXXX_AI_*` в 4 репо
  - `fetch-followup-context.sh` — собирает контекст для follow-up
  - `remove-jira-labels.sh` — снимает label через Jira API
  - `reply-no-action.sh` — пишет ответ автору комментария
- [ ] Решить вопрос runtime (см. open question Q-RUNTIME), реализовать соответствующий entry-point в `run-claude-*.sh`
- [ ] Добавить label `ai-generated` в репо (видим в PR list)

### 14.5. tangem-claude-agents

- [ ] Дизайн агентов из связанной спеки реализован и доступен
- [ ] `tangem-orchestrator` имеет в prompt полную zone-карту (green/yellow/red)
- [ ] `tangem-orchestrator` поддерживает auto-detect нужных репо по контексту задачи
- [ ] Скрипты setup-cross-repo.sh (для клонирования других репо в /tmp) реализованы как часть orchestrator workflow

### 14.6. CODEOWNERS

- [ ] Проверить, что `tangem-app/CODEOWNERS` есть и актуален. Если нет — создать
- [ ] Аналогично в bsdk, card-sdk, hot-sdk (если используются)

## 15. Что не входит в этот дизайн

- Финальный выбор runtime'а Claude (см. Q-RUNTIME)
- Точные критерии «годности» задачи (см. Q-CRITERIA)
- Лимиты cost / бюджет на Anthropic (см. Q-COST)
- Метрики наблюдаемости в Datadog/Grafana
- Дашборд статистики работы pipeline (только GitHub Actions UI и Jira-комментарии)
- Sandbox-окружение для тестового прогона перед мержем
- Авто-merge мерж стратегия для AI-PR (всегда требуется human-review)
- Локализованные ответы pipeline (английский/русский в комментариях)
- Поддержка задач в проектах Jira отличных от `AND`

## 16. Риски и митигации

| Риск | Митигация |
|---|---|
| Claude портит код в red zone | Refuse-list в orchestrator prompt; pre-check Jira labels на red-zone (`area:crypto`, `area:nfc` и т.п.); pre-check затронутых путей |
| Cost-runaway (Claude съел много токенов) | Hard timeout `timeout-minutes: 30`/`20` на pipeline-уровне; soft-monitoring через Anthropic usage dashboard |
| Бесконечные follow-up'ы | Без жёстких лимитов (Q6.4: B), но в команде договорённость: после 5 итераций — поручить человеку; soft-warning через Jira comment на 5-й итерации |
| Race condition: два comment'а одновременно | GH Actions `concurrency.group` по ticket_id |
| Чужие секреты в логах | `secrets.X` автоматически маскируются; ни один script не должен делать `echo` payload в plain |
| Зловредный комментарий («забей всё» в Jira) | Filter источника: только из specific groups Jira/GitHub teams; явный AI-bot filter в Jira rule |
| Сломанная связь Jira ↔ branch | Stateless + `gh pr list` всегда даёт актуальное состояние; если ветка не найдена — pipeline ответит «не нашёл ветку, создаю новую?» в комментарии |
| Webhook от Jira пропал | Auto-retry на стороне Jira automation (3-кратно); если всё равно фейл — manual restart через `Run workflow` в Actions UI |
| Hijack PAT бота | PAT хранится в Actions secrets; rotation раз в N месяцев; масштабы прав ограничены до 4 репо |

## 17. Open questions

### Q-RUNTIME: какой Claude Code runtime в pipeline?

| Вариант | Плюсы | Минусы |
|---|---|---|
| `anthropics/claude-code-action@v1` | Официальный, headless из коробки, видит `.claude/agents/`, MCP, skills | Зависимость от стороннего action; меньше контроля над промптом и стадиями |
| Claude Code CLI вручную (npm package) | Полный контроль, простой install | Поддерживать install steps, парсить output вручную |
| Claude Agent SDK (Python/TS) | Programmatic control над циклом «думай → проверь → доработай»; legible logs | Избыточен для текущего скоупа; пишем больше кода |
| Hybrid | Лучшее из миров | Сложнее ментально |


### Q-CRITERIA: точные критерии «годности» задачи

Что pipeline проверяет до делегирования Claude? Возможные критерии:

- Наличие непустого description в Jira
- Наличие AC (либо в отдельном поле, либо в description с ключевыми словами «AC:» / «Acceptance Criteria»)
- Для UI задач — наличие Figma URL в attachments
- Для bug fix — наличие Crashlytics URL / stack trace в description
- Лимит на «диапазон». Что считать максимумом — ?
- Список модулей-исключений (red zone) на уровне pre-check

Финальный список — обсудим отдельно.

### Q-COST: бюджет на Anthropic API

- Есть ли месячный лимит $X на AI-pipeline?
- Нужна ли pre-flight оценка стоимости задачи?
- Лимит токенов на одну задачу?

### Q-AUTH: PAT vs GitHub App

PAT classic простой, но имеет ограниченный lifetime и rate-limit. GitHub App даёт более высокий rate-limit, ротацию токенов автоматически. Решение — в плане реализации, на старте PAT норм.

### Q-METRICS: наблюдаемость

Нужны ли dashboard с метриками: количество запусков, success rate, средняя длительность, стоимость? На старте — нет (наблюдаемость через GitHub Actions UI + Jira-комментарии). При масштабировании — добавить.

### Q-LANG: язык комментариев pipeline

Pipeline пишет комментарии в Jira/PR. На каком языке: всегда английский (стандарт коммитов и PR в проекте), или русский (язык команды)? Решение — отдельно.