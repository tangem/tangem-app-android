package com.tangem.data.moneyaccount.yield

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.domain.yield.supply.status.YieldModuleStatusFetcher
import com.tangem.domain.yield.supply.status.YieldModuleStatusProducer
import com.tangem.domain.yield.supply.status.YieldModuleStatusSupplier
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultMoneyAccountYieldModeStatusProviderTest {

    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()
    private val yieldModuleStatusFetcher: YieldModuleStatusFetcher = mockk()
    private val yieldModuleStatusSupplier: YieldModuleStatusSupplier = mockk()

    private val provider = DefaultMoneyAccountYieldModeStatusProvider(
        moneyAccountFeatureToggles = moneyAccountFeatureToggles,
        tokensFeatureToggles = tokensFeatureToggles,
        yieldModuleStatusFetcher = yieldModuleStatusFetcher,
        yieldModuleStatusSupplier = yieldModuleStatusSupplier,
    )

    private val tokenId: CryptoCurrency.ID = mockk()
    private val token: CryptoCurrency.Token = mockk {
        every { id } returns tokenId
        every { network } returns mockk { every { id } returns NETWORK_ID }
        every { contractAddress } returns CONTRACT_ADDRESS
    }
    private val networkStatus: NetworkStatus.Verified = mockk {
        every { yieldSupplyStatuses } returns mapOf(tokenId to LEGACY_STATUS)
    }
    private val fetcherParams = YieldModuleStatusFetcher.Params(userWalletId = USER_WALLET_ID, tokens = setOf(token))
    private val supplierParams = YieldModuleStatusProducer.Params(userWalletId = USER_WALLET_ID)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountFeatureToggles, tokensFeatureToggles, yieldModuleStatusFetcher, yieldModuleStatusSupplier)
        every { moneyAccountFeatureToggles.isYieldModeEnabled } returns true
        every { tokensFeatureToggles.isYieldContributionEnabled } returns true
        coEvery { yieldModuleStatusFetcher(fetcherParams) } returns Unit.right()
    }

    @Test
    fun `GIVEN yield mode disabled WHEN get THEN null and nothing fetched`() = runTest {
        // Arrange
        every { moneyAccountFeatureToggles.isYieldModeEnabled } returns false

        // Act
        val actual = provider.get(userWalletId = USER_WALLET_ID, token = token, networkStatus = networkStatus)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { yieldModuleStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN yield contribution disabled WHEN get THEN status from network status`() = runTest {
        // Arrange
        every { tokensFeatureToggles.isYieldContributionEnabled } returns false

        // Act
        val actual = provider.get(userWalletId = USER_WALLET_ID, token = token, networkStatus = networkStatus)

        // Assert
        assertThat(actual).isEqualTo(LEGACY_STATUS)
        coVerify(exactly = 0) { yieldModuleStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN yield contribution enabled WHEN get THEN token position fetched and returned`() = runTest {
        // Arrange
        coEvery { yieldModuleStatusSupplier.getSyncOrNull(supplierParams, any()) } returns setOf(tokenPosition())

        // Act
        val actual = provider.get(userWalletId = USER_WALLET_ID, token = token, networkStatus = networkStatus)

        // Assert
        assertThat(actual).isEqualTo(POSITION_STATUS)
        coVerify(exactly = 1) { yieldModuleStatusFetcher(fetcherParams) }
    }

    @Test
    fun `GIVEN position fetch failed WHEN get THEN cached position returned`() = runTest {
        // Arrange
        coEvery { yieldModuleStatusFetcher(fetcherParams) } returns IllegalStateException("RPC").left()
        coEvery { yieldModuleStatusSupplier.getSyncOrNull(supplierParams, any()) } returns setOf(tokenPosition())

        // Act
        val actual = provider.get(userWalletId = USER_WALLET_ID, token = token, networkStatus = networkStatus)

        // Assert
        assertThat(actual).isEqualTo(POSITION_STATUS)
    }

    @Test
    fun `GIVEN no position for token WHEN get THEN null`() = runTest {
        // Arrange
        val otherTokenPosition = tokenPosition().copy(tokenAddress = "0xother")
        coEvery { yieldModuleStatusSupplier.getSyncOrNull(supplierParams, any()) } returns setOf(otherTokenPosition)

        // Act
        val actual = provider.get(userWalletId = USER_WALLET_ID, token = token, networkStatus = networkStatus)

        // Assert
        assertThat(actual).isNull()
    }

    private fun tokenPosition(): YieldModuleStatus {
        val key = YieldModuleStatus.Key(token)
        return YieldModuleStatus(
            networkId = key.networkId,
            tokenAddress = key.tokenAddress,
            isActive = POSITION_STATUS.isActive,
            isInitialized = POSITION_STATUS.isInitialized,
            isAllowedToSpend = POSITION_STATUS.isAllowedToSpend,
            effectiveProtocolBalance = POSITION_STATUS.effectiveProtocolBalance,
            effectiveBalance = BigDecimal("15"),
            source = StatusSource.ACTUAL,
        )
    }

    private companion object {
        const val CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"

        val USER_WALLET_ID = UserWalletId("011")
        val NETWORK_ID = Network.ID(value = "polygon-pos", derivationPath = Network.DerivationPath.None)
        val LEGACY_STATUS = YieldSupplyStatus(
            isActive = false,
            isInitialized = true,
            isAllowedToSpend = false,
            effectiveProtocolBalance = null,
        )
        val POSITION_STATUS = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("10"),
        )
    }
}