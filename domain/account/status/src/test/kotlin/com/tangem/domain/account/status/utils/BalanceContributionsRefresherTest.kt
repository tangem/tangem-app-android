package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.TokensFeatureToggles
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BalanceContributionsRefresherTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val balanceFetchingOperations: BalanceFetchingOperations = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()

    private val refresher = BalanceContributionsRefresher(
        singleAccountListSupplier = singleAccountListSupplier,
        balanceFetchingOperations = balanceFetchingOperations,
        tokensFeatureToggles = tokensFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = cryptoCurrencyFactory.ethereum
    private val ethereumToken = cryptoCurrencyFactory.createToken(blockchain = Blockchain.Ethereum)
    private val cardano = cryptoCurrencyFactory.cardano

    @BeforeEach
    fun setUp() {
        clearMocks(singleAccountListSupplier, balanceFetchingOperations, tokensFeatureToggles)
        every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns true
        coEvery { balanceFetchingOperations.fetchAll(any(), any(), any()) } returns emptyMap()
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList(
            currencies = listOf(ethereum, ethereumToken, cardano),
        )
    }

    @Test
    fun `GIVEN portfolio with several networks WHEN invoke THEN contributions of the network currencies are fetched`() =
        runTest {
            // Act
            refresher(userWalletId = userWalletId, network = ethereum.network)

            // Assert
            coVerify(exactly = 1) {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(ethereum, ethereumToken),
                    sources = setOf(FetchingSource.CONTRIBUTIONS),
                )
            }
        }

    @Test
    fun `GIVEN pluggable balance sources disabled WHEN invoke THEN nothing is fetched`() = runTest {
        // Arrange
        every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns false

        // Act
        refresher(userWalletId = userWalletId, network = ethereum.network)

        // Assert
        coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN no currency on the network WHEN invoke THEN nothing is fetched`() = runTest {
        // Arrange
        val solana = cryptoCurrencyFactory.createCoin(Blockchain.Solana)

        // Act
        refresher(userWalletId = userWalletId, network = solana.network)

        // Assert
        coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no account list WHEN invoke THEN nothing is fetched`() = runTest {
        // Arrange
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null

        // Act
        refresher(userWalletId = userWalletId, network = ethereum.network)

        // Assert
        coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
    }

    private fun accountList(currencies: List<CryptoCurrency>): AccountList = mockk {
        every { flattenCurrencies() } returns currencies
    }
}