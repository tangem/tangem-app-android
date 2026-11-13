package com.tangem.features.earn.lending.impl.analytics

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee
import com.tangem.features.earn.lending.impl.session.LendingOperation
import com.tangem.features.earn.lending.impl.session.LendingPlan
import com.tangem.features.earn.lending.impl.session.LendingPlannedOperation
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

internal class LendingAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase = mockk()
    private val eth = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    private val sender = LendingAnalyticsSender(
        analyticsEventHandler = analyticsEventHandler,
        getCurrencyUSDQuoteUseCase = getCurrencyUSDQuoteUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, getCurrencyUSDQuoteUseCase)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSent {

        @Test
        fun `GIVEN usd price known WHEN onSent THEN sends signed with amount bucket, submitted and transaction sent`() =
            runTest {
                // Arrange
                coEvery { getCurrencyUSDQuoteUseCase(eth.id.rawCurrencyId!!) } returns BigDecimal("3000")

                // Act
                sender.onSent(
                    plan = depositPlan(amount = BigDecimal("0.2"), isWithUpgrade = true),
                    operation = LendingOperation.Deposit,
                    currency = eth,
                )

                // Assert
                verifyOrder {
                    analyticsEventHandler.send(
                        LendingAnalyticsEvent.TransactionSigned(
                            operation = LendingOperation.Deposit,
                            isWithUpgrade = true,
                            amountBucket = "250-1K",
                        ),
                    )
                    analyticsEventHandler.send(
                        LendingAnalyticsEvent.TransactionSubmitted(LendingOperation.Deposit, isWithUpgrade = true),
                    )
                    analyticsEventHandler.send(expectedTransactionSent())
                }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee picked in the selector WHEN onSent THEN transaction sent reports its fee type`(
            model: FeeTypeModel,
        ) = runTest {
            // Arrange
            coEvery { getCurrencyUSDQuoteUseCase(eth.id.rawCurrencyId!!) } returns null

            // Act
            sender.onSent(
                plan = depositPlan(feeType = model.feeType),
                operation = LendingOperation.Deposit,
                currency = eth,
            )

            // Assert
            val sent = mutableListOf<AnalyticsEvent>()
            verify { analyticsEventHandler.send(capture(sent)) }
            val transactionSent = sent.filterIsInstance<Basic.TransactionSent>().single()
            assertThat(transactionSent.params["Fee Type"]).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FeeTypeModel(feeType = AnalyticsParam.FeeType.Fixed, expected = "Fixed"),
            FeeTypeModel(feeType = AnalyticsParam.FeeType.Min, expected = "Min"),
            FeeTypeModel(feeType = AnalyticsParam.FeeType.Normal, expected = "Normal"),
            FeeTypeModel(feeType = AnalyticsParam.FeeType.Max, expected = "Max"),
            FeeTypeModel(feeType = AnalyticsParam.FeeType.Custom, expected = "Custom"),
        )

        @Test
        fun `GIVEN usd price unknown WHEN onSent THEN signed goes without amount bucket`() = runTest {
            // Arrange
            coEvery { getCurrencyUSDQuoteUseCase(eth.id.rawCurrencyId!!) } returns null

            // Act
            sender.onSent(plan = withdrawPlan(), operation = LendingOperation.Withdraw, currency = eth)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    LendingAnalyticsEvent.TransactionSigned(
                        operation = LendingOperation.Withdraw,
                        isWithUpgrade = false,
                        amountBucket = null,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN currency without raw id WHEN onSent THEN signed goes without amount bucket and no quote is read`() =
            runTest {
                // Arrange
                val currency = eth.copy(
                    id = CryptoCurrency.ID(
                        prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                        body = CryptoCurrency.ID.Body.NetworkId(rawId = "ethereum"),
                        suffix = CryptoCurrency.ID.Suffix.ContractAddress(contractAddress = "0x1"),
                    ),
                )

                // Act
                sender.onSent(plan = depositPlan(), operation = LendingOperation.Deposit, currency = currency)

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        LendingAnalyticsEvent.TransactionSigned(
                            operation = LendingOperation.Deposit,
                            isWithUpgrade = false,
                            amountBucket = null,
                        ),
                    )
                }
                coVerify(exactly = 0) { getCurrencyUSDQuoteUseCase(any()) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSendFailed {

        @Test
        fun `GIVEN send error WHEN onSendFailed THEN sends transaction failed with its error code`() {
            // Arrange
            val error = SendTransactionError.NetworkError(message = "rejected", code = "500")

            // Act
            sender.onSendFailed(plan = depositPlan(), operation = LendingOperation.Deposit, error = error)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    LendingAnalyticsEvent.TransactionFailed(
                        operation = LendingOperation.Deposit,
                        isWithUpgrade = false,
                        errorCode = "NetworkError: 500",
                    ),
                )
            }
        }

        @Test
        fun `GIVEN user cancelled on the card WHEN onSendFailed THEN sends nothing`() {
            // Act
            sender.onSendFailed(
                plan = depositPlan(),
                operation = LendingOperation.Deposit,
                error = SendTransactionError.UserCancelledError,
            )

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }
    }

    data class FeeTypeModel(val feeType: AnalyticsParam.FeeType, val expected: String)

    private fun expectedTransactionSent() = Basic.TransactionSent(
        sentFrom = AnalyticsParam.TxSentFrom.Earning(
            blockchain = "Ethereum",
            token = "ETH",
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = "ETH",
            feeAssetType = AnalyticsParam.FeeAssetType.Coin,
        ),
        memoType = Basic.TransactionSent.MemoType.Null,
        isCustomToken = false,
        isCustomFeeToken = false,
    )

    private fun depositPlan(
        amount: BigDecimal = BigDecimal.ONE,
        isWithUpgrade: Boolean = false,
        feeType: AnalyticsParam.FeeType = AnalyticsParam.FeeType.Normal,
    ) = LendingPlan(
        fee = ethFee(BigInteger.ONE),
        feeType = feeType,
        expectedAmount = amount,
        rows = emptyList(),
        withdrawMode = null,
        action = Deposit(),
        operation = LendingPlannedOperation.Deposit,
        amount = amount,
        txs = emptyList(),
        planKey = "plan",
        isWithUpgrade = isWithUpgrade,
    )

    private fun withdrawPlan() = LendingPlan(
        fee = ethFee(BigInteger.ONE),
        feeType = AnalyticsParam.FeeType.Normal,
        expectedAmount = BigDecimal.ONE,
        rows = emptyList(),
        withdrawMode = WithdrawMode.Instant,
        action = Withdraw(),
        operation = LendingPlannedOperation.Withdraw(isEntire = false),
        amount = BigDecimal.ONE,
        txs = emptyList(),
        planKey = "plan",
        isWithUpgrade = false,
    )
}