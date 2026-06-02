# Дизайн: Smart Gas (EVM) — связка Gasless ↔ Yield Mode

> Источник требований: PDF «Smart Gas в EVM» (стр. 9–12 — мобильное приложение; стр. 10–11 — связка с Yield Mode).
> Gap-анализ показал, что базовый Gasless/Smart Gas в EVM уже реализован и в SDK, и в приложении
> (серия задач [REDACTED_TASK_KEY]…[REDACTED_TASK_KEY]). Нереализованной осталась связка **Gasless ↔ Yield Mode**
> (gap'ы G1/G2/G3/G5 из анализа). Этот документ проектирует именно её.

## 1. Цель и границы

баланса токена не хватает — дозабором `withdraw(yieldToken, amount)`, добавленным в gasless-транзакцию
как **батч-транзакция** (`GaslessBatchTransaction`).

- `tangem-app-android`: `domain/transaction`, `data/transaction`, `core/datasource`, `features/send-v2`.
- `blockchain-sdk-kotlin`: малая добавка (partial-withdraw calldata + обобщение upgrade-wrap).

- Бэкенд gasless-сервиса имеет batch-эндпоинт (`gaslessBatchTransaction` → `executeBatchTransaction`,
  Method ID `0xf9b181bf`) — готов или делается параллельно.
- Контракты (`Tangem7702GaslessExecutor`/`EntryPoint`) с поддержкой батча — готовы.


## 2. Архитектура (подход B — явный `GaslessFeePlan`)

Центральная новая абстракция — `GaslessFeePlan` и `ResolveGaslessFeePlanUseCase`, инкапсулирующий
дерево решений PDF. Шаг отправки «тупой»: берёт план и строит single/batch.

```
GaslessFeePlan (sealed)
 ├─ NativePay(fee)                                   // хватает нативной монеты
 ├─ TokenPay(token, fee)                             // хватает чистого баланса токена
 └─ TokenPayWithYieldWithdraw(                        // нужен дозабор из Yield
        token, fee, withdrawAmount, moduleUpgradeRequired)
```

```
FeeSelector ─▶ ResolveGaslessFeePlanUseCase ─▶ GaslessFeePlan
                                                   │
Confirm/Send ─▶ CreateAndSendGaslessTransactionUseCase(plan)
                 ├─ NativePay / TokenPay        ─▶ GaslessTransaction      (single, текущий путь)
                 └─ TokenPayWithYieldWithdraw    ─▶ GaslessBatchTransaction [mainTx, withdrawTx]
```

Обоснование выбора B (vs A «расширить на месте», C «сначала обобщённый батч»):
- Лучше всего ложится на форму дерева решений PDF; вся ветвистая логика — в одном тестируемом месте.
- Fee-модель не перегружается транспортной нагрузкой.
- Максимум переиспользования уже готовой Yield-машинерии (см. §7).
- Обобщённый батч-примитив из C встраивается бесплатно: withdraw — это «доп. транзакция батча».

## 3. Компоненты

### 3.1 Новые domain-модели (`domain/transaction`)
- `GaslessBatchTransactionData(transactions: List<Transaction>, fee, nonce)` — параллель к существующему
  `GaslessTransactionData` (переиспользует вложенные `Transaction` и `Fee`).
- `GaslessFeePlan` (sealed, см. §2) — несёт всё нужное и для отображения комиссии, и для отправки.
- Расширить `GetFeeError.GaslessError`: добавить `ModuleUpdateUnavailable`
  (есть `NotEnoughFunds`/`NetworkIsNotSupported`/`NoSupportedTokensFound`/`DataError`).

### 3.2 `ResolveGaslessFeePlanUseCase` (новый — дерево решений PDF, стр. 5–6)
1. Считаем комиссию в токене (переиспользуем `TokenFeeCalculator`).
2. Чистый баланс (минус сумма отправки, если комиссия платится тем же токеном) ≥ комиссии → `TokenPay`.
3. Иначе, если у токена активен Yield Mode:
   - a. Версия модуля через `YieldSupplyTransactionRepository` / `YieldModuleVersionStatus`:
     - `UpgradeUnavailable` / `Indeterminate` → ошибка нехватки фи (`ModuleUpdateUnavailable`).
     - `UpgradeAvailable` → `moduleUpgradeRequired = true`.
     - `UpToDate` → `moduleUpgradeRequired = false`.
   - b. Пересчёт комиссии с добавлением `withdrawGasLimit` (газ на `withdraw(yieldToken, 10000-проба)`;
     при апгрейде — газ на upgrade-wrapped вызов).
   - c. `withdrawAmount` = пересчитанный `maxTokenFee` (+ сумма отправки, если тот же токен).
   - d. `(чистый + yield) ≥ withdrawAmount` → `TokenPayWithYieldWithdraw`, иначе `NotEnoughFunds`.
4. Иначе → `NotEnoughFunds`.

(`findTokensToPayFee` / `GetAvailableFeeTokensUseCase`) расширяется — токен попадает в список,
если комиссию покрывает чистый **или** yield-баланс.

### 3.3 `CreateAndSendGaslessTransactionUseCase` (расширить)
- Принимает `GaslessFeePlan`.
- `TokenPayWithYieldWithdraw`: строит `withdrawTx` (`to` = адрес Yield-модуля юзера, `value` = 0,
  `data` = partial-withdraw calldata, при `moduleUpgradeRequired` — upgrade-wrapped), собирает
  `GaslessBatchTransaction = [mainTx, withdrawTx]`, подписывает batch EIP-712, шлёт на batch-эндпоинт.
- Single-путь без изменений (по-прежнему один `signer.sign(listOf(eip712, eip7702))` — одно касание).

### 3.4 `Eip712TypedDataBuilder` (расширить)
- `buildBatch(gaslessBatchTransaction, chainId, verifyingContract)`:
  `primaryType = "GaslessBatchTransaction"`, тип `transactions: Transaction[]` (массив) + `Fee` + `nonce`.
  Домен (`Tangem7702GaslessExecutor`, version `1`) идентичен single-варианту.

### 3.5 Репозиторий / data (`data/transaction` + `core/datasource`)
- `GaslessTransactionRepository.signGaslessBatchTransaction(...)`.
- Новые DTO в `core/datasource/api/gasless`: `GaslessBatchTransactionRequest` (поле `gaslessBatchTransaction`),
  под batch-эндпоинт. Ответ переиспользует существующий `GaslessSignedTransactionResult`.

### 3.6 `blockchain-sdk-kotlin` (малая добавка)
- Новый `EthereumYieldSupplyWithdrawCallData` для `withdraw(address yieldToken, uint256 amount)`.
  (Текущий `EthereumYieldSupplyExitCallData` — это **полный** `withdrawAndDeactivate(address)`,
  Method ID `0xc65e6dcf`, не подходит для частичного вывода.)
- Обобщить `wrapYieldSwapCallDataWithUpgradeIfNeeded` → `wrapYieldCallDataWithUpgradeIfNeeded`
  (метод уже принимает generic `SmartContractCallData`) для переиспользования с withdraw-calldata.
- Адрес Yield-модуля — существующий `YieldModuleAddressProvider`;
  `withdrawGasLimit` — `eth_estimateGas` на существующей инфре `EthereumLikeJsonRpcProvider`.

### 3.7 Связка (`features/send-v2`)
- Протащить `GaslessFeePlan` из fee-селектора (`FeeSelectorModel`/`FeeSelectorLogic`) в шаг отправки
  (`SendConfirmModel`).
- Нотификация юзеру, что комиссия будет выведена из Yield Mode (прозрачность, не блокирующий шаг) —
  через существующий `NotificationsModel`.
- Фича-тоггл: `GASLESS_YIELD_WITHDRAW_ENABLED` (в `feature_toggles_config.json` + `SendFeatureToggles`).

## 4. Обработка ошибок
- `YieldModuleVersionStatus` → `GaslessError`:
  - `UpgradeUnavailable` / `Indeterminate` → «не хватает фи» (`ModuleUpdateUnavailable`)
    (PDF: «если обновление невозможно — показываем ошибку о нехватке фи»).
  - `(чистый + yield) < нужного` → `NotEnoughFunds`.
- Ошибки batch-эндпоинта → `SendTransactionError.DataError` (текущий паттерн `catch { ... }`).
- Фолбэк: если yield-путь не разрешился — поведение как сейчас (токен-only / нативная монета).

## 5. Тестирование
- Unit на `ResolveGaslessFeePlanUseCase` — каждая ветка PDF: хватает чистого; топап из yield;
  добавка суммы отправки при том же токене; нужен апгрейд; апгрейд недоступен; indeterminate;
  не хватает даже с yield. MockK + Turbine + Truth.
- Golden-JSON тест `Eip712TypedDataBuilder.buildBatch` (зеркало существующего EIP-712 теста).
- Тест кодирования `EthereumYieldSupplyWithdrawCallData` в SDK (зеркало `EthereumYieldSupplyExitCallData`-теста).
- Batch-ветка `CreateAndSendGaslessTransactionUseCase` (2 подписи, batch DTO, pending).

## 6. Открытые вопросы / допущения
1. Точная форма batch-DTO бэкенда — сверить по README gasless-сервиса (имена полей `gaslessBatchTransaction`,
   `transactions[]`).
2. UX: явная нотификация «вывод из Yield для оплаты комиссии» — рекомендуется нотификация, не блокирующий шаг.
3. Проба `withdrawGasLimit` = 10000 — сверить с ожиданием контракта.
4. Тот же токен: если комиссия платится тем же токеном, что выводится из yield — порядок в батче
   (withdraw второй по PDF; полагаемся, что контракт исполняет батч по порядку массива и баланса хватает
   к моменту списания комиссии).

## 7. Переиспользуемая существующая машинерия (не пишем заново)
- `YieldModuleVersionStatus` (SDK) — состояния `UpToDate/UpgradeAvailable/UpgradeUnavailable/NotDeployed/Indeterminate`
  = PDF-флаг `yieldModuleRequiresUpdate` + «обновление невозможно → ошибка».
- `WrapYieldSwapCallDataWithUpgradeUseCase` / `wrapYieldSwapCallDataWithUpgradeIfNeeded` —
  оборачивание call data транзакцией обновления модуля.
- `YieldSupplyTransactionRepository.getEffectiveProtocolBalance` — yield-баланс (G5).
- `YieldModuleAddressProvider` — адрес Yield-модуля юзера.
- `CreateAndSendGaslessTransactionUseCase.signHashes` — подпись EIP-712 + EIP-7702 в одно касание.
- `TokenFeeCalculator` / `EstimateFeeForTokenUseCase` — расчёт комиссии в токене.
- `Fee.Ethereum.TokenCurrency` — несёт `coinPriceInToken`/`feeTransferGasLimit`/`baseGas`.