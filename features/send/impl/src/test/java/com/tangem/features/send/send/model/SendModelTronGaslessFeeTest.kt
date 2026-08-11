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
import com.tangem.domain.models.network.Network
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

    private val tronNetwork: Network = mockk(relaxed = true)
    private val tronToken: CryptoCurrency.Token = mockk(relaxed = true) {
        every { network } returns tronNetwork
    }
    private val tronCoinId: CryptoCurrency.ID = mockk(relaxed = true)
    private val tronCoin: CryptoCurrency.Coin = mockk(relaxed = true) {
        every { network } returns tronNetwork
        every { id } returns tronCoinId
    }
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
        stubNativeBalance(
            balance = model.nativeBalance,
            feePaidCurrency = if (model.feePaidFallsBackToSentToken) tronToken else tronCoin,
        )
        coEvery { getFeeForGaslessUseCase(any(), any(), any(), any()) } returns nativeFee(model).right()
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
        /** Non-null builds a [Fee.Tron] carrying the account's energy, as the SDK reports it. */
        val remainingEnergy: Long? = null,
        val feeEnergy: Long? = null,
        /** The fee-paid currency could not be resolved, so the status holds the sent token. */
        val feePaidFallsBackToSentToken: Boolean = false,
    )

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        // The account's free bandwidth and (often delegated) energy cover the transfer, so the SDK
        // charges nothing. Nothing is covered by the wallet itself, so the sent token is quoted
        // whatever the TRX balance is — empty, dust, or enough for a real fee.
        TestModel(nativeFee = BigDecimal.ZERO, nativeBalance = BigDecimal.ZERO, expectsGasless = true),
        TestModel(nativeFee = BigDecimal.ZERO, nativeBalance = BigDecimal("0.00004"), expectsGasless = true),
        TestModel(nativeFee = BigDecimal.ZERO, nativeBalance = BigDecimal("10"), expectsGasless = true),
        // No energy delegated: the fee is burned in TRX the wallet does not have.
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal.ZERO, expectsGasless = true),
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal("0.00004"), expectsGasless = true),
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal("1.5"), expectsGasless = true),
        // The wallet can pay in TRX — keep the native fee.
        TestModel(nativeFee = BigDecimal("6.4285"), nativeBalance = BigDecimal("10"), expectsGasless = false),
        // Delegated energy still on the address after a gasless send: the SDK only charges the
        // bandwidth burn, which a dust TRX balance happens to cover. The wallet cannot pay the real
        // price of the transfer once the delegation is reclaimed, so the sent token is quoted.
        TestModel(
            nativeFee = BigDecimal("0.345"),
            nativeBalance = BigDecimal("0.346"),
            expectsGasless = true,
            remainingEnergy = 173_571,
            feeEnergy = 64_285,
        ),
        // Same, with the balance well above the discounted fee — still not the price it would pay.
        TestModel(
            nativeFee = BigDecimal("0.345"),
            nativeBalance = BigDecimal("10"),
            expectsGasless = true,
            remainingEnergy = 173_571,
            feeEnergy = 64_285,
        ),
        // Energy only partially covers the call: the fee is still discounted by what is delegated.
        TestModel(
            nativeFee = BigDecimal("3.2"),
            nativeBalance = BigDecimal("10"),
            expectsGasless = true,
            remainingEnergy = 30_000,
            feeEnergy = 64_285,
        ),
        // No energy on the account: the fee is the full burn, and the wallet can pay it.
        TestModel(
            nativeFee = BigDecimal("6.4285"),
            nativeBalance = BigDecimal("10"),
            expectsGasless = false,
            remainingEnergy = 0,
            feeEnergy = 64_285,
        ),
        // …but not when the balance falls short of it.
        TestModel(
            nativeFee = BigDecimal("6.4285"),
            nativeBalance = BigDecimal("1.5"),
            expectsGasless = true,
            remainingEnergy = 0,
            feeEnergy = 64_285,
        ),
        // TRX is missing from the portfolio, so the fee-paid status falls back to the sent token: the
        // USDT balance must not be read as TRX the wallet could pay the fee with.
        TestModel(
            nativeFee = BigDecimal("6.4285"),
            nativeBalance = BigDecimal("27.85"),
            expectsGasless = true,
            remainingEnergy = 0,
            feeEnergy = 64_285,
            feePaidFallsBackToSentToken = true,
        ),
    )

    override fun defaultSendParams() = super.defaultSendParams().copy(currency = tronToken)

    private fun stubNativeBalance(balance: BigDecimal, feePaidCurrency: CryptoCurrency = tronCoin) {
        val nativeStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
            every { currency } returns feePaidCurrency
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

    private fun nativeFee(model: TestModel): TransactionFeeExtended {
        val amount = Amount(model.nativeFee, Blockchain.Tron)
        val fee = if (model.remainingEnergy != null && model.feeEnergy != null) {
            Fee.Tron(amount = amount, remainingEnergy = model.remainingEnergy, feeEnergy = model.feeEnergy)
        } else {
            Fee.Common(amount)
        }
        return TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = fee),
            feeTokenId = tronCoinId,
        )
    }

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