package com.tangem.domain.yield.supply.status

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldModuleStatusContributionFetcherTest {

    private val yieldModuleStatusFetcher: YieldModuleStatusFetcher = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()
    private val contributionFetcher = YieldModuleStatusContributionFetcher(
        yieldModuleStatusFetcher = yieldModuleStatusFetcher,
        tokensFeatureToggles = tokensFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")

    private val token = MockYieldSupply.token()
    private val coin = MockYieldSupply.coin()
    private val jointToken = MockYieldSupply.token(
        network = MockYieldSupply.network(derivationPath = Network.DerivationPath.Joint("m/44'/60'/888888'/0/0")),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(yieldModuleStatusFetcher, tokensFeatureToggles)
        every { tokensFeatureToggles.isYieldContributionEnabled } returns true
    }

    @Test
    fun `GIVEN yield contribution disabled WHEN fetch THEN returns right without fetching`() = runTest {
        // Arrange
        every { tokensFeatureToggles.isYieldContributionEnabled } returns false

        // Act
        val actual = contributionFetcher.fetch(userWalletId, listOf(token))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { yieldModuleStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN coin, joint and personal tokens WHEN fetch THEN only the personal token is fetched`() = runTest {
        // Arrange
        coEvery { yieldModuleStatusFetcher(any()) } returns Unit.right()

        // Act
        val actual = contributionFetcher.fetch(userWalletId, listOf(coin, jointToken, token))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            yieldModuleStatusFetcher(YieldModuleStatusFetcher.Params(userWalletId, setOf(token)))
        }
    }

    @Test
    fun `GIVEN no personal tokens WHEN fetch THEN returns right without fetching`() = runTest {
        // Act
        val actual = contributionFetcher.fetch(userWalletId, listOf(coin, jointToken))

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { yieldModuleStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN fetcher fails WHEN fetch THEN the failure is returned`() = runTest {
        // Arrange
        val error = IllegalStateException("rpc")
        coEvery { yieldModuleStatusFetcher(any()) } returns error.left()

        // Act
        val actual = contributionFetcher.fetch(userWalletId, listOf(token))

        // Assert
        assertThat(actual).isEqualTo(error.left())
    }
}