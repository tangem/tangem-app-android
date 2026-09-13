package com.tangem.features.send.send.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.send.SendModelTestBase
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
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendModelTronGaslessFeeTest : SendModelTestBase() {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val sentUsdt = currencyFactory.createToken(Blockchain.Tron, id = "usdt", contractAddress = "TUsdt")
    private val pickedUsdc = currencyFactory.createToken(Blockchain.Tron, id = "usdc", contractAddress = "TUsdc")
    private val gaslessQuote: TronGaslessQuote = mockk(relaxed = true)

    @BeforeEach
    fun setUpTronGasless() {
        clearMocks(getTronGaslessFeeUseCase, getFeeForGaslessUseCase, answers = false, recordedCalls = true)
        every { sendFeatureToggles.isTronGaslessEnabled } returns true
        coEvery { isTronGaslessSupportedUseCase(any(), any(), any()) } returns true
        coEvery { getTronGaslessFeeUseCase(any(), any()) } returns gaslessFee().right()
        coEvery { getFeeForGaslessUseCase(any(), any(), any(), any()) } returns nativeFee().right()
        stubBalances(sentBalance = BigDecimal("100"))
    }

    private fun stubBalances(sentBalance: BigDecimal) {
        val sentStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
            every { currency } returns sentUsdt
            every { value } returns mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
                every { amount } returns sentBalance
            }
        }
        val accountStatus: AccountCryptoCurrencyStatus = mockk(relaxed = true) {
            every { component1() } returns mockk(relaxed = true)
            every { component2() } returns sentStatus
        }
        every { getAccountCurrencyStatusUseCase(any(), any<CryptoCurrency>()) } returns flowOf(accountStatus)
    }

    @Test
    fun `GIVEN gasless token picked explicitly WHEN loadFeeExtended THEN it is quoted in that token`() = runTest {
        // Arrange
        val pickedToken: CryptoCurrencyStatus = mockk(relaxed = true) {
            every { currency } returns pickedUsdc
        }
        val sendModel = createSendModel(this)
        advanceUntilIdle()
        sendModel.predefinedValues = deeplink(amount = "1.0")

        // Act
        val result = sendModel.loadFeeExtended(maybeToken = pickedToken)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(gaslessFee())
        coVerify(exactly = 1) { getTronGaslessFeeUseCase(any(), pickedUsdc) }
        coVerify(exactly = 0) { getFeeForGaslessUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN no fee token picked WHEN loadFeeExtended THEN the sent token is quoted by default`() = runTest {
        // Arrange
        val sendModel = createSendModel(this)
        advanceUntilIdle()
        sendModel.predefinedValues = deeplink(amount = "1.0")

        // Act
        val result = sendModel.loadFeeExtended(maybeToken = null)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(gaslessFee())
        coVerify(exactly = 1) { getTronGaslessFeeUseCase(any(), sentUsdt) }
        coVerify(exactly = 0) { getFeeForGaslessUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN balance covers the fee but not the amount and the fee WHEN loadFeeExtended THEN gasless is kept`() =
        runTest {
            // Arrange
            stubBalances(sentBalance = BigDecimal("5"))
            val sendModel = createSendModel(this)
            advanceUntilIdle()
            sendModel.predefinedValues = deeplink(amount = "5.0")

            // Act
            val result = sendModel.loadFeeExtended(maybeToken = null)

            // Assert
            assertThat(result.getOrNull()).isEqualTo(gaslessFee())
            coVerify(exactly = 0) { getFeeForGaslessUseCase(any(), any(), any(), any()) }
        }

    override fun defaultSendParams() = super.defaultSendParams().copy(currency = sentUsdt)

    private fun nativeFee() = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(
            normal = Fee.Common(Amount(BigDecimal("6.4285"), Blockchain.Tron)),
        ),
        feeTokenId = currencyFactory.createCoin(Blockchain.Tron).id,
    )

    private fun gaslessFee() = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(normal = Fee.Common(Amount(BigDecimal("2.4102"), Blockchain.Tron))),
        feeTokenId = sentUsdt.id,
        tronGaslessQuote = gaslessQuote,
    )

    private fun deeplink(amount: String) = PredefinedValues.Content.Deeplink(
        amount = amount,
        address = "addr123",
        memo = null,
        transactionId = "tx123",
    )
}