package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.expressStatus.state.ExpressTransactionsBlockState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.ExpressAnalyticsStatus
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeStatusModel
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express.MockExpressTransactionFactory.createExchange
import com.tangem.feature.tokendetails.store.SwapTransactionStatusStore
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.Provider
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class ExchangeStatusFactoryTest {

    private val swapTransactionRepository: SwapTransactionRepository = mockk()
    private val swapRepository: SwapRepository = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    private val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase = mockk()
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase = mockk()
    private val swapTransactionStatusStore: SwapTransactionStatusStore = mockk()
    private val analyticsEventsHandler: AnalyticsEventHandler = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val clickIntents: ExpressTransactionsClickIntents = mockk()
    private val userWallet: UserWallet = mockk()
    private val cryptoCurrency: CryptoCurrency = mockk()
    private val refundCurrency: CryptoCurrency = mockk(relaxed = true)

    private val userWalletId = MockAccounts.userWalletId
    private val account = MockAccounts.createAccount(
        derivationIndex = 1,
        cryptoCurrencies = listOf(cryptoCurrency),
        userWalletId = userWalletId,
    )
    private val cryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = cryptoCurrency,
        value = CryptoCurrencyStatus.Loading,
    )
    private val cryptoCurrencyId = CryptoCurrency.ID.fromValue(value = "coin⟨BCH⟩bitcoin-cash")

    private val currentState = ExpressTransactionsBlockState(
        transactions = persistentListOf(),
        transactionsToDisplay = persistentListOf(),
        bottomSheetSlot = null,
    )

    private val refundedStatusModel = ExchangeStatusModel(
        providerId = "changenow",
        status = ExchangeStatus.Refunded,
        txId = TX_ID,
        refundNetwork = REFUND_NETWORK,
        refundContractAddress = REFUND_CONTRACT_ADDRESS,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            swapTransactionRepository,
            swapRepository,
            getAccountCurrencyStatusUseCase,
            manageCryptoCurrenciesUseCase,
            swapTransactionStatusStore,
            getUserWalletUseCase,
            cryptoCurrency,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `GIVEN account scope WHEN invoke THEN getAccountTransactions used and getTransactions not called`() =
            runTest {
                // Arrange
                val scope = ExpressTransactionsComponent.Scope.Account(accountId = account.accountId, maxCount = 2)
                every {
                    swapTransactionRepository.getAccountTransactions(userWallet = userWallet, accountId = account.accountId)
                } returns flowOf(null)

                // Act
                val actual = getEmittedValues(createFactory(scope = scope).invoke())

                // Assert
                assertThat(actual).containsExactly(persistentListOf<ExchangeUM>())
                verify(exactly = 1) {
                    swapTransactionRepository.getAccountTransactions(userWallet = userWallet, accountId = account.accountId)
                }
                verify(exactly = 0) { swapTransactionRepository.getTransactions(userWallet = any(), cryptoCurrencyId = any()) }
            }

        @Test
        fun `GIVEN currency scope WHEN invoke THEN getTransactions used`() = runTest {
            // Arrange
            every { cryptoCurrency.id } returns cryptoCurrencyId
            every {
                swapTransactionRepository.getTransactions(userWallet = userWallet, cryptoCurrencyId = cryptoCurrencyId)
            } returns flowOf(null)

            // Act
            val actual = getEmittedValues(createFactory(scope = ExpressTransactionsComponent.Scope.Currency).invoke())

            // Assert
            assertThat(actual).containsExactly(persistentListOf<ExchangeUM>())
            verify(exactly = 1) {
                swapTransactionRepository.getTransactions(userWallet = userWallet, cryptoCurrencyId = cryptoCurrencyId)
            }
            verify(exactly = 0) { swapTransactionRepository.getAccountTransactions(userWallet = any(), accountId = any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateSwapTxStatus {

        @Test
        fun `GIVEN CEX provider WHEN updateSwapTxStatus THEN account currency status not requested and state stored without refund`() =
            runTest {
                // Arrange
                val swapTx = createExchange(
                    txId = TX_ID,
                    providerType = ExchangeProviderType.CEX,
                    fromUserWalletId = userWalletId,
                )
                stubExchangeStatusRequest()

                // Act
                val actual = createFactory().updateSwapTxStatus(swapTx)

                // Assert
                coVerify(exactly = 1) {
                    swapTransactionRepository.storeTransactionState(
                        txId = TX_ID,
                        status = refundedStatusModel,
                        accountWithCurrency = null,
                    )
                }
                verify { getAccountCurrencyStatusUseCase wasNot Called }
                verify { manageCryptoCurrenciesUseCase wasNot Called }
                assertThat(actual.notification).isNull()
            }

        @Test
        fun `GIVEN DEX_BRIDGE with refund network and contract WHEN updateSwapTxStatus THEN refund currency added to the account and stored with it`() =
            runTest {
                // Arrange
                val swapTx = createExchange(
                    txId = TX_ID,
                    providerType = ExchangeProviderType.DEX_BRIDGE,
                    fromUserWalletId = userWalletId,
                )
                stubExchangeStatusRequest()
                coEvery {
                    getAccountCurrencyStatusUseCase.invokeSync(userWalletId = userWalletId, currency = cryptoCurrency)
                } returns AccountCryptoCurrencyStatus(account = account, status = cryptoCurrencyStatus).some()
                coEvery {
                    manageCryptoCurrenciesUseCase.add(
                        accountId = account.accountId,
                        networkId = REFUND_NETWORK,
                        contractAddress = REFUND_CONTRACT_ADDRESS,
                    )
                } returns refundCurrency.right()

                // Act
                val actual = createFactory().updateSwapTxStatus(swapTx)

                // Assert
                coVerify(exactly = 1) {
                    manageCryptoCurrenciesUseCase.add(
                        accountId = account.accountId,
                        networkId = REFUND_NETWORK,
                        contractAddress = REFUND_CONTRACT_ADDRESS,
                    )
                }
                coVerify(exactly = 1) {
                    swapTransactionRepository.storeTransactionState(
                        txId = TX_ID,
                        status = refundedStatusModel,
                        accountWithCurrency = account.accountId to refundCurrency,
                    )
                }
                val notification = actual.notification as? ExchangeStatusNotification.TokenRefunded
                assertThat(notification?.cryptoCurrency).isEqualTo(refundCurrency)
            }
    }

    private fun stubExchangeStatusRequest() {
        every { getUserWalletUseCase(userWalletId) } returns userWallet.right()
        coEvery {
            swapRepository.getExchangeStatus(userWallet = userWallet, userWalletId = userWalletId, txId = TX_ID)
        } returns refundedStatusModel.right()
        coEvery { swapTransactionStatusStore.getTransactionStatus(TX_ID) } returns ExpressAnalyticsStatus.Refunded
        coEvery {
            swapTransactionRepository.storeTransactionState(txId = any(), status = any(), accountWithCurrency = any())
        } just Runs
    }

    private fun createFactory(
        scope: ExpressTransactionsComponent.Scope = ExpressTransactionsComponent.Scope.Currency,
    ) = ExchangeStatusFactory(
        swapTransactionRepository = swapTransactionRepository,
        swapRepository = swapRepository,
        getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        getPaymentAccountCryptoCurrencyStatusUseCase = getPaymentAccountCryptoCurrencyStatusUseCase,
        manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
        swapTransactionStatusStore = swapTransactionStatusStore,
        analyticsEventsHandler = analyticsEventsHandler,
        getUserWalletUseCase = getUserWalletUseCase,
        clickIntents = clickIntents,
        appCurrencyProvider = Provider { AppCurrency.Default },
        currentStateProvider = Provider { currentState },
        userWallet = userWallet,
        cryptoCurrency = cryptoCurrency,
        scope = scope,
    )

    private companion object {
        const val TX_ID = "tx_id"
        const val REFUND_NETWORK = "ethereum"
        const val REFUND_CONTRACT_ADDRESS = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}