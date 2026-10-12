package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.MockExpressTransactionFactory.createExchange
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.MockExpressTransactionFactory.createOnramp
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class ExpressStatusFactoryTest {

    private val clickIntents: ExpressTransactionsClickIntents = mockk()
    private val userWallet: UserWallet = mockk()
    private val cryptoCurrency: CryptoCurrency = mockk()
    private val analyticsEventsHandler: AnalyticsEventHandler = mockk()
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles = mockk()
    private val exchangeStatusFactoryCreator: ExchangeStatusFactory.Factory = mockk()
    private val exchangeStatusFactory: ExchangeStatusFactory = mockk()
    private val onrampStatusFactoryCreator: OnrampStatusFactory.Factory = mockk()
    private val onrampStatusFactory: OnrampStatusFactory = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val currentState = ExpressTransactionsBlockState(
        transactions = persistentListOf(),
        transactionsToDisplay = persistentListOf(),
        bottomSheetSlot = null,
    )

    private val accountScope = ExpressTransactionsComponent.Scope.Account(
        accountId = AccountId.forMainCryptoPortfolio(userWalletId = MockAccounts.userWalletId),
        maxCount = 2,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            txHistoryFeatureToggles,
            exchangeStatusFactoryCreator,
            exchangeStatusFactory,
            onrampStatusFactoryCreator,
            onrampStatusFactory,
        )
        every {
            exchangeStatusFactoryCreator.create(any(), any(), any(), any(), any(), any())
        } returns exchangeStatusFactory
        every {
            onrampStatusFactoryCreator.create(any(), any(), any(), any(), any(), any())
        } returns onrampStatusFactory
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetExpressStatuses {

        @Test
        fun `GIVEN account scope and 3 swaps WHEN getExpressStatuses THEN emits 2 newest in descending order`() =
            runTest {
                // Arrange
                val oldest = createExchange(txId = "oldest", timestamp = 100L)
                val newest = createExchange(txId = "newest", timestamp = 300L)
                val middle = createExchange(txId = "middle", timestamp = 200L)
                every { exchangeStatusFactory.invoke() } returns flowOf(persistentListOf(oldest, newest, middle))
                val factory = createFactory(scope = accountScope)

                // Act
                val actual = getEmittedValues(factory.getExpressStatuses())

                // Assert
                assertThat(actual).containsExactly(listOf(newest, middle))
            }

        @Test
        fun `GIVEN account scope WHEN getExpressStatuses THEN onramp factory not invoked`() = runTest {
            // Arrange
            every { exchangeStatusFactory.invoke() } returns flowOf(persistentListOf(createExchange()))
            val factory = createFactory(scope = accountScope)

            // Act
            getEmittedValues(factory.getExpressStatuses())

            // Assert
            verify { onrampStatusFactoryCreator wasNot Called }
            verify { onrampStatusFactory wasNot Called }
        }

        @Test
        fun `GIVEN currency scope WHEN getExpressStatuses THEN exchanges and onramps merged by timestamp desc`() =
            runTest {
                // Arrange
                val olderExchange = createExchange(txId = "older-exchange", timestamp = 100L)
                val newerExchange = createExchange(txId = "newer-exchange", timestamp = 300L)
                val olderOnramp = createOnramp(txId = "older-onramp", timestamp = 200L)
                val newerOnramp = createOnramp(txId = "newer-onramp", timestamp = 400L)
                every { exchangeStatusFactory.invoke() } returns flowOf(persistentListOf(olderExchange, newerExchange))
                every { onrampStatusFactory.invoke() } returns flowOf(listOf(olderOnramp, newerOnramp))
                val factory = createFactory(scope = ExpressTransactionsComponent.Scope.Currency)

                // Act
                val actual = getEmittedValues(factory.getExpressStatuses())

                // Assert
                assertThat(actual).containsExactly(listOf(newerOnramp, newerExchange, olderOnramp, olderExchange))
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetStateWithUpdatedExpressTxs {

        private val exchange = createExchange(txId = "exchange")
        private val visibleOnramp = createOnramp(txId = "visible-onramp", activeStatus = OnrampStatus.Status.Sending)
        private val hiddenOnramp = createOnramp(txId = "hidden-onramp", activeStatus = OnrampStatus.Status.Created)
        private val expressTxs = persistentListOf(exchange, visibleOnramp, hiddenOnramp)

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN scope and new tx history toggle WHEN getStateWithUpdatedExpressTxs THEN expected txs displayed`(
            model: TransactionsToDisplayModel,
        ) {
            // Arrange
            every { txHistoryFeatureToggles.isNewTxHistoryEnabled } returns model.isNewTxHistoryEnabled
            val factory = createFactory(scope = model.scope)

            // Act
            val actual = factory.getStateWithUpdatedExpressTxs(expressTxs = expressTxs, updateBalance = {})

            // Assert
            val expected = currentState.copy(
                transactions = expressTxs,
                transactionsToDisplay = model.expected.toPersistentList(),
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            TransactionsToDisplayModel(
                scope = ExpressTransactionsComponent.Scope.Currency,
                isNewTxHistoryEnabled = true,
                expected = emptyList(),
            ),
            TransactionsToDisplayModel(
                scope = ExpressTransactionsComponent.Scope.Currency,
                isNewTxHistoryEnabled = false,
                expected = listOf(exchange, visibleOnramp),
            ),
            TransactionsToDisplayModel(
                scope = accountScope,
                isNewTxHistoryEnabled = true,
                expected = listOf(exchange, visibleOnramp),
            ),
            TransactionsToDisplayModel(
                scope = accountScope,
                isNewTxHistoryEnabled = false,
                expected = listOf(exchange, visibleOnramp),
            ),
        )
    }

    private fun createFactory(scope: ExpressTransactionsComponent.Scope) = ExpressStatusFactory(
        currentStateProvider = Provider { currentState },
        clickIntents = clickIntents,
        cryptoCurrency = cryptoCurrency,
        appCurrencyProvider = Provider { AppCurrency.Default },
        userWallet = userWallet,
        cryptoCurrencyStatusProvider = Provider<CryptoCurrencyStatus?> { null },
        scope = scope,
        dispatchers = dispatchers,
        analyticsEventsHandler = analyticsEventsHandler,
        onrampStatusFactory = onrampStatusFactoryCreator,
        exchangeStatusFactory = exchangeStatusFactoryCreator,
        txHistoryFeatureToggles = txHistoryFeatureToggles,
    )

    internal data class TransactionsToDisplayModel(
        val scope: ExpressTransactionsComponent.Scope,
        val isNewTxHistoryEnabled: Boolean,
        val expected: List<ExpressTransactionStateUM>,
    )
}