package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrencyStatus
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

internal class NetworkContributionsRefresherTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val balanceFetchingOperations: BalanceFetchingOperations = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()

    private val refresher = NetworkContributionsRefresher(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
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
        clearMocks(singleAccountStatusListSupplier, balanceFetchingOperations, tokensFeatureToggles)
        every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns true
        coEvery { balanceFetchingOperations.fetchAll(any(), any(), any()) } returns emptyMap()
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList(
            statuses = listOf(ethereum, ethereumToken, cardano).map { CryptoCurrencyStatus(it, mockk()) },
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
        coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) }
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
    fun `GIVEN no account status list WHEN invoke THEN nothing is fetched`() = runTest {
        // Arrange
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns null

        // Act
        refresher(userWalletId = userWalletId, network = ethereum.network)

        // Assert
        coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
    }

    private fun accountStatusList(statuses: List<CryptoCurrencyStatus>): AccountStatusList = mockk {
        every { flattenCurrencies() } returns statuses
    }
}