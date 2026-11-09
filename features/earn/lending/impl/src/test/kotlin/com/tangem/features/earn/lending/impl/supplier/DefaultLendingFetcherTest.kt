package com.tangem.features.earn.lending.impl.supplier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.features.earn.lending.impl.provider.FakeLendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultLendingFetcherTest {

    private val userWalletId = UserWalletId("011")
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val asset = LendingAsset(currency = eth, wrappedToken = currencies.createToken(Blockchain.Ethereum))
    private val networkParams = SingleNetworkStatusFetcher.Params(userWalletId, eth.network)

    private val networkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private lateinit var provider: FakeLendingProvider
    private lateinit var fetcher: DefaultLendingFetcher

    @BeforeEach
    fun setUp() {
        clearMocks(networkStatusFetcher)
        provider = FakeLendingProvider(assets = mapOf(eth.id to asset))
        fetcher = DefaultLendingFetcher(provider = provider, networkStatusFetcher = networkStatusFetcher)
        coEvery { networkStatusFetcher(networkParams) } returns Unit.right()
    }

    @Test
    fun `GIVEN provider and network refresh succeed WHEN fetch THEN Right and both refreshed`() = runTest {
        // Act
        val actual = fetcher(LendingFetcher.Params(userWalletId, eth))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(provider.refreshCalls).containsExactly(userWalletId to asset)
        coVerify(exactly = 1) { networkStatusFetcher(networkParams) }
    }

    @Test
    fun `GIVEN currency without asset WHEN fetch THEN Left without refresh`() = runTest {
        // Act
        val actual = fetcher(LendingFetcher.Params(userWalletId, currencies.createCoin(Blockchain.BSC)))

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(provider.refreshCalls).isEmpty()
    }

    @Test
    fun `GIVEN provider refresh fails WHEN fetch THEN Left`() = runTest {
        // Arrange
        provider.refreshResult = IllegalStateException("provider").left()

        // Act
        val actual = fetcher(LendingFetcher.Params(userWalletId, eth))

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN network refresh fails WHEN fetch THEN Left`() = runTest {
        // Arrange
        coEvery { networkStatusFetcher(networkParams) } returns IllegalStateException("network").left()

        // Act
        val actual = fetcher(LendingFetcher.Params(userWalletId, eth))

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }
}