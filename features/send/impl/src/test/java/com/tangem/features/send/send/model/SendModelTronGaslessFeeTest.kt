package com.tangem.features.send.send.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.send.SendModelTestBase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Covers the default fee-token choice on the Tron gasless path (`loadFeeExtended(null)`), where a
 * native fee of zero must not be mistaken for "the wallet can pay it itself" ([REDACTED_TASK_KEY]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendModelTronGaslessFeeTest : SendModelTestBase() {

    private val tronToken: CryptoCurrency.Token = mockk(relaxed = true)
    private val tronCoinId: CryptoCurrency.ID = mockk(relaxed = true)
    private val gaslessQuote: TronGaslessQuote = mockk(relaxed = true)

    @BeforeEach
    fun setUpTronGasless() {
        // PER_CLASS reuses one instance across the parameterized rows, so recorded calls would
        // otherwise accumulate and break the per-row coVerify below.
        clearMocks(getTronGaslessFeeUseCase, answers = false, recordedCalls = true, childMocks = false)

        every { sendFeatureToggles.isTronGaslessEnabled } returns true
        coEvery { isTronGaslessSupportedUseCase(any(), any()) } returns true
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns gaslessFee().right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun loadFeeExtended(model: TestModel) = runTest {
        // Arrange
        stubNativeBalance(model.nativeBalance)
        coEvery { getFeeForGaslessUseCase(any(), any(), any()) } returns nativeFee(model.nativeFee).right()
        val sendModel = createSendModel(this, MutableParamsContainer(defaultSendParams()))
        advanceUntilIdle()
        sendModel.predefinedValues = deeplink(amount = "1.0")

        // Act
        val result = sendModel.loadFeeExtended(maybeToken = null)

        // Assert
        assertThat(result.getOrNull()?.tronGaslessQuote).isEqualTo(if (model.expectsGasless) gaslessQuote else null)
        coVerify(exactly = if (model.expectsGasless) 1 else 0) { getTronGaslessFeeUseCase(any(), any()) }
    }

    internal data class TestModel(
        val nativeFee: BigDecimal,
        val nativeBalance: BigDecimal,
        val expectsGasless: Boolean,
    )

    private fun provideTestModels() = listOf(
        // The account's free bandwidth and (often delegated) energy cover the transfer, so the SDK
        // charges nothing — a wallet holding no TRX must still be quoted in the sent token.
        TestModel(nativeFee = BigDecimal.ZERO, nativeBalance = BigDecimal.ZERO, expectsGasless = true),
        // Dust left on the account does not make it able to pay a real fee either.
        TestModel(nativeFee = BigDecimal.ZERO, nativeBalance = BigDecimal("0.000003"), expectsGasless = false),
        // No energy delegated: the fee is burned in TRX the wallet does not have.
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal.ZERO, expectsGasless = true),
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal("1.5"), expectsGasless = true),
        // The wallet can pay in TRX — keep the native fee.
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal("10"), expectsGasless = false),
    )

    override fun defaultSendParams() = super.defaultSendParams().copy(currency = tronToken)

    private fun stubNativeBalance(balance: BigDecimal) {
        val nativeStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
            every { value } returns mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
                every { amount } returns balance
            }
        }
        val accountStatus: AccountCryptoCurrencyStatus = mockk(relaxed = true) {
            every { component1() } returns mockk(relaxed = true)
            every { component2() } returns nativeStatus
        }
        every { getAccountCurrencyStatusUseCase(any(), any<CryptoCurrency>()) } returns flowOf(accountStatus)
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns nativeStatus.right()
    }

    private fun nativeFee(value: BigDecimal) = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(value, Blockchain.Tron))),
        feeTokenId = tronCoinId,
    )

    private fun gaslessFee() = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal("2.4102"), Blockchain.Tron))),
        feeTokenId = tronToken.id,
        tronGaslessQuote = gaslessQuote,
    )

    private fun deeplink(amount: String) = PredefinedValues.Content.Deeplink(
        amount = amount,
        address = "addr123",
        memo = null,
        transactionId = "tx123",
    )
}