package com.tangem.data.tokens.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultCurrenciesRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val repository = DefaultCurrenciesRepository(
        tangemTechApi = mockk(),
        userWalletsListRepository = mockk(),
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
        excludedBlockchains = mockk<ExcludedBlockchains>(relaxed = true),
    )

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN network pending state WHEN isSendBlockedByPendingTransactions THEN returns expected`(
        model: PendingTestModel,
    ) = runTest {
        // Arrange
        val walletManager = model.recentStatuses?.let { statuses ->
            mockk<WalletManager> {
                every { wallet.recentTransactions } returns statuses.mapTo(mutableListOf(), ::createTransaction)
            }
        }
        coEvery {
            walletManagersFacade.getOrCreateWalletManager(userWalletId, model.currency.network)
        } returns walletManager

        // Act
        val actual = repository.isSendBlockedByPendingTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = CryptoCurrencyStatus(model.currency, createValue(model.currencyPendingOutgoing)),
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun createTransaction(status: TransactionStatus): TransactionData.Uncompiled = mockk {
        every { this@mockk.status } returns status
    }

    private fun createValue(pendingOutgoing: List<Boolean>): CryptoCurrencyStatus.Value = mockk {
        every { pendingTransactions } returns pendingOutgoing.mapTo(hashSetOf()) { isOutgoing ->
            mockk<TxInfo> { every { this@mockk.isOutgoing } returns isOutgoing }
        }
    }

    private fun provideTestModels() = listOf(
        PendingTestModel(
            name = "EVM coin with unconfirmed transaction is blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
            recentStatuses = listOf(TransactionStatus.Unconfirmed),
            expected = true,
        ),
        PendingTestModel(
            name = "EVM token is blocked by any unconfirmed transaction in its network",
            currency = cryptoCurrencyFactory.createToken(Blockchain.Polygon),
            recentStatuses = listOf(TransactionStatus.Confirmed, TransactionStatus.Unconfirmed),
            expected = true,
        ),
        PendingTestModel(
            name = "EVM with only confirmed transactions is not blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
            recentStatuses = listOf(TransactionStatus.Confirmed),
            expected = false,
        ),
        PendingTestModel(
            name = "EVM without recent transactions is not blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Arbitrum),
            recentStatuses = emptyList(),
            expected = false,
        ),
        PendingTestModel(
            name = "EVM without wallet manager is not blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Ethereum),
            recentStatuses = null,
            expected = false,
        ),
        PendingTestModel(
            name = "Tron keeps parallel sending",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Tron),
            recentStatuses = listOf(TransactionStatus.Unconfirmed),
            expected = false,
        ),
        PendingTestModel(
            name = "Bitcoin with outgoing pending transaction is blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
            currencyPendingOutgoing = listOf(false, true),
            expected = true,
        ),
        PendingTestModel(
            name = "Bitcoin with only incoming pending transactions is not blocked",
            currency = cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin),
            currencyPendingOutgoing = listOf(false),
            recentStatuses = listOf(TransactionStatus.Unconfirmed),
            expected = false,
        ),
    )

    data class PendingTestModel(
        val name: String,
        val currency: CryptoCurrency,
        val expected: Boolean,
        val recentStatuses: List<TransactionStatus>? = emptyList(),
        val currencyPendingOutgoing: List<Boolean> = emptyList(),
    ) {
        override fun toString(): String = name
    }
}