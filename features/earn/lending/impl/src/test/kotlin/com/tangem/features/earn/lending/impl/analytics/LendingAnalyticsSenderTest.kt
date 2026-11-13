package com.tangem.features.earn.lending.impl.analytics

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.lending.impl.provider.LendingTestTransactions.ethFee
import com.tangem.features.earn.lending.impl.session.LendingPlan
import com.tangem.features.earn.lending.impl.session.LendingPlannedOperation
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.Withdraw
import com.tangem.features.earn.product.api.session.WithdrawMode
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
        fun `GIVEN usd price known WHEN onSent THEN sends signed with amount bucket and then submitted`() = runTest {
            // Arrange
            coEvery { getCurrencyUSDQuoteUseCase(eth.id.rawCurrencyId!!) } returns BigDecimal("3000")
            val transaction = LendingAnalyticsTransaction(LendingAnalyticsTransaction.Type.Deposit, isWithUpgrade = true)

            // Act
            sender.onSent(plan = depositPlan(amount = BigDecimal("0.2"), isWithUpgrade = true), currency = eth)

            // Assert
            verifyOrder {
                analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSigned(transaction, amountBucket = "250-1K"))
                analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSubmitted(transaction))
            }
        }

        @Test
        fun `GIVEN usd price unknown WHEN onSent THEN signed goes without amount bucket`() = runTest {
            // Arrange
            coEvery { getCurrencyUSDQuoteUseCase(eth.id.rawCurrencyId!!) } returns null
            val transaction =
                LendingAnalyticsTransaction(LendingAnalyticsTransaction.Type.Withdraw, isWithUpgrade = false)

            // Act
            sender.onSent(plan = withdrawPlan(), currency = eth)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSigned(transaction, amountBucket = null))
            }
            verify(exactly = 1) { analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSubmitted(transaction)) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSendFailed {

        @Test
        fun `GIVEN send error WHEN onSendFailed THEN sends transaction failed with its error code`() {
            // Arrange
            val error = SendTransactionError.NetworkError(message = "rejected", code = "500")
            val transaction =
                LendingAnalyticsTransaction(LendingAnalyticsTransaction.Type.Deposit, isWithUpgrade = false)

            // Act
            sender.onSendFailed(plan = depositPlan(), error = error)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    LendingAnalyticsEvent.TransactionFailed(transaction, errorCode = "NetworkError: 500"),
                )
            }
        }

        @Test
        fun `GIVEN user cancelled on the card WHEN onSendFailed THEN sends nothing`() {
            // Act
            sender.onSendFailed(plan = depositPlan(), error = SendTransactionError.UserCancelledError)

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        }
    }

    private fun depositPlan(amount: BigDecimal = BigDecimal.ONE, isWithUpgrade: Boolean = false) = LendingPlan(
        fee = ethFee(BigInteger.ONE),
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