# Gasless ↔ Yield Mode interplay — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow a gasless EVM transaction to pay its token fee from the token's Yield Mode balance — by appending a partial `withdraw(yieldToken, amount)` to the gasless transaction as a `GaslessBatchTransaction` when the plain token balance is insufficient.

**Architecture:** Approach B from the design spec — an explicit `GaslessFeePlan` (sealed) produced by a new `ResolveGaslessFeePlanUseCase` that encapsulates the PDF decision tree. The plan is carried inside the existing `TransactionFeeExtended` object that already flows from the fee model to `CreateAndSendGaslessTransactionUseCase`. The send step is "dumb": it builds a single `GaslessTransaction` or a `GaslessBatchTransaction` based on the plan. Yield version/upgrade machinery (`YieldModuleVersionStatus`, `wrapWithUpgradeIfNeeded`), `getEffectiveProtocolBalance`, and the partial-withdraw calldata are reused/added in `blockchain-sdk-kotlin`.

**Tech Stack:** Kotlin, Arrow (`Either`/`Raise`), Hilt, Moshi/Retrofit, MockK + Truth + JUnit5 + Turbine, two repos: `blockchain-sdk-kotlin` (JUnit4 + Truth) and `tangem-app-android`.

**Spec:** `docs/superpowers/specs/2026-06-02-gasless-yield-mode-interplay-design.md`

**Two repos / paths:**
- SDK = `/Users/mikhailz/StudioProjects/tangem/private/blockchain-sdk-kotlin`
- APP = `/Users/mikhailz/StudioProjects/tangem/private/tangem-app-android`

**Branch:** create `feature/AND-XXXX_gasless_yield_interplay` in each repo before starting (replace `AND-XXXX` with the real Jira id). Commit messages: `AND-XXXX <desc>`.

---

## File Structure (decomposition)

**SDK (`blockchain-sdk-kotlin`)**
- Create `.../yieldsupply/providers/ethereum/yield/EthereumYieldSupplyWithdrawCallData.kt` — calldata for `withdraw(address,uint256)`.
- Modify `.../yieldsupply/YieldSupplyContractCallDataProviderFactory.kt` — add `getWithdrawCallData(...)`.

**APP domain models (`domain/transaction`)**
- Create `.../domain/transaction/models/GaslessBatchTransactionData.kt`
- Create `.../domain/transaction/models/GaslessFeePlan.kt`
- Modify `.../domain/transaction/models/TransactionFeeExtended.kt` — add optional `gaslessFeePlan`.
- Modify `.../domain/transaction/models/.../error/GetFeeError.kt` — add `ModuleUpdateUnavailable`.

**APP yield repo (`domain/yield-supply`, `data/yield-supply`)**
- Modify `.../domain/yield/supply/YieldSupplyTransactionRepository.kt` — add `getYieldModuleVersionStatus(...)` and `createPartialWithdrawCallData(...)`.
- Modify `.../data/yield/supply/DefaultYieldSupplyTransactionRepository.kt` — implement the two new methods.

**APP gasless domain logic (`domain/transaction/usecase/gasless`)**
- Create `.../usecase/gasless/ResolveGaslessFeePlanUseCase.kt`
- Modify `.../usecase/gasless/GetFeeForGaslessUseCase.kt` — delegate to resolver, map to `TransactionFeeExtended`.
- Modify `.../usecase/gasless/Eip712TypedDataBuilder.kt` — add `buildBatch(...)`.
- Modify `.../usecase/gasless/CreateAndSendGaslessTransactionUseCase.kt` — batch branch.

**APP data/API (`core/datasource`, `data/transaction`)**
- Create `.../datasource/api/gasless/models/GaslessBatchTransactionRequest.kt`
- Modify `.../datasource/api/gasless/GaslessTxServiceApi.kt` — add batch endpoint.
- Modify `.../data/transaction/convertes/GaslessTransactionRequestBuilder.kt` — add batch builder.
- Modify `.../domain/transaction/GaslessTransactionRepository.kt` + `.../data/transaction/DefaultGaslessTransactionRepository.kt` — add `signGaslessBatchTransaction(...)`.

**APP wiring (`features/send-v2`, DI, toggles)**
- Modify `SendConfirmModel.kt`, `NotificationsModel.kt`, `feature_toggles_config.json`, relevant DI modules.

**Phase boundary for partial merge:** Phases 1–5 (SDK + domain + data) are independently testable and mergeable. Phase 6 (UI wiring + toggle) flips the feature on. Keep `GASLESS_YIELD_WITHDRAW_ENABLED` off until Phase 6 is verified.

---

## Phase 1 — SDK: partial withdraw calldata

### Task 1: `EthereumYieldSupplyWithdrawCallData`

**Files:**
- Create: `blockchain-sdk-kotlin/blockchain/src/main/java/com/tangem/blockchain/yieldsupply/providers/ethereum/yield/EthereumYieldSupplyWithdrawCallData.kt`
- Test: `blockchain-sdk-kotlin/blockchain/src/test/java/com/tangem/blockchain/yieldsupply/providers/ethereum/yield/EthereumYieldSupplyWithdrawCallDataTest.kt`

- [ ] **Step 1: Confirm the method selector**

Run (from contracts repo or any Foundry env): `cast sig "withdraw(address,uint256)"`
Expected: `0xf3fef3a3`. This MUST match the deployed `Tangem7702*` yield module ABI (PDF: "Адреса/ABI контрактов брать в README в репозитории контрактов"). If the README lists a different selector, use that value in `METHOD_ID` below and update the test.

- [ ] **Step 2: Write the failing test** (mirror `EthereumYieldSupplyExitCallDataTest`)

```kotlin
package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.google.common.truth.Truth
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.extensions.hexToBytes
import org.junit.Test
import java.math.BigDecimal

internal class EthereumYieldSupplyWithdrawCallDataTest {
    private val blockchain = Blockchain.Ethereum
    private val methodId = "0xf3fef3a3".hexToBytes()
    private val tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678"
    private val tokenAddressData = "0000000000000000000000001234567890abcdef1234567890abcdef12345678".hexToBytes()

    // 6-decimals token, value 0.01 => 10000 base units => 0x2710
    private val amount = Amount(
        token = Token(symbol = "USDC", contractAddress = tokenContractAddress, decimals = 6),
        value = BigDecimal("0.01"),
    )
    private val amountData = "0000000000000000000000000000000000000000000000000000000000002710".hexToBytes()

    @Test
    fun `call data is correct`() {
        val callData = EthereumYieldSupplyWithdrawCallData(
            tokenContractAddress = tokenContractAddress,
            amount = amount,
        )
        val expected = methodId + tokenAddressData + amountData
        Truth.assertThat(callData.data).isEqualTo(expected)
    }

    @Test
    fun `validate call data`() {
        val valid = EthereumYieldSupplyWithdrawCallData(tokenContractAddress, amount)
        Truth.assertThat(valid.validate(blockchain)).isTrue()

        val zeroAddr = EthereumYieldSupplyWithdrawCallData(EthereumUtils.ZERO_ADDRESS, amount)
        Truth.assertThat(zeroAddr.validate(blockchain)).isFalse()
    }
}
```

- [ ] **Step 3: Run test, verify it fails**

Run: `cd blockchain-sdk-kotlin && ./gradlew :blockchain:testDebugUnitTest --tests "*EthereumYieldSupplyWithdrawCallDataTest*"`
Expected: FAIL — `EthereumYieldSupplyWithdrawCallData` unresolved.

- [ ] **Step 4: Write the implementation** (mirror `EthereumYieldSupplySendCallData` for amount encoding)

```kotlin
package com.tangem.blockchain.yieldsupply.providers.ethereum.yield

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.isNotZeroAddress
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.blockchain.extensions.hexToFixedSizeBytes
import com.tangem.blockchain.extensions.toFixedSizeBytes
import com.tangem.common.extensions.hexToBytes

/**
 * Call data for partially withdrawing a given [amount] of a yield token from the user's yield module.
 *
 * Signature: `withdraw(address yieldToken, uint256 amount)`
 */
class EthereumYieldSupplyWithdrawCallData(
    val tokenContractAddress: String,
    private val amount: Amount,
) : SmartContractCallData {
    override val methodId: String = METHOD_ID
    override val data: ByteArray
        get() {
            val prefixData = methodId.hexToBytes()
            val tokenContractAddressData = tokenContractAddress.hexToFixedSizeBytes()
            val amountData = amount.bigIntegerValue()?.toFixedSizeBytes() ?: error("Invalid withdraw amount")
            return prefixData + tokenContractAddressData + amountData
        }

    override fun validate(blockchain: Blockchain): Boolean {
        return blockchain.validateAddress(tokenContractAddress) && tokenContractAddress.isNotZeroAddress()
    }

    companion object {
        const val METHOD_ID = "0xf3fef3a3"
    }
}
```

- [ ] **Step 5: Run test, verify it passes**

Run: `cd blockchain-sdk-kotlin && ./gradlew :blockchain:testDebugUnitTest --tests "*EthereumYieldSupplyWithdrawCallDataTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
cd blockchain-sdk-kotlin
git add blockchain/src/main/java/com/tangem/blockchain/yieldsupply/providers/ethereum/yield/EthereumYieldSupplyWithdrawCallData.kt \
        blockchain/src/test/java/com/tangem/blockchain/yieldsupply/providers/ethereum/yield/EthereumYieldSupplyWithdrawCallDataTest.kt
git commit -m "AND-XXXX Add EthereumYieldSupplyWithdrawCallData for partial yield withdraw"
```

### Task 2: Factory method `getWithdrawCallData`

**Files:**
- Modify: `blockchain-sdk-kotlin/blockchain/src/main/java/com/tangem/blockchain/yieldsupply/YieldSupplyContractCallDataProviderFactory.kt`
- Test: `blockchain-sdk-kotlin/blockchain/src/test/java/com/tangem/blockchain/yieldsupply/YieldSupplyContractCallDataProviderFactoryTest.kt`

- [ ] **Step 1: Write the failing test** (append to the existing factory test class)

```kotlin
@Test
fun `getWithdrawCallData returns withdraw call data`() {
    val amount = Amount(
        token = Token(symbol = "USDC", contractAddress = "0x1234567890abcdef1234567890abcdef12345678", decimals = 6),
        value = BigDecimal("0.01"),
    )
    val callData = YieldSupplyContractCallDataProviderFactory.getWithdrawCallData(
        tokenContractAddress = "0x1234567890abcdef1234567890abcdef12345678",
        amount = amount,
    )
    Truth.assertThat(callData).isInstanceOf(EthereumYieldSupplyWithdrawCallData::class.java)
    Truth.assertThat(callData.methodId).isEqualTo("0xf3fef3a3")
}
```
(Ensure imports `com.tangem.blockchain.common.Amount`, `com.tangem.blockchain.common.Token`, `java.math.BigDecimal`, and `...ethereum.yield.EthereumYieldSupplyWithdrawCallData` are present.)

- [ ] **Step 2: Run test, verify it fails**

Run: `cd blockchain-sdk-kotlin && ./gradlew :blockchain:testDebugUnitTest --tests "*YieldSupplyContractCallDataProviderFactoryTest*"`
Expected: FAIL — `getWithdrawCallData` unresolved.

- [ ] **Step 3: Add the factory method** (place next to `getExitCallData`)

```kotlin
/**
 * Provides call data for partially withdrawing [amount] of a yield token from the user's yield module.
 *
 * @param tokenContractAddress The address of the token contract.
 * @param amount The amount to withdraw.
 * @return [SmartContractCallData] for `withdraw(address,uint256)`.
 */
fun getWithdrawCallData(tokenContractAddress: String, amount: Amount): SmartContractCallData =
    EthereumYieldSupplyWithdrawCallData(
        tokenContractAddress = tokenContractAddress,
        amount = amount,
    )
```

- [ ] **Step 4: Run test, verify it passes**

Run: `cd blockchain-sdk-kotlin && ./gradlew :blockchain:testDebugUnitTest --tests "*YieldSupplyContractCallDataProviderFactoryTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd blockchain-sdk-kotlin
git add blockchain/src/main/java/com/tangem/blockchain/yieldsupply/YieldSupplyContractCallDataProviderFactory.kt \
        blockchain/src/test/java/com/tangem/blockchain/yieldsupply/YieldSupplyContractCallDataProviderFactoryTest.kt
git commit -m "AND-XXXX Add getWithdrawCallData to YieldSupplyContractCallDataProviderFactory"
```

> After Phase 1, publish/refresh the local SDK artifact the app consumes (the app depends on `com.tangem:blockchain` via `tangem_dependencies.toml`). If the team uses a local `mavenLocal` flow, run the SDK's publish task; otherwise bump the dependency to the branch build. Confirm the app can resolve `EthereumYieldSupplyWithdrawCallData` / `getWithdrawCallData` before Phase 3.

---

## Phase 2 — APP domain models

### Task 3: `GaslessBatchTransactionData`

**Files:**
- Create: `domain/transaction/src/main/java/com/tangem/domain/transaction/models/GaslessBatchTransactionData.kt`
- Test: `domain/transaction/src/test/java/com/tangem/domain/transaction/models/GaslessBatchTransactionDataTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.tangem.domain.transaction.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class GaslessBatchTransactionDataTest {
    @Test
    fun `holds transactions fee and nonce`() {
        val tx = GaslessTransactionData.Transaction(to = "0xabc", value = BigInteger.ZERO, data = byteArrayOf(1))
        val withdraw = GaslessTransactionData.Transaction(to = "0xdef", value = BigInteger.ZERO, data = byteArrayOf(2))
        val fee = GaslessTransactionData.Fee(
            feeToken = "0xtoken", maxTokenFee = BigInteger.TEN, coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(100), baseGas = BigInteger.valueOf(60000), feeReceiver = "0xrecv",
        )
        val batch = GaslessBatchTransactionData(transactions = listOf(tx, withdraw), fee = fee, nonce = BigInteger.ZERO)

        assertThat(batch.transactions).hasSize(2)
        assertThat(batch.transactions[1]).isEqualTo(withdraw)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*GaslessBatchTransactionDataTest*"`
Expected: FAIL — `GaslessBatchTransactionData` unresolved.

- [ ] **Step 3: Write the model** (reuses nested `Transaction`/`Fee` from `GaslessTransactionData`)

```kotlin
package com.tangem.domain.transaction.models

import java.math.BigInteger

/**
 * Domain model for a gasless BATCH transaction (EIP-712 primaryType `GaslessBatchTransaction`).
 * Reuses [GaslessTransactionData.Transaction] and [GaslessTransactionData.Fee].
 *
 * @property transactions ordered list — index 0 is the user's main transaction, subsequent entries
 *                        are appended operations (e.g. the yield `withdraw`). Executed in array order.
 * @property fee fee payment configuration.
 * @property nonce nonce from the user's contract.
 */
data class GaslessBatchTransactionData(
    val transactions: List<GaslessTransactionData.Transaction>,
    val fee: GaslessTransactionData.Fee,
    val nonce: BigInteger,
)
```

- [ ] **Step 4: Run test, verify it passes**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*GaslessBatchTransactionDataTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/models/GaslessBatchTransactionData.kt \
        domain/transaction/src/test/java/com/tangem/domain/transaction/models/GaslessBatchTransactionDataTest.kt
git commit -m "AND-XXXX Add GaslessBatchTransactionData domain model"
```

### Task 4: `GaslessFeePlan` + `ModuleUpdateUnavailable` error

**Files:**
- Create: `domain/transaction/src/main/java/com/tangem/domain/transaction/models/GaslessFeePlan.kt`
- Modify: `domain/transaction/models/src/main/kotlin/com/tangem/domain/transaction/error/GetFeeError.kt`

- [ ] **Step 1: Add the error case** in `GetFeeError.GaslessError`

```kotlin
sealed class GaslessError : GetFeeError() {
    data object NetworkIsNotSupported : GaslessError()
    data object NoSupportedTokensFound : GaslessError()
    data object NotEnoughFunds : GaslessError()
    data object ModuleUpdateUnavailable : GaslessError()
    data class DataError(val cause: Throwable?) : GaslessError()
}
```

- [ ] **Step 2: Write the `GaslessFeePlan` model**

```kotlin
package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

/**
 * Resolved strategy for paying a gasless transaction fee. Produced by ResolveGaslessFeePlanUseCase,
 * consumed by CreateAndSendGaslessTransactionUseCase.
 */
sealed interface GaslessFeePlan {

    /** Pay in the native coin (enough native balance) — falls back to the standard fee. */
    data class NativePay(val fee: Fee) : GaslessFeePlan

    /** Pay the fee from the token's plain balance. */
    data class TokenPay(
        val feeToken: CryptoCurrency.Token,
        val fee: Fee.Ethereum.TokenCurrency,
    ) : GaslessFeePlan

    /**
     * Pay the fee by first withdrawing [withdrawAmount] of the token from the user's yield module
     * (appended as a second batch transaction). [withdrawCallData] is already upgrade-wrapped when
     * the module needs an upgrade.
     */
    data class TokenPayWithYieldWithdraw(
        val feeToken: CryptoCurrency.Token,
        val fee: Fee.Ethereum.TokenCurrency,
        val withdrawAmount: BigInteger,
        val withdrawCallData: SmartContractCallData,
        val yieldModuleAddress: String,
    ) : GaslessFeePlan
}
```

- [ ] **Step 3: Compile the module**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:compileDebugKotlin :domain:transaction:models:compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/models/GaslessFeePlan.kt \
        domain/transaction/models/src/main/kotlin/com/tangem/domain/transaction/error/GetFeeError.kt
git commit -m "AND-XXXX Add GaslessFeePlan model and ModuleUpdateUnavailable error"
```

### Task 5: Carry the plan in `TransactionFeeExtended`

**Files:**
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/models/TransactionFeeExtended.kt`

- [ ] **Step 1: Add the optional field**

```kotlin
package com.tangem.domain.transaction.models

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrency

data class TransactionFeeExtended(
    val transactionFee: TransactionFee,
    val feeTokenId: CryptoCurrency.ID,
    /**
     * Resolved gasless fee strategy. Non-null only for token-paid gasless fees; null for native fee.
     * When it is [GaslessFeePlan.TokenPayWithYieldWithdraw], the send step builds a batch transaction.
     */
    val gaslessFeePlan: GaslessFeePlan? = null,
)
```

- [ ] **Step 2: Compile** (the new param is defaulted, so existing call sites still compile)

Run: `cd tangem-app-android && ./gradlew :domain:transaction:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/models/TransactionFeeExtended.kt
git commit -m "AND-XXXX Carry GaslessFeePlan inside TransactionFeeExtended"
```

---

## Phase 3 — APP yield repo extensions

### Task 6: Expose version status + partial-withdraw calldata from the yield repo

**Files:**
- Modify: `domain/yield-supply/src/main/java/com/tangem/domain/yield/supply/YieldSupplyTransactionRepository.kt`
- Modify: `data/yield-supply/src/main/java/com/tangem/data/yield/supply/DefaultYieldSupplyTransactionRepository.kt`
- Test: `data/yield-supply/src/test/java/com/tangem/data/yield/supply/DefaultYieldSupplyTransactionRepositoryTest.kt`

Rationale: `ResolveGaslessFeePlanUseCase` needs (a) the module version once (to wrap and to detect unavailable/indeterminate) and (b) an upgrade-wrapped partial-withdraw calldata. The existing `wrapYieldSwapCallDataWithUpgradeIfNeeded` already wraps generic call data + throws on unavailable/indeterminate; we add a thin method that builds the withdraw calldata and wraps it, plus expose the version status for the affordability/error mapping.

- [ ] **Step 1: Add interface methods**

```kotlin
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus

// inside interface YieldSupplyTransactionRepository:

/** Returns the on-chain version status of the user's yield module for [network]. */
suspend fun getYieldModuleVersionStatus(userWalletId: UserWalletId, network: Network): YieldModuleVersionStatus

/**
 * Builds an upgrade-wrapped `withdraw(yieldToken, amount)` call data for the user's yield module.
 * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
 * @throws com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
 */
suspend fun createPartialWithdrawCallData(
    userWalletId: UserWalletId,
    cryptoCurrency: CryptoCurrency,
    amount: Amount,
): SmartContractCallData
```

- [ ] **Step 2: Write the failing test** (mirror style of `DefaultYieldSupplyTransactionRepositoryTest`; mock `WalletManagersFacade`/wallet manager)

```kotlin
@Test
fun `createPartialWithdrawCallData wraps withdraw call data when module up to date`() = runTest {
    val token = MockCryptoCurrencyFactory().ethereumToken // use existing test currency factory; otherwise build a CryptoCurrency.Token
    val walletManager = mockWalletManagerReturningVersion(YieldModuleVersionStatus.UpToDate)
    coEvery { walletManagersFacade.getOrCreateWalletManager(any(), any<Blockchain>(), any()) } returns walletManager

    val callData = repository.createPartialWithdrawCallData(userWalletId, token, amount)

    assertThat(callData.methodId).isEqualTo("0xf3fef3a3")
}
```
(Use the test's existing helpers for constructing a `CryptoCurrency.Token` and a mocked EVM wallet manager; `mockWalletManagerReturningVersion` stubs `walletManager.checkModuleVersionStatus()`.)

- [ ] **Step 3: Run test, verify it fails**

Run: `cd tangem-app-android && ./gradlew :data:yield-supply:testDebugUnitTest --tests "*DefaultYieldSupplyTransactionRepositoryTest*"`
Expected: FAIL — `createPartialWithdrawCallData` unresolved.

- [ ] **Step 4: Implement in `DefaultYieldSupplyTransactionRepository`**

```kotlin
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionStatus

override suspend fun getYieldModuleVersionStatus(
    userWalletId: UserWalletId,
    network: Network,
): YieldModuleVersionStatus = withContext(dispatchers.io) {
    val walletManager = walletManagersFacade.getOrCreateWalletManager(
        userWalletId = userWalletId,
        blockchain = network.toBlockchain(),
        derivationPath = network.derivationPath.value,
    ) ?: error("Wallet manager not found for $network")
    walletManager.checkModuleVersionStatus()
}

override suspend fun createPartialWithdrawCallData(
    userWalletId: UserWalletId,
    cryptoCurrency: CryptoCurrency,
    amount: Amount,
): SmartContractCallData = withContext(dispatchers.io) {
    require(cryptoCurrency is CryptoCurrency.Token)
    val walletManager = walletManagersFacade.getOrCreateWalletManager(
        userWalletId = userWalletId,
        blockchain = cryptoCurrency.network.toBlockchain(),
        derivationPath = cryptoCurrency.network.derivationPath.value,
    ) ?: error("Wallet manager not found")
    val withdrawCallData = YieldSupplyContractCallDataProviderFactory.getWithdrawCallData(
        tokenContractAddress = cryptoCurrency.contractAddress,
        amount = amount,
    )
    val versionStatus = walletManager.checkModuleVersionStatus()
    YieldSupplyContractCallDataProviderFactory.wrapWithUpgradeIfNeeded(versionStatus, withdrawCallData)
}
```

- [ ] **Step 5: Run test, verify it passes**

Run: `cd tangem-app-android && ./gradlew :data:yield-supply:testDebugUnitTest --tests "*DefaultYieldSupplyTransactionRepositoryTest*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
cd tangem-app-android
git add domain/yield-supply/src/main/java/com/tangem/domain/yield/supply/YieldSupplyTransactionRepository.kt \
        data/yield-supply/src/main/java/com/tangem/data/yield/supply/DefaultYieldSupplyTransactionRepository.kt \
        data/yield-supply/src/test/java/com/tangem/data/yield/supply/DefaultYieldSupplyTransactionRepositoryTest.kt
git commit -m "AND-XXXX Expose yield module version + partial withdraw call data"
```

---

## Phase 4 — APP gasless decision logic

### Task 7: `ResolveGaslessFeePlanUseCase` — the decision tree

**Files:**
- Create: `domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/ResolveGaslessFeePlanUseCase.kt`
- Test: `domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/ResolveGaslessFeePlanUseCaseTest.kt`

This usecase is given the already-computed token fee (`Fee.Ethereum.TokenCurrency` from `TokenFeeCalculator.calculateTokenFee`) and the token status, and decides between `TokenPay` and `TokenPayWithYieldWithdraw`, or raises `NotEnoughFunds` / `ModuleUpdateUnavailable`.

Inputs (constructor): `YieldSupplyTransactionRepository`, `WalletManagersFacade` (for withdraw gas estimation).

- [ ] **Step 1: Write the failing tests** (cover each PDF branch)

```kotlin
package com.tangem.domain.transaction.usecase.gasless

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.GaslessFeePlan
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class ResolveGaslessFeePlanUseCaseTest {

    private val yieldRepo = mockk<com.tangem.domain.yield.supply.YieldSupplyTransactionRepository>()
    private val walletManagersFacade = mockk<com.tangem.domain.walletmanager.WalletManagersFacade>(relaxed = true)
    private val useCase = ResolveGaslessFeePlanUseCase(yieldRepo, walletManagersFacade)

    // helpers omitted: tokenStatus(plainBalance, yieldActive), tokenFee(feeAmount) -> Fee.Ethereum.TokenCurrency

    @Test
    fun `plain balance covers fee -- TokenPay`() = runTest {
        val result = useCase(/* userWallet, tokenStatus(plain = 5, yieldActive = false), tokenFee(feeAmount = 3) */)
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPay::class.java)
    }

    @Test
    fun `plain insufficient, yield active and covers -- TokenPayWithYieldWithdraw`() = runTest {
        coEvery { yieldRepo.getEffectiveProtocolBalance(any(), any()) } returns BigDecimal("10")
        coEvery { yieldRepo.createPartialWithdrawCallData(any(), any(), any()) } returns mockk(relaxed = true)
        // stub withdraw gas estimate via walletManagersFacade.estimateFee or walletManager.getGasLimit
        val result = useCase(/* tokenStatus(plain = 1, yieldActive = true), tokenFee(feeAmount = 3) */)
        assertThat(result.getOrNull()).isInstanceOf(GaslessFeePlan.TokenPayWithYieldWithdraw::class.java)
    }

    @Test
    fun `plain insufficient, no yield -- NotEnoughFunds`() = runTest {
        val result = useCase(/* tokenStatus(plain = 1, yieldActive = false), tokenFee(feeAmount = 3) */)
        assertThat(result.swap().getOrNull()).isEqualTo(GaslessError.NotEnoughFunds)
    }

    @Test
    fun `module upgrade unavailable -- ModuleUpdateUnavailable`() = runTest {
        coEvery { yieldRepo.getEffectiveProtocolBalance(any(), any()) } returns BigDecimal("10")
        coEvery { yieldRepo.createPartialWithdrawCallData(any(), any(), any()) } throws
            YieldModuleUpgradeUnavailableException(currentImplementation = "0xold")
        val result = useCase(/* tokenStatus(plain = 1, yieldActive = true), tokenFee(feeAmount = 3) */)
        assertThat(result.swap().getOrNull()).isEqualTo(GaslessError.ModuleUpdateUnavailable)
    }

    @Test
    fun `plain plus yield still insufficient -- NotEnoughFunds`() = runTest {
        coEvery { yieldRepo.getEffectiveProtocolBalance(any(), any()) } returns BigDecimal("0.5")
        val result = useCase(/* tokenStatus(plain = 1, yieldActive = true), tokenFee(feeAmount = 3) */)
        assertThat(result.swap().getOrNull()).isEqualTo(GaslessError.NotEnoughFunds)
    }
}
```
(Flesh out the omitted helpers using `CryptoCurrencyStatus`/`Fee.Ethereum.TokenCurrency` builders already used in `TokenFeeCalculatorTest`.)

- [ ] **Step 2: Run tests, verify they fail**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*ResolveGaslessFeePlanUseCaseTest*"`
Expected: FAIL — `ResolveGaslessFeePlanUseCase` unresolved.

- [ ] **Step 3: Implement the usecase**

```kotlin
package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.providers.YieldModuleUpgradeUnavailableException
import com.tangem.blockchain.yieldsupply.providers.YieldModuleVersionIndeterminateException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.GetFeeError.GaslessError
import com.tangem.domain.transaction.models.GaslessFeePlan
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Implements the PDF decision tree (steps 5–6) for resolving how a gasless token fee is paid.
 *
 * @param sendAmountInFeeToken amount of the fee token the user is also spending in the main tx
 *        (BigDecimal.ZERO unless the fee is paid in the very token being sent).
 */
class ResolveGaslessFeePlanUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        tokenStatus: CryptoCurrencyStatus,
        tokenFee: Fee.Ethereum.TokenCurrency,
        isYieldActive: Boolean,
        sendAmountInFeeToken: BigDecimal,
    ): Either<GetFeeError, GaslessFeePlan> = either {
        val token = tokenStatus.currency as? CryptoCurrency.Token
            ?: raise(GaslessError.DataError(IllegalStateException("fee currency must be a token")))

        val feeAmount = tokenFee.amount.value
            ?: raise(GaslessError.DataError(IllegalStateException("token fee amount is null")))
        val plainBalance = tokenStatus.value.amount ?: BigDecimal.ZERO
        val required = feeAmount + sendAmountInFeeToken

        if (plainBalance >= required) {
            return@either GaslessFeePlan.TokenPay(feeToken = token, fee = tokenFee)
        }

        if (!isYieldActive) raise(GaslessError.NotEnoughFunds)

        val yieldBalance = yieldSupplyTransactionRepository
            .getEffectiveProtocolBalance(userWallet.walletId, token) ?: BigDecimal.ZERO

        if (plainBalance + yieldBalance < required) raise(GaslessError.NotEnoughFunds)

        // amount to withdraw = the shortfall, but withdraw at least the full fee per PDF (we withdraw `required`
        // minus what's already on the plain balance is enough; PDF withdraws the computed fee amount).
        val withdrawAmountDecimal = required - plainBalance
        val withdrawAmount = withdrawAmountDecimal.movePointRight(token.decimals).toBigInteger()

        val withdrawCallData = catch(
            block = {
                yieldSupplyTransactionRepository.createPartialWithdrawCallData(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = token,
                    amount = Amount(
                        token = Token(token.symbol, token.contractAddress, token.decimals),
                        value = withdrawAmountDecimal,
                    ),
                )
            },
            catch = { error ->
                when (error) {
                    is YieldModuleUpgradeUnavailableException,
                    is YieldModuleVersionIndeterminateException,
                    -> raise(GaslessError.ModuleUpdateUnavailable)
                    else -> raise(GaslessError.DataError(error))
                }
            },
        )

        val yieldModuleAddress = yieldSupplyTransactionRepository
            .getYieldContractAddress(userWallet.walletId, token)
            ?: raise(GaslessError.DataError(IllegalStateException("yield module address is null")))

        GaslessFeePlan.TokenPayWithYieldWithdraw(
            feeToken = token,
            fee = tokenFee,
            withdrawAmount = withdrawAmount,
            withdrawCallData = withdrawCallData,
            yieldModuleAddress = yieldModuleAddress,
        )
    }
}
```

> Note on `withdrawGasLimit`: the spec adds `withdrawGasLimit` to the fee gas before recomputing the token fee. Because `tokenFee` is passed in already-computed, the gas-limit augmentation lives in the caller (`GetFeeForGaslessUseCase`, Task 11) which estimates the withdraw gas and recomputes the fee before calling this resolver for the yield branch. This keeps the resolver pure decision logic and avoids a second wallet-manager dependency here. If the team prefers the estimation inside the resolver, inject `WalletManagersFacade` and estimate on `withdrawCallData` with the 10000-probe amount before recompute.

- [ ] **Step 4: Run tests, verify they pass**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*ResolveGaslessFeePlanUseCaseTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/ResolveGaslessFeePlanUseCase.kt \
        domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/ResolveGaslessFeePlanUseCaseTest.kt
git commit -m "AND-XXXX Add ResolveGaslessFeePlanUseCase decision tree"
```

### Task 8: `Eip712TypedDataBuilder.buildBatch`

**Files:**
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/Eip712TypedDataBuilder.kt`
- Test: `domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/Eip712TypedDataBuilderBatchTest.kt`

- [ ] **Step 1: Write the failing test** (assert structure of the batch typed data)

```kotlin
package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.domain.transaction.models.GaslessTransactionData
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class Eip712TypedDataBuilderBatchTest {

    @Test
    fun `buildBatch emits GaslessBatchTransaction primary type with transactions array`() {
        val tx = GaslessTransactionData.Transaction(to = "0xaaa", value = BigInteger.ZERO, data = byteArrayOf(0x12))
        val withdraw = GaslessTransactionData.Transaction(to = "0xbbb", value = BigInteger.ZERO, data = byteArrayOf(0x34))
        val fee = GaslessTransactionData.Fee(
            feeToken = "0xtoken", maxTokenFee = BigInteger.TEN, coinPriceInToken = BigInteger.ONE,
            feeTransferGasLimit = BigInteger.valueOf(100), baseGas = BigInteger.valueOf(60000), feeReceiver = "0xrecv",
        )
        val batch = GaslessBatchTransactionData(listOf(tx, withdraw), fee, BigInteger.ZERO)

        val json = JSONObject(Eip712TypedDataBuilder.buildBatch(batch, chainId = 1, verifyingContract = "0xuser"))

        assertThat(json.getString("primaryType")).isEqualTo("GaslessBatchTransaction")
        val message = json.getJSONObject("message")
        assertThat(message.getJSONArray("transactions").length()).isEqualTo(2)
        assertThat(message.getJSONArray("transactions").getJSONObject(1).getString("to")).isEqualTo("0xbbb")
        // GaslessBatchTransaction type declares transactions as Transaction[]
        val types = json.getJSONObject("types").getJSONArray("GaslessBatchTransaction")
        assertThat(types.getJSONObject(0).getString("type")).isEqualTo("Transaction[]")
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*Eip712TypedDataBuilderBatchTest*"`
Expected: FAIL — `buildBatch` unresolved.

- [ ] **Step 3: Add `buildBatch` to `Eip712TypedDataBuilder`** (reuse existing `typeProperty`, `buildDomain`; add batch type + message)

```kotlin
private const val PRIMARY_TYPE_BATCH = "GaslessBatchTransaction"

fun buildBatch(gaslessBatch: GaslessBatchTransactionData, chainId: Int, verifyingContract: String): String {
    val typedData = JSONObject().apply {
        put("types", buildBatchTypes())
        put("primaryType", PRIMARY_TYPE_BATCH)
        put("domain", buildDomain(chainId, verifyingContract))
        put("message", buildBatchMessage(gaslessBatch))
    }
    return typedData.toString()
}

@Suppress("NestedScopeFunctions")
private fun buildBatchTypes(): JSONObject {
    return JSONObject().apply {
        put("EIP712Domain", JSONArray().apply {
            put(typeProperty("name", "string"))
            put(typeProperty("version", "string"))
            put(typeProperty("chainId", "uint256"))
            put(typeProperty("verifyingContract", "address"))
        })
        put("Transaction", JSONArray().apply {
            put(typeProperty("to", "address"))
            put(typeProperty("value", "uint256"))
            put(typeProperty("data", "bytes"))
        })
        put("Fee", JSONArray().apply {
            put(typeProperty("feeToken", "address"))
            put(typeProperty("maxTokenFee", "uint256"))
            put(typeProperty("coinPriceInToken", "uint256"))
            put(typeProperty("feeTransferGasLimit", "uint256"))
            put(typeProperty("baseGas", "uint256"))
            put(typeProperty("feeReceiver", "address"))
        })
        put("GaslessBatchTransaction", JSONArray().apply {
            put(typeProperty("transactions", "Transaction[]"))
            put(typeProperty("fee", "Fee"))
            put(typeProperty("nonce", "uint256"))
        })
    }
}

@Suppress("NestedScopeFunctions")
private fun buildBatchMessage(gaslessBatch: GaslessBatchTransactionData): JSONObject {
    return JSONObject().apply {
        put("transactions", JSONArray().apply {
            gaslessBatch.transactions.forEach { tx ->
                put(JSONObject().apply {
                    put("to", tx.to)
                    put("value", tx.value.toString())
                    put("data", tx.data.toHexString())
                })
            }
        })
        put("fee", JSONObject().apply {
            put("feeToken", gaslessBatch.fee.feeToken)
            put("maxTokenFee", gaslessBatch.fee.maxTokenFee.toString())
            put("coinPriceInToken", gaslessBatch.fee.coinPriceInToken.toString())
            put("feeTransferGasLimit", gaslessBatch.fee.feeTransferGasLimit.toString())
            put("baseGas", gaslessBatch.fee.baseGas.toString())
            put("feeReceiver", gaslessBatch.fee.feeReceiver)
        })
        put("nonce", gaslessBatch.nonce.toString())
    }
}
```
(Add `import com.tangem.domain.transaction.models.GaslessBatchTransactionData`.)

- [ ] **Step 4: Run test, verify it passes**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*Eip712TypedDataBuilderBatchTest*"`
Expected: PASS.

> Verify against the gasless-service / contracts README that the EIP-712 batch type name is exactly `GaslessBatchTransaction` and the array field is `transactions: Transaction[]`. Adjust if the deployed contract differs — the signature must match what `executeBatchTransaction` (0xf9b181bf) verifies.

- [ ] **Step 5: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/Eip712TypedDataBuilder.kt \
        domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/Eip712TypedDataBuilderBatchTest.kt
git commit -m "AND-XXXX Add EIP-712 buildBatch for GaslessBatchTransaction"
```

---

## Phase 5 — APP data/API: batch endpoint

### Task 9: Batch request DTO

**Files:**
- Create: `core/datasource/src/main/java/com/tangem/datasource/api/gasless/models/GaslessBatchTransactionRequest.kt`

- [ ] **Step 1: Write the DTO** (reuses existing `FeeData`, `TransactionData`, `Eip7702AuthorizationDTO` from `GaslessTransactionRequest.kt`)

```kotlin
package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for a gasless BATCH transaction submission (executeBatchTransaction).
 */
@JsonClass(generateAdapter = true)
data class GaslessBatchTransactionRequest(
    @Json(name = "gaslessBatchTransaction")
    val gaslessBatchTransaction: GaslessBatchTransactionDataDTO,

    @Json(name = "signature")
    val signature: String,

    @Json(name = "userAddress")
    val userAddress: String,

    @Json(name = "chainId")
    val chainId: Int,

    @Json(name = "eip7702auth")
    val eip7702Auth: Eip7702AuthorizationDTO? = null,
)

@JsonClass(generateAdapter = true)
data class GaslessBatchTransactionDataDTO(
    @Json(name = "transactions")
    val transactions: List<TransactionData>,

    @Json(name = "fee")
    val fee: FeeData,

    @Json(name = "nonce")
    val nonce: String,
)
```

- [ ] **Step 2: Compile core/datasource** (KSP generates the Moshi adapter)

Run: `cd tangem-app-android && ./gradlew :core:datasource:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

> Confirm field names (`gaslessBatchTransaction`, `transactions`, `eip7702auth`) against the gasless-service README before merge.

- [ ] **Step 3: Commit**

```bash
cd tangem-app-android
git add core/datasource/src/main/java/com/tangem/datasource/api/gasless/models/GaslessBatchTransactionRequest.kt
git commit -m "AND-XXXX Add GaslessBatchTransactionRequest DTO"
```

### Task 10: Batch endpoint on the API + repository

**Files:**
- Modify: `core/datasource/src/main/java/com/tangem/datasource/api/gasless/GaslessTxServiceApi.kt`
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/GaslessTransactionRepository.kt`
- Modify: `data/transaction/src/main/java/com/tangem/data/transaction/convertes/GaslessTransactionRequestBuilder.kt`
- Modify: `data/transaction/src/main/java/com/tangem/data/transaction/DefaultGaslessTransactionRepository.kt`
- Test: `data/transaction/src/test/java/com/tangem/data/transaction/DefaultGaslessTransactionRepositoryTest.kt` (create if absent)

- [ ] **Step 1: Add the endpoint** to `GaslessTxServiceApi`

```kotlin
@POST("api/v1/transaction/batch/sign")
suspend fun signGaslessBatchTransaction(
    @Body transaction: GaslessBatchTransactionRequest,
): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>
```
(Confirm the exact path with the gasless-service README; `api/v1/transaction/sign` is the single-tx path.)

- [ ] **Step 2: Add the repository method** to `GaslessTransactionRepository`

```kotlin
suspend fun signGaslessBatchTransaction(
    gaslessBatchTransactionData: com.tangem.domain.transaction.models.GaslessBatchTransactionData,
    signature: String,
    userAddress: String,
    network: Network,
    eip7702Auth: Eip7702Authorization? = null,
): GaslessSignedTransactionResult
```

- [ ] **Step 3: Add a batch builder** to `GaslessTransactionRequestBuilder`

```kotlin
import com.tangem.datasource.api.gasless.models.GaslessBatchTransactionDataDTO
import com.tangem.datasource.api.gasless.models.GaslessBatchTransactionRequest
import com.tangem.datasource.api.gasless.models.TransactionData
import com.tangem.datasource.api.gasless.models.FeeData
import com.tangem.domain.transaction.models.GaslessBatchTransactionData
import com.tangem.common.extensions.toHexString

fun buildBatch(
    gaslessBatch: GaslessBatchTransactionData,
    signature: String,
    userAddress: String,
    chainId: Int,
    eip7702Auth: Eip7702Authorization? = null,
): GaslessBatchTransactionRequest {
    return GaslessBatchTransactionRequest(
        gaslessBatchTransaction = GaslessBatchTransactionDataDTO(
            transactions = gaslessBatch.transactions.map { tx ->
                TransactionData(
                    to = tx.to,
                    value = tx.value.toString(),
                    data = tx.data.toHexString().withHexPrefix(),
                )
            },
            fee = FeeData(
                feeToken = gaslessBatch.fee.feeToken,
                maxTokenFee = gaslessBatch.fee.maxTokenFee.toString(),
                coinPriceInToken = gaslessBatch.fee.coinPriceInToken.toString(),
                feeTransferGasLimit = gaslessBatch.fee.feeTransferGasLimit.toString(),
                baseGas = gaslessBatch.fee.baseGas.toString(),
                feeReceiver = gaslessBatch.fee.feeReceiver,
            ),
            nonce = gaslessBatch.nonce.toString(),
        ),
        signature = signature,
        userAddress = userAddress,
        chainId = chainId,
        eip7702Auth = eip7702Auth?.toDTO(),
    )
}
```
(Reuse the existing private `Eip7702Authorization.toDTO()` — make it accessible from `buildBatch`, e.g. keep both methods in the same class. Match the `data` hex encoding the existing single-tx `GaslessTxDataToGaslessRequestConverter` uses — open that converter and mirror its `to`/`value`/`data` formatting exactly, including 0x-prefix handling. Replace `withHexPrefix()` with the same helper the existing converter uses.)

- [ ] **Step 4: Implement in `DefaultGaslessTransactionRepository`** (mirror `signGaslessTransaction`)

```kotlin
override suspend fun signGaslessBatchTransaction(
    gaslessBatchTransactionData: GaslessBatchTransactionData,
    signature: String,
    userAddress: String,
    network: Network,
    eip7702Auth: Eip7702Authorization?,
): GaslessSignedTransactionResult = withContext(coroutineDispatcherProvider.io) {
    val blockchain = network.toBlockchain()
    val request = gaslessTransactionRequestBuilder.buildBatch(
        gaslessBatch = gaslessBatchTransactionData,
        signature = signature,
        userAddress = userAddress,
        chainId = blockchain.getChainId() ?: error("ChainId is null for blockchain: $blockchain"),
        eip7702Auth = eip7702Auth,
    )
    val response = gaslessTxServiceApi.signGaslessBatchTransaction(request).getOrThrow()
    if (!response.isSuccess) error("Gasless service returned unsuccessful response")
    signedTransactionResultConverter.convert(response.result)
}
```

- [ ] **Step 5: Write a repository test** (mock `GaslessTxServiceApi`, assert it calls the batch endpoint and returns the txHash)

```kotlin
@Test
fun `signGaslessBatchTransaction posts batch request and returns txHash`() = runTest {
    coEvery { api.signGaslessBatchTransaction(any()) } returns
        ApiResponse.Success(GaslessServiceResponse(GaslessSignedTransactionResultDTO("0xhash"), true, "t"))
    val result = repository.signGaslessBatchTransaction(batchData, "0xsig", "0xuser", network, null)
    assertThat(result.txHash).isEqualTo("0xhash")
    coVerify { api.signGaslessBatchTransaction(any()) }
}
```

- [ ] **Step 6: Run tests + compile**

Run: `cd tangem-app-android && ./gradlew :data:transaction:testDebugUnitTest --tests "*DefaultGaslessTransactionRepositoryTest*" :core:datasource:compileDebugKotlin`
Expected: PASS / BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
cd tangem-app-android
git add core/datasource/src/main/java/com/tangem/datasource/api/gasless/GaslessTxServiceApi.kt \
        domain/transaction/src/main/java/com/tangem/domain/transaction/GaslessTransactionRepository.kt \
        data/transaction/src/main/java/com/tangem/data/transaction/convertes/GaslessTransactionRequestBuilder.kt \
        data/transaction/src/main/java/com/tangem/data/transaction/DefaultGaslessTransactionRepository.kt \
        data/transaction/src/test/java/com/tangem/data/transaction/DefaultGaslessTransactionRepositoryTest.kt
git commit -m "AND-XXXX Add gasless batch sign endpoint, repository and request builder"
```

### Task 11: Batch branch in `CreateAndSendGaslessTransactionUseCase`

**Files:**
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/CreateAndSendGaslessTransactionUseCase.kt`
- Test: `domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/CreateAndSendGaslessTransactionUseCaseTest.kt` (create/extend)

The usecase already reads `fee: TransactionFeeExtended`. Branch on `fee.gaslessFeePlan`:
- `TokenPayWithYieldWithdraw` → build `[mainTx, withdrawTx]`, sign `buildBatch` EIP-712, call `signGaslessBatchTransaction`.
- else → existing single-tx path unchanged.

- [ ] **Step 1: Write the failing test** (batch path builds 2 transactions and calls the batch repo method)

```kotlin
@Test
fun `yield withdraw plan -- builds batch and calls batch endpoint`() = runTest {
    // arrange: fee.gaslessFeePlan = TokenPayWithYieldWithdraw(token, tokenFee, withdrawAmount, withdrawCallData, yieldModuleAddress)
    coEvery { gaslessTransactionRepository.signGaslessBatchTransaction(any(), any(), any(), any(), any()) } returns
        GaslessSignedTransactionResult(txHash = "0xhash")
    // stub signer to return 2 signatures, provider nonce/eip7702 as in existing tests

    val result = useCase(userWallet, txData, feeWithYieldPlan)

    assertThat(result.getOrNull()).isEqualTo("0xhash")
    coVerify { gaslessTransactionRepository.signGaslessBatchTransaction(
        match { it.transactions.size == 2 }, any(), any(), any(), any(),
    ) }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*CreateAndSendGaslessTransactionUseCaseTest*"`
Expected: FAIL.

- [ ] **Step 3: Implement the branch.** In `prepareGaslessContext`/`invoke`, when `fee.gaslessFeePlan is GaslessFeePlan.TokenPayWithYieldWithdraw`, build the withdraw transaction and a `GaslessBatchTransactionData`, then sign with `Eip712TypedDataBuilder.buildBatch` and send via `signGaslessBatchTransaction`. Concretely, add:

```kotlin
private fun buildWithdrawTransaction(
    plan: GaslessFeePlan.TokenPayWithYieldWithdraw,
): GaslessTransactionData.Transaction = GaslessTransactionData.Transaction(
    to = plan.yieldModuleAddress,
    value = java.math.BigInteger.ZERO,
    data = plan.withdrawCallData.data,
)
```

Refactor `signGaslessTransactionByUser` / `signAndSendTransactionOnBackend` to accept either a `GaslessTransactionData` or a `GaslessBatchTransactionData`. Minimal approach — branch in `invoke`:

```kotlin
suspend operator fun invoke(
    userWallet: UserWallet,
    transactionData: TransactionData,
    fee: TransactionFeeExtended,
): Either<SendTransactionError, String> = either {
    catch(
        block = {
            val uncompiledTxData = validateTransactionData(transactionData)
            when (val plan = fee.gaslessFeePlan) {
                is GaslessFeePlan.TokenPayWithYieldWithdraw ->
                    sendBatch(userWallet, uncompiledTxData, fee, plan)
                else ->
                    sendSingle(userWallet, uncompiledTxData, fee) // existing path extracted into sendSingle(...)
            }
        },
        catch = { raise(SendTransactionError.DataError(it.message)) },
    )
}
```

`sendBatch` mirrors the single path but:
- `gaslessBatchTransactionData = GaslessBatchTransactionData(transactions = listOf(mainTx, buildWithdrawTransaction(plan)), fee = builtFee, nonce = contractNonce)`
- `eip712Data = Eip712TypedDataBuilder.buildBatch(gaslessBatchTransactionData, chainId, verifyingContract = sourceAddress)`
- sign exactly as `signHashes` (eip712 + eip7702, two hashes one tap),
- send via `gaslessTransactionRepository.signGaslessBatchTransaction(...)`,
- `addPendingGaslessTransaction(...)` as today.

Extract the existing body of `prepareGaslessContext`/`signGaslessTransactionByUser`/`signAndSendTransactionOnBackend` into `sendSingle(...)` verbatim (no behavior change) so the single path is preserved.

- [ ] **Step 4: Run tests, verify they pass**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*CreateAndSendGaslessTransactionUseCaseTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/CreateAndSendGaslessTransactionUseCase.kt \
        domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/CreateAndSendGaslessTransactionUseCaseTest.kt
git commit -m "AND-XXXX Build and send GaslessBatchTransaction for yield withdraw plan"
```

---

## Phase 6 — Wiring, fee selection, toggle

### Task 12: Wire the resolver into `GetFeeForGaslessUseCase` + include yield-covered tokens

**Files:**
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/GetFeeForGaslessUseCase.kt`
- Modify: `domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/TokenFeeCalculator.kt` (only if the withdraw-gas augmentation is added here)
- Test: extend `domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/` tests

Goal: after `TokenFeeCalculator.calculateTokenFee` yields a `TransactionFeeExtended` with `Fee.Ethereum.TokenCurrency`, call `ResolveGaslessFeePlanUseCase` to attach a `gaslessFeePlan`, and — for the yield branch — recompute the fee gas including `withdrawGasLimit` (estimate gas on the wrapped withdraw calldata with the 10000-probe amount, add to `maxTokenFeeGas`) before building the final `Fee.Ethereum.TokenCurrency`.

- [ ] **Step 1: Inject `ResolveGaslessFeePlanUseCase`** into `GetFeeForGaslessUseCase` constructor.

- [ ] **Step 2: Extend `findTokensToPayFee`** to also include supported tokens whose **yield** balance can cover the fee (G5). Today it filters `value.amount == ZERO` out; change the filter so a token with zero plain balance but active yield + sufficient effective protocol balance remains eligible. Pull `getEffectiveProtocolBalance` from the yield repo (inject `YieldSupplyTransactionRepository`).

```kotlin
val candidates = networkCurrenciesStatuses
    .filter { it.currency is CryptoCurrency.Token }
    .filter { status ->
        val token = status.currency as CryptoCurrency.Token
        token.contractAddress.lowercase() in supportedGaslessTokens
    }
// keep tokens with plain balance OR (active yield with effective balance)
```

- [ ] **Step 3: After `calculateTokenFee`**, resolve the plan:

```kotlin
val isYieldActive = /* query YieldSupplyStatus for tokenForPayFeeStatus */
val plan = resolveGaslessFeePlanUseCase(
    userWallet = userWallet,
    tokenStatus = tokenForPayFeeStatus,
    tokenFee = feeInTokenCurrency, // Fee.Ethereum.TokenCurrency
    isYieldActive = isYieldActive,
    sendAmountInFeeToken = sendAmountInFeeToken, // ZERO unless fee token == sent token
).bind()
return@either tokenFeeExtended.copy(gaslessFeePlan = plan)
```
For `TokenPayWithYieldWithdraw`, recompute the fee with `withdrawGasLimit` added (see Task 7 note) and rebuild `Fee.Ethereum.TokenCurrency` so the displayed `maxTokenFee` already accounts for the withdraw gas.

- [ ] **Step 4: Add/adjust unit tests** for: token selectable via yield balance; returned `TransactionFeeExtended.gaslessFeePlan` is `TokenPayWithYieldWithdraw` when plain balance is short but yield covers.

- [ ] **Step 5: Run tests**

Run: `cd tangem-app-android && ./gradlew :domain:transaction:test --tests "*Gasless*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
cd tangem-app-android
git add domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/GetFeeForGaslessUseCase.kt \
        domain/transaction/src/main/java/com/tangem/domain/transaction/usecase/gasless/TokenFeeCalculator.kt \
        domain/transaction/src/test/java/com/tangem/domain/transaction/usecase/gasless/
git commit -m "AND-XXXX Resolve gasless fee plan with yield withdraw in GetFeeForGaslessUseCase"
```

### Task 13: DI wiring

**Files:**
- Modify: the Hilt module(s) that provide gasless usecases (search: `prov* GetFeeForGaslessUseCase` / `CreateAndSendGaslessTransactionUseCase`).

- [ ] **Step 1: Find the providers**

Run: `cd tangem-app-android && grep -rn "GetFeeForGaslessUseCase\|CreateAndSendGaslessTransactionUseCase" --include="*.kt" domain features core data | grep -i "provide\|Module"`
Expected: locate the `@Provides`/`@Module` (likely in a `domain/transaction` DI module).

- [ ] **Step 2: Add `ResolveGaslessFeePlanUseCase` provider** and pass it + `YieldSupplyTransactionRepository` into `GetFeeForGaslessUseCase`'s provider. Mirror the existing provider signature exactly.

- [ ] **Step 3: Compile the app**

Run: `cd tangem-app-android && ./gradlew :app:assembleGoogleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd tangem-app-android
git add -A
git commit -m "AND-XXXX Wire ResolveGaslessFeePlanUseCase via DI"
```

### Task 14: Feature toggle + Yield withdraw notification

**Files:**
- Modify: `core/config-toggles/src/main/assets/configs/feature_toggles_config.json`
- Modify: `features/send-v2/api/src/main/java/com/tangem/features/send/v2/api/SendFeatureToggles.kt` (+ `DefaultSendFeatureToggles.kt`)
- Modify: `features/send-v2/impl/src/main/java/com/tangem/features/send/v2/subcomponents/notifications/model/NotificationsModel.kt`

- [ ] **Step 1: Add the toggle** entry (mirror the format of `GASLESS_APPROVAL_ENABLED`)

```json
{ "name": "GASLESS_YIELD_WITHDRAW_ENABLED", "version": "5.XX" }
```
(Use the actual format/fields present in the file; set the target version per release planning. Keep it OFF for QA until verified.)

- [ ] **Step 2: Expose it** via `SendFeatureToggles.isGaslessYieldWithdrawEnabled` and gate the yield branch in `GetFeeForGaslessUseCase`/resolver call: when the toggle is off, do NOT consider yield balance (behave exactly as today). Thread the toggle into the usecase (inject the toggles interface or pass a boolean).

- [ ] **Step 3: Add a notification** in `NotificationsModel` shown when the resolved plan is `TokenPayWithYieldWithdraw` ("Часть комиссии будет выведена из Yield Mode"). Reuse the existing notification building pattern in that model; add a Lokalise string key (run `python3 lokalize.py` after adding the key per repo convention).

- [ ] **Step 4: Compile + run send-v2 tests**

Run: `cd tangem-app-android && ./gradlew :features:send-v2:impl:testDebugUnitTest :app:assembleGoogleDebug`
Expected: PASS / BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
cd tangem-app-android
git add -A
git commit -m "AND-XXXX Add GASLESS_YIELD_WITHDRAW_ENABLED toggle and yield withdraw notification"
```

### Task 15: End-to-end verification

- [ ] **Step 1: Full module test sweep**

Run:
```bash
cd blockchain-sdk-kotlin && ./gradlew :blockchain:testDebugUnitTest --tests "*YieldSupplyWithdraw*" --tests "*CallDataProviderFactory*"
cd ../tangem-app-android && ./gradlew :domain:transaction:test :data:transaction:testDebugUnitTest :data:yield-supply:testDebugUnitTest :core:datasource:compileDebugKotlin
```
Expected: all PASS.

- [ ] **Step 2: Detekt**

Run: `cd tangem-app-android && ./gradlew detekt detektMain`
Expected: no new violations (watch `UnsafeStringResourceUsage` for the notification string — use the `Safe` variant).

- [ ] **Step 3: Manual smoke (toggle ON, testnet)** — token with plain balance below fee but funds in Yield Mode: confirm the fee selector offers the token, the confirm screen shows the yield-withdraw notification, the send produces a batch transaction accepted by the gasless backend, and the history labels the fee transfer as "Gasless transaction fee".

- [ ] **Step 4: Final commit / PR**

```bash
cd tangem-app-android && git push -u origin feature/AND-XXXX_gasless_yield_interplay
cd ../blockchain-sdk-kotlin && git push -u origin feature/AND-XXXX_gasless_yield_interplay
```
Open PRs in both repos targeting `develop`.

---

## Self-Review Notes (coverage of spec §3)

- §3.1 models → Tasks 3, 4, 5.
- §3.2 ResolveGaslessFeePlanUseCase + G5 selection → Tasks 7, 12.
- §3.3 send batch branch → Task 11.
- §3.4 EIP-712 buildBatch → Task 8.
- §3.5 data/API batch → Tasks 9, 10.
- §3.6 SDK withdraw calldata + upgrade-wrap reuse → Tasks 1, 2, 6.
- §3.7 wiring + notification + toggle → Tasks 13, 14.
- §4 error handling (`ModuleUpdateUnavailable`, fallback) → Tasks 4, 7, 14.
- §5 testing → tests in every task.
- §6 open questions surfaced as explicit verification steps (Tasks 1/8/9/10) against contracts/backend README.

## Known external dependencies to confirm before merge
1. `withdraw(address,uint256)` selector (`0xf3fef3a3`) vs deployed contract ABI (Task 1).
2. Batch endpoint path + DTO field names from gasless-service README (Tasks 9, 10).
3. EIP-712 batch type name `GaslessBatchTransaction` / `transactions: Transaction[]` vs contract (Task 8).
4. `data` hex formatting parity with existing `GaslessTxDataToGaslessRequestConverter` (Task 10).