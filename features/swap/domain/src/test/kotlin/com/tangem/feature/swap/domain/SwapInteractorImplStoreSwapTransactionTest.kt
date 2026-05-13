package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SavedSwapTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for [SwapInteractorImpl.storeSwapTransaction].
 *
 * Behavior:
 *  - Delegates to [SwapTransactionRepository.storeTransaction] with fields derived from
 *    the from/to currency statuses, the amount, the provider, and the [SwapDataModel].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplStoreSwapTransactionTest : SwapInteractorImplTestBase() {

    @BeforeEach
    fun resetSwapTransactionRepository() {
        clearMocks(swapTransactionRepository)
    }

    @Test
    fun `should delegate to swapTransactionRepository storeTransaction with correct fields`() = runTest {
        // Given
        val fromStatus = buildSwapCurrencyStatus(
            networkRawId = Blockchain.Ethereum.toNetworkId(),
            isCoin = true,
        )
        val toStatus = buildSwapCurrencyStatus(
            networkRawId = Blockchain.Bitcoin.toNetworkId(),
            isCoin = true,
        )
        val amount = SwapAmount(value = BigDecimal("1.25"), decimals = 18)
        val provider = buildSwapProvider(type = ExchangeProviderType.DEX, providerId = "dex-store")
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.42"), 8),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.25"), 18),
                toAmount = SwapAmount(BigDecimal("0.42"), 8),
                txValue = "0",
                txId = "persisted-tx-id",
                txTo = "0xRecipient",
                txExtraId = null,
                txFrom = "0xSender",
                txData = "dGVzdA==",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
            ),
        )
        val timestamp = 1_700_000_000L

        val transactionSlot = slot<SavedSwapTransactionModel>()

        // When
        sut.storeSwapTransaction(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            amount = amount,
            swapProvider = provider,
            swapDataModel = swapDataModel,
            timestamp = timestamp,
            txExternalUrl = "https://explorer/tx",
            txExternalId = "ext-id-1",
            averageDuration = 120,
        )

        // Then
        coVerify(exactly = 1) {
            swapTransactionRepository.storeTransaction(
                fromUserWalletId = any(),
                toUserWalletId = any(),
                fromCryptoCurrency = any(),
                toCryptoCurrency = any(),
                fromAccount = any(),
                toAccount = any(),
                transaction = capture(transactionSlot),
            )
        }

        val captured = transactionSlot.captured
        assertThat(captured.txId).isEqualTo("persisted-tx-id")
        assertThat(captured.provider).isEqualTo(provider)
        assertThat(captured.timestamp).isEqualTo(timestamp)
        assertThat(captured.fromCryptoAmount).isEqualTo(BigDecimal("1.25"))
        assertThat(captured.toCryptoAmount).isEqualTo(BigDecimal("0.42"))
        val status = requireNotNull(captured.status) { "status should not be null" }
        assertThat(status.providerId).isEqualTo("dex-store")
        assertThat(status.status).isEqualTo(ExchangeStatus.New)
        assertThat(status.txExternalUrl).isEqualTo("https://explorer/tx")
        assertThat(status.txExternalId).isEqualTo("ext-id-1")
        assertThat(status.averageDuration).isEqualTo(120)
    }

    @Test
    fun `should accept null txExternalUrl and txExternalId and averageDuration`() = runTest {
        // Given
        val fromStatus = buildSwapCurrencyStatus()
        val toStatus = buildSwapCurrencyStatus()
        val amount = SwapAmount(BigDecimal("0.5"), 18)
        val provider = buildSwapProvider()
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.1"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("0.5"), 18),
                toAmount = SwapAmount(BigDecimal("0.1"), 18),
                txValue = "0",
                txId = "tx-id-2",
                txTo = "0xRecipient",
                txExtraId = null,
                txFrom = "0xSender",
                txData = "dGVzdA==",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
            ),
        )

        val transactionSlot = slot<SavedSwapTransactionModel>()

        // When
        sut.storeSwapTransaction(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
            amount = amount,
            swapProvider = provider,
            swapDataModel = swapDataModel,
            timestamp = 1L,
            txExternalUrl = null,
            txExternalId = null,
            averageDuration = null,
        )

        // Then
        coVerify(exactly = 1) {
            swapTransactionRepository.storeTransaction(
                fromUserWalletId = any(),
                toUserWalletId = any(),
                fromCryptoCurrency = any(),
                toCryptoCurrency = any(),
                fromAccount = any(),
                toAccount = any(),
                transaction = capture(transactionSlot),
            )
        }
        val status = requireNotNull(transactionSlot.captured.status)
        assertThat(status.txExternalUrl).isNull()
        assertThat(status.txExternalId).isNull()
        assertThat(status.averageDuration).isNull()
    }
}