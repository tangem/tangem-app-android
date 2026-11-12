package com.tangem.features.earn.lending.impl.supplier

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.lending.impl.provider.FakeLendingProvider
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingBalanceFetcherTest {

    private val userWalletId = UserWalletId("011")
    private val currencies = MockCryptoCurrencyFactory()
    private val eth = currencies.createCoin(Blockchain.Ethereum)
    private val arbitrumEth = currencies.createCoin(Blockchain.Arbitrum)
    private val ethAsset = LendingAsset(
        currency = eth,
        wrappedToken = currencies.createToken(Blockchain.Ethereum, id = "WETH", contractAddress = "0xweth"),
    )
    private val arbitrumAsset = LendingAsset(
        currency = arbitrumEth,
        wrappedToken = currencies.createToken(Blockchain.Arbitrum, id = "WETH", contractAddress = "0xarbweth"),
    )
    private val assets = mapOf(eth.id to ethAsset, arbitrumEth.id to arbitrumAsset)

    private val earnFeatureToggles: EarnFeatureToggles = mockk()
    private lateinit var provider: FakeLendingProvider
    private lateinit var fetcher: LendingBalanceFetcher

    @BeforeEach
    fun setUp() {
        clearMocks(earnFeatureToggles)
        every { earnFeatureToggles.isEarnEnabled } returns true
        createFetcher(FakeLendingProvider(assets = assets))
    }

    @Test
    fun `GIVEN currencies with lending assets WHEN fetch THEN only their positions refreshed`() = runTest {
        // Arrange
        val usdt = currencies.createToken(Blockchain.Ethereum)

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(eth, usdt, arbitrumEth, currencies.createCoin(Blockchain.BSC)))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(provider.refreshPositionCalls).containsExactly(userWalletId to ethAsset, userWalletId to arbitrumAsset)
        assertThat(provider.refreshCalls).isEmpty()
    }

    @Test
    fun `GIVEN same coin twice WHEN fetch THEN its position refreshed once`() = runTest {
        // Act
        fetcher.fetch(userWalletId, listOf(eth, eth))

        // Assert
        assertThat(provider.refreshPositionCalls).containsExactly(userWalletId to ethAsset)
    }

    @Test
    fun `GIVEN earn toggle disabled WHEN fetch THEN nothing refreshed`() = runTest {
        // Arrange
        every { earnFeatureToggles.isEarnEnabled } returns false

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(eth))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(provider.refreshPositionCalls).isEmpty()
    }

    @Test
    fun `GIVEN provider unavailable WHEN fetch THEN nothing refreshed`() = runTest {
        // Arrange
        createFetcher(FakeLendingProvider(assets = assets, isAvailable = false))

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(eth))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(provider.refreshPositionCalls).isEmpty()
    }

    @Test
    fun `GIVEN position refresh fails WHEN fetch THEN Right and every position still refreshed`() = runTest {
        // Arrange
        provider.refreshPositionResult = IllegalStateException("rpc").left()

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(eth, arbitrumEth))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(provider.refreshPositionCalls).containsExactly(userWalletId to ethAsset, userWalletId to arbitrumAsset)
    }

    private fun createFetcher(fakeProvider: FakeLendingProvider) {
        provider = fakeProvider
        fetcher = LendingBalanceFetcher(provider = provider, earnFeatureToggles = earnFeatureToggles)
    }
}