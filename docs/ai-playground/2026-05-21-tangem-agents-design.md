# Tangem Claude Code Agents — Design Spec


---

## 1. Контекст и проблема

Проект Tangem распределён по 5 репозиториям:

- `tangem-app` — основное Android-приложение (~220 модулей)
- `blockchain-sdk-kotlin` — multichain SDK (~48 блокчейнов)
- `tangem-sdk-android` — Card SDK для NFC (42 NFC-команды)
- `tangem-hot-sdk-kotlin` — hot wallet SDK (AES-256, KeyStore, trezor-crypto)
- `tangem-android-tools` — Detekt rules и tooling

Типовые задачи часто затрагивают **2+ репо**: app + bsdk, app + hot-sdk, app + card-sdk. Хочется набор Claude Code-агентов, который:

1. Покрывает весь скоуп проекта без «слепых зон»
2. Доступен из любого репо, как «команда сотрудников, которую можно позвать на помощь»
3. Версионируется и расшаривается между разработчиками
4. Работает в автоматизированных Claude-задачах на CI


## 2. Цели и не-цели

### Цели

- **G1.** Один общий пул из ~40 агентов, доступный одинаково во всех 5 репо
- **G2.** Иерархия: orchestrator → repo-эксперты → узкие специалисты → cross-cutting роли
- **G3.** PR-флоу для изменений агентов без блокирующих зависимостей между PR
- **G4.** Простой setup на новой машине и на CI (idempotent, через стандартные git-операции)
- **G5.** Покрытие критичных областей: фичи app по кластерам, семейства блокчейнов в bsdk, NFC/backup/crypto в card-sdk, key storage/signing в hot-sdk
- **G6.** Cross-cutting роли для типового workflow: bug investigation, code review, test writing, security, UI implementation, CI/CD, Figma extraction

### Не-цели

- Не покрываем каждую фичу отдельным агентом (это 200+ агентов, неподдерживаемо) — гранулярность по кластерам
- Не делаем агента для каждого блокчейна (48 шт.) — группируем по семействам
- Не дублируем функциональность существующих skills (`analyze-logs`, `fix-crashlytics`, `cleanup-feature-toggles` и т.д.); агенты их вызывают
- Не пишем агентов под Visa, KYC, NFT и подобные «маленькие» фичи как отдельные — они уходят в кластерные L3
- Не блокируем рабочий процесс ради воспроизводимости версий агентов (агенты — это инструкции, а не код)

## 3. Архитектура

### 3.1. Размещение: floating submodule

Создаётся **новый репозиторий** `tangem-developments/tangem-claude-agents`.

В каждом из 4 рабочих репо (`tangem-app`, `blockchain-sdk-kotlin`, `tangem-sdk-android`, `tangem-hot-sdk-kotlin`) подключается как submodule **с отслеживанием ветки `main`**:

```ini
# .gitmodules
[submodule "tangem-claude-agents"]
    path = tangem-claude-agents
    url = git@github.com:tangem-developments/tangem-claude-agents.git
    branch = main
```

Ключевое отличие от обычного submodule — наличие `branch = main`. Это позволяет командой `git submodule update --remote` подтягивать **последний коммит main**, не привязываясь к зафиксированному pointer в parent-репо.

### 3.2. Symlinks

В каждом из 4 репо `.claude/agents` — symlink на папку внутри submodule:

```
<repo>/
├── tangem-claude-agents/         ← submodule (одинаково везде)
│   └── agents/                   ← реальные .md-файлы
└── .claude/
    └── agents → ../tangem-claude-agents/agents   ← symlink (относительный путь)
```

Claude при запуске в любом из 4 репо видит `.claude/agents/` как обычную папку (symlink прозрачен для ОС) и подгружает агентов.


### 3.3. Структура `tangem-claude-agents`

```
tangem-claude-agents/
├── agents/
│   ├── orchestrator.md                  # L1
│   ├── _shared/                         # cross-cutting (7)
│   │   ├── bug-investigator.md
│   │   ├── code-reviewer.md
│   │   ├── test-writer.md
│   │   ├── security-auditor.md
│   │   ├── figma-extractor.md
│   │   ├── ui-implementer.md
│   │   └── ci-cd-expert.md
│   ├── app/                             # L2 + L3 для app (1 + 9)
│   │   ├── repo-expert.md               # L2 (единственный в L2)
│   │   ├── portfolio-expert.md          # включает markets и nft
│   │   ├── send-swap-expert.md
│   │   ├── staking-earn-expert.md
│   │   ├── onramp-expert.md
│   │   ├── wallet-mgmt-expert.md        # включает card-scan
│   │   ├── onboarding-expert.md
│   │   ├── walletconnect-expert.md
│   │   ├── visa-expert.md
│   │   └── platform-expert.md
│   ├── bsdk/                            # L3 для bsdk (9)
│   │   ├── evm-expert.md
│   │   ├── utxo-expert.md
│   │   ├── cosmos-expert.md
│   │   ├── solana-expert.md
│   │   ├── custom-l1-expert.md          # включает substrate
│   │   ├── network-provider-expert.md   # включает pendingtx, assetsdiscovery
│   │   ├── txhistory-expert.md
│   │   ├── nft-expert.md
│   │   └── yieldsupply-expert.md
│   ├── card-sdk/                        # L3 для card-sdk (3)
│   │   ├── nfc-commands-expert.md
│   │   ├── backup-expert.md
│   │   └── crypto-expert.md
│   └── hot-sdk/                         # L3 для hot-sdk (3)
│       ├── wallet-creation-expert.md
│       ├── key-storage-expert.md
│       └── signing-expert.md
├── .github/workflows/validate.yml       # CI на самом agents-репо (опционально)
└── README.md                            # setup и conventions
```

Группировка папок по репо — для удобства навигации и владения. **Claude видит плоский список агентов** (по полному `name` из frontmatter), подпапки в имени Task tool не фигурируют.


## 4. Состав агентов

Полный реестр всех агентов хранится в `docs/tangem-agents.csv` (для Notion-импорта). Сводная таблица:

| Слой | Количество | Назначение |
|---|---|---|
| **L1 Orchestrator** | 1 | Тимлид: классификация задач и делегирование |
| **L2 Repo experts** | 1 | Только `app-repo-expert` (общая структура 220-модульного app) |
| **L3 app specialists** | 9 | portfolio, send-swap, staking-earn, onramp, wallet-mgmt, onboarding, walletconnect, visa, platform |
| **L3 bsdk specialists** | 9 | Семейства блокчейнов (5) + сквозные подсистемы (4): EVM, UTXO, Cosmos, Solana, Custom-L1; network-provider, txhistory, nft, yieldsupply |
| **L3 card-sdk specialists** | 3 | nfc-commands, backup, crypto |
| **L3 hot-sdk specialists** | 3 | wallet-creation, key-storage, signing |
| **Cross-cutting roles** | 7 | bug-investigator, code-reviewer, test-writer, security-auditor, figma-extractor, ui-implementer, ci-cd-expert |
| **Итого** | **33** | |

Подробные описания каждого агента — см. `docs/tangem-agents.csv`.

### Объединённые зоны ответственности (для понимания scope L3)

| Агент | Включает |
|---|---|
| `app-portfolio-expert` | features: home, wallet, manage-tokens, tokendetails, txhistory, token-receive, **markets, news, nft**; соответствующие domain |
| `app-wallet-mgmt-expert` | features: wallet-settings, account, hot-wallet, biometry, **card-scan (ScanCardProcessor, scanCard chain)**; соответствующие domain |
| `bsdk-custom-l1-expert` | Cardano, XRP, TON, Stellar, Tron, Aptos, Sui, Algorand, Kaspa, Filecoin, Hedera, Chia, Vechain, Quai, Casper, Alephium, Koinos, Nexa, Ducatus, Fact0rn, **Polkadot, Kusama** (substrate) |
| `bsdk-network-provider-expert` | MultiNetworkProvider, JSON-RPC, BlockBook, BlockChair, Electrum, retry/timeouts, **pending tx polling**, **assets discovery** |
| `tangem-ci-cd-expert` | Pipeline-конфиги: gradle build types/flavors, release branches, secrets, marathon UI tests, fastlane/GitHub Actions, **Detekt конфигурация и кастомные правила в tangem-android-tools** |

## 5. Протокол делегирования

### 5.1. Маршрутизация задач

```
Пользователь
    │
    ▼
tangem-orchestrator  ← входная точка (через slash-команду или явный вызов)
    │
    ├──► L3 specialist        (для понятных задач: «добавь Foochain» → bsdk-evm-expert)
    │
    ├──► L2 repo-expert       (когда нужен общий контекст репо: «рефактор navigation root»)
    │
    └──► параллельно несколько L3  (cross-repo задачи: app + bsdk одновременно)
    
L3 specialist
    │
    ├──► cross-cutting role     (специалист зовёт code-reviewer или test-writer)
    │
    └──► (никогда напрямую в другой L3 — только через orchestrator)
```

### 5.2. Правила

- **L3 ↔ L3 не общаются напрямую.** Только через orchestrator, чтобы избежать кольцевых вызовов и потери контекста.
- **L3 ↔ cross-cutting роли — можно напрямую.** Специалист в любой момент может попросить ревью или тесты у соответствующей роли.
- **Orchestrator распараллеливает независимые подзадачи** через множественные `Task` вызовы в одном сообщении.
- **Каждый специалист получает абсолютный путь к своему репо в prompt** при вызове. Если репо не в workspace — агент возвращает сообщение «нужен `/add-dir <path>`».
- **L3 не пишут тесты.** L3 реализует код (имплементация, базовые `Detekt`/unit sanity-проверки). Финальные тесты пишет `tangem-test-writer` отдельной фазой.
- **L3 не делает финальный self-review.** Перед PR Claude вызывает `tangem-code-reviewer` (Detekt + проектные паттерны: Decompose, Either, drawable naming, UnsafeStringResourceUsage).
- **`app-platform-expert` владеет core/*** (analytics, feature-toggles, navigation root, DI, settings). Другие L3 не трогают core напрямую — делегируют platform-эксперту.

### 5.3. Таблица маршрутизации (для orchestrator-prompt)

Чтобы избежать путаницы со встроенными агентами Claude Code (есть похожие имена!), orchestrator **обязан вызывать наших агентов по точному имени**. Таблица в его prompt:

| Тип задачи / триггер | Точное имя агента (Task tool) | НЕ использовать |
|---|---|---|
| Code review перед PR | `tangem-code-reviewer` | `code-reviewer` |
| Security audit | `tangem-security-auditor` | `security-auditor` |
| Bug investigation (Crashlytics, краши) | `tangem-bug-investigator` | `debugger` |
| Написать тесты | `tangem-test-writer` | — |
| Реализовать Compose-UI по дизайну | `tangem-ui-implementer` (использует skill `figma:figma-implement-design` внутри) | прямо вызывать skill |
| Извлечь Figma `.fig` → JSON | `tangem-figma-extractor` (использует skill `figma-extract`) | прямо вызывать skill |
| CI/CD, gradle, fastlane, GitHub Actions | `tangem-ci-cd-expert` | — |
| Кодбейс explore (открытый поиск) | `Explore` (встроенный — OK) | — |
| Высокоуровневый план | `Plan` (встроенный — OK) или `tangem-orchestrator` | — |
| EVM-блокчейн в bsdk | `bsdk-evm-expert` | `blockchain-developer` |
| Любой L3-специалист | его точное `name` из реестра | общие mobile/kotlin/blockchain агенты |

### 5.4. Конвенция вызовов

При делегировании Task tool обязательно указывается:

1. **Абсолютный путь к репо**, в котором работать
2. **Краткое описание задачи** (без избыточного контекста)
3. **Ожидаемый формат результата** (summary, diff-references, todo list, и т.п.)
4. **Связанные агенты**, если задача может потребовать дополнительной экспертизы

## 6. Структура отдельного агента

Каждый агент — отдельный `.md` файл с YAML frontmatter:

```markdown
---
name: app-portfolio-expert
description: Эксперт по фичам портфолио и токенов в tangem-app. Использовать при работе с home, wallet, manage-tokens, tokendetails, txhistory, NFT-получением, domain tokens/tokensync.
model: sonnet
---

# Зона ответственности

Feature-модули в репозитории tangem-app:
- features:home, features:wallet, features:manage-tokens
- features:tokendetails, features:txhistory, features:token-recieve

Domain-модули:
- domain:tokens, domain:tokensync, domain:manage-tokens, domain:assetsdiscovery

# Абсолютные пути

- Репо: /Users/nzeeei/android/tangem-app (default; может быть переопределён в prompt при вызове)
- При cross-repo задаче дополнительно может потребоваться:
  - bsdk: /Users/nzeeei/android/blockchain-sdk-kotlin

Перед началом работы проверь, что репо доступен в workspace. Если нет — попроси `/add-dir <path>`.

# Ключевые паттерны

- Decompose: API/Impl split, Default*Component, *Model, *UM, ChildStack/ChildSlot
- Hilt: @ModelScoped для моделей, @SingletonComponent для остального
- Either<Error, Success> через Arrow
- StateFlow + collectAsStateWithLifecycle()
- Suppliers / Fetchers (см. domain/CLAUDE.md)

(полный список паттернов прорабатывается в плане реализации)

# Когда передавать другому агенту (обязательная секция)

- Тесты для написанного кода → `tangem-test-writer` (L3 тесты не пишет)
- Финальный self-review перед PR → `tangem-code-reviewer`
- Security-аспекты → `tangem-security-auditor`
- Изменения в core/* (analytics, feature-toggles, navigation, DI) → `app-platform-expert`
- Изменения в blockchain-sdk-kotlin → соответствующий `bsdk-*-expert` через orchestrator
- UI по Figma → `tangem-ui-implementer`
```


### 6.1. Модель

- **`model: sonnet`** — по умолчанию для всех L2/L3/roles
- **`model: opus`** — для `tangem-orchestrator` (сложная классификация) и `tangem-security-auditor` (критичность)
- **`model: haiku`** — для простых ролей вроде `tangem-figma-extractor` (если применимо)

### 6.2. Frontmatter обязательные поля

| Поле | Назначение |
|---|---|
| `name` | Уникальный плоский идентификатор, kebab-case |
| `description` | Однострочно: когда использовать (это видит orchestrator при выборе) |
| `model` | sonnet / opus / haiku |

## 7. Конфликт-стратегия

В окружении Claude Code присутствуют:

- **Встроенные субагенты** Claude Code: `code-reviewer`, `security-auditor`, `debugger`, `kotlin-specialist`, `mobile-app-developer`, `mobile-developer`, `blockchain-developer`, `agent-organizer`, `multi-agent-coordinator` и др.
- **Сторонние плагины**: figma, Notion, atlassian и т.д.

Наши агенты пересекаются с ними по именам и зонам. Чтобы Claude всегда выбирал нашего агента, применяются две меры.

### 7.1. Префиксы и явные descriptions

- Все наши агенты префиксированы:
  - `tangem-*` — cross-cutting роли и orchestrator
  - `app-*`, `bsdk-*`, `card-*`, `hot-*` — repo-experts и L3 specialists
- В поле `description` каждого агента **обязательная фраза, явно ограничивающая зону**: «для проекта Tangem Android, опирается на структуру модулей, Detekt rules, проектные паттерны (Decompose, Either, ModelScoped и т.д.)».

Пример description для `tangem-code-reviewer`:

> Code review для проекта Tangem Android. Проверяет соответствие Detekt rules, проектным паттернам (Decompose API/Impl, Either, ModelScoped, StateFlow), drawable naming (`ic_*`/`img_*`/`ill_*`), UnsafeStringResourceUsage. Использовать вместо общего `code-reviewer`.

### 7.2. Точная таблица маршрутизации в orchestrator-prompt

`tangem-orchestrator` обязан использовать Task tool **с точным именем** агента, не с общими описаниями. Полная таблица — см. §5.3.

В prompt orchestrator явно перечислены конкурирующие пары:

| Зона | Используем (точное имя) | НЕ путать с |
|---|---|---|
| Code review | `tangem-code-reviewer` | `code-reviewer` |
| Security audit | `tangem-security-auditor` | `security-auditor` |
| Bug investigation | `tangem-bug-investigator` | `debugger` |
| Testing | `tangem-test-writer` | — |
| EVM-блокчейн | `bsdk-evm-expert` | `blockchain-developer` |
| Android-фичи | соответствующий `app-*-expert` | `mobile-app-developer`, `mobile-developer`, `kotlin-specialist` |

### 7.3. Известные пересечения внутри пула

| Сценарий | Кто отвечает | Кто НЕ отвечает |
|---|---|---|
| Тесты после имплементации | `tangem-test-writer` | L3-специалист, написавший имплементацию |
| Финальный pre-PR review | `tangem-code-reviewer` | L3 |
| Изменения в core/analytics/feature-toggles/navigation/DI | `app-platform-expert` | Другие L3 (даже если фича их домена использует core) |
| NFT в UI/domain | `app-portfolio-expert` | `bsdk-nft-expert` (он только про providers в bsdk) |
| NFT-провайдеры в bsdk | `bsdk-nft-expert` | `app-portfolio-expert` |
| Сканирование карты в app | `app-wallet-mgmt-expert` | `card-nfc-commands-expert` (он только про NFC внутри card-sdk) |
| NFC-команды внутри card-sdk | `card-nfc-commands-expert` | `app-wallet-mgmt-expert` |
| Cross-repo задача (app + bsdk) | Orchestrator делит на подзадачи, каждый агент работает в своём репо | Никто не делает «и то и то» |

## 8. Workflow обновления агентов

### 8.1. Локально у разработчика

```bash
# 1. Правка
cd <любой_репо>/tangem-claude-agents
git checkout -b feature/AND-XXXX_update_portfolio_expert
$EDITOR agents/app/portfolio-expert.md

# 2. Commit + push + PR в agents-репо
git add agents/app/portfolio-expert.md
git commit -m "AND-XXXX Update app-portfolio-expert: add new pattern X"
git push -u origin feature/AND-XXXX_update_portfolio_expert
gh pr create   # ← один PR в tangem-claude-agents

# 3. После merge — никаких bump в parent-репо.
#    git submodule update --remote tangem-claude-agents подтягивает свежее.
```

### 8.2. Получить свежие агенты

```bash
# Опционально настроить алиас один раз:
git config --global alias.refresh-agents '!git submodule update --remote tangem-claude-agents'

# Дальше:
git refresh-agents
```

### 8.3. На CI (GitHub Actions)

Все pipeline'ы Claude-задач запускаются в GitHub Actions. Шаги setup:

```yaml
- uses: actions/checkout@v4
  with:
    submodules: recursive
    token: ${{ secrets.AGENTS_READ_PAT }}   # PAT с read к tangem-claude-agents

- name: Refresh agents to latest main
  run: git submodule update --remote tangem-claude-agents

- name: Run Claude task
  run: claude --task "..."
```

Никаких write-доступов в `tangem-claude-agents` от CI-бота не требуется — он только читает.

## 9. Setup для новой машины / нового репо

### 9.1. Первичный setup существующих репо (выполнить один раз)

```bash
# Для каждого из 4 репо (app, bsdk, card-sdk, hot-sdk):
cd <repo>
git submodule add -b main \
    git@github.com:tangem-developments/tangem-claude-agents.git \
    tangem-claude-agents
mkdir -p .claude
ln -s ../tangem-claude-agents/agents .claude/agents
git add .gitmodules tangem-claude-agents .claude/agents
git commit -m "AND-XXXX Add Claude agents submodule"
```

### 9.2. Новый разработчик клонирует репо

```bash
git clone --recurse-submodules <repo-url>
# .claude/agents автоматически работает через symlink
```

Если репо уже было склонировано без флага:

```bash
git submodule update --init --recursive
```

## 10. Опциональный CI на agents-репо (GitHub Actions)

В `tangem-claude-agents/.github/workflows/validate.yml`:

- **Markdown lint** — базовая валидация формата
- **YAML frontmatter валидатор** — проверка обязательных полей (`name`, `description`, `model`), уникальности `name` по всему репо
- **Path validator** — проверка, что упомянутые в prompt абсолютные пути соответствуют шаблонам валидных путей репо
- **Detekt-style проверка** — что не используется устаревший API (например, `pluralStringResource` без `Safe`-варианта в примерах)

Это ловит самые частые ошибки до ревью.

## 11. Что не входит в этот дизайн (выносится в план)

- Конкретное содержимое system prompt для каждого из 43 агентов
- Шаблоны frontmatter и инструкций по делегированию
- Скрипт `validate.yml` для CI agents-репо
- Скрипт `refresh-agents.sh` для разработчиков (если решим, что нужен)
- Документация в `README.md` agents-репо
- Миграция существующих skills (`analyze-logs`, `fix-crashlytics` и пр.) — оставить как есть, агенты их вызывают
- Какие именно подзадачи orchestrator делегирует параллельно vs последовательно (это конвенция в его prompt, прорабатывается в плане)

## 12. Риски и митигации

| Риск | Митигация |
|---|---|
| Floating submodule даёт нестабильность агентов между запусками | Агенты — инструкции, не код; не влияют на build/test/release |
| Разработчик забыл `git submodule update --remote` и использует устаревших агентов | Алиас `git refresh-agents`; в CI команда вызывается всегда |
| 43 агента — много для поддержки | Гранулярность сознательно укрупнена по кластерам; каждый агент лёгкий (~50-150 строк) |
| Orchestrator может выбрать «не того» специалиста | Чёткие `description` в frontmatter; orchestrator-prompt содержит карту-таблицу когда кого звать |
| Cross-cutting роли пересекаются с существующими skills | Роли явно вызывают skills (например, `tangem-bug-investigator` использует `analyze-logs`) |

## 13. Открытые вопросы (для плана)

1. Включать ли в `tangem-claude-agents` файл с **глобальной картой репозиториев** (абсолютные пути по умолчанию, конвенции имён)? Это упростит prompts отдельных агентов.
2. Должны ли cross-cutting роли иметь право звать L3-агентов, или они только реагируют на запросы L3? (Сейчас в дизайне — двусторонняя связь.)
3. Финальный выбор моделей: проверить, что поддержка `model:` в frontmatter и наличие нужных моделей (sonnet/opus/haiku) актуальны на момент реализации; перепроверить рекомендации (opus для orchestrator/security, sonnet для остальных).