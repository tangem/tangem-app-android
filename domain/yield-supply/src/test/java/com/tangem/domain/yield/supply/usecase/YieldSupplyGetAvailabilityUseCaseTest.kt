package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyAvailability
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyGetAvailabilityUseCaseTest {

    private val repository: YieldSupplyRepository = mockk()

    private val useCase = YieldSupplyGetAvailabilityUseCase(yieldSupplyRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: AvailabilityModel) = runTest {
        // Arrange
        coEvery { repository.getCachedMarkets() } returns listOf(model.market)

        // Act
        val actual = useCase(token()).getOrNull()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        AvailabilityModel(
            market = market(hasCampaign = true),
            expected = YieldSupplyAvailability.Available(apy = "5.5"),
        ),
        AvailabilityModel(
            market = market(hasCampaign = false),
            expected = YieldSupplyAvailability.Available(apy = "4"),
        ),
        AvailabilityModel(
            market = market(campaign = null),
            expected = YieldSupplyAvailability.Available(apy = "4"),
        ),
        AvailabilityModel(
            market = market(hasCampaign = true, isActive = false),
            expected = YieldSupplyAvailability.Unavailable,
        ),
    )

    /**
     * The per-token endpoint behind [YieldSupplyRepository.getTokenStatus] is the plain one and carries no campaign
     * fields, so a cache miss serves the base APY even while a campaign is running.
     */
    @Test
    fun `GIVEN market missing from cache WHEN invoke THEN fetched status base apy used`() = runTest {
        // Arrange
        coEvery { repository.getCachedMarkets() } returns emptyList()
        coEvery { repository.getTokenStatus(any()) } returns market(campaign = null)

        // Act
        val actual = useCase(token()).getOrNull()

        // Assert
        assertThat(actual).isEqualTo(YieldSupplyAvailability.Available(apy = "4"))
    }

    @Test
    fun `GIVEN coin WHEN invoke THEN unavailable`() = runTest {
        // Act
        val actual = useCase(coin()).getOrNull()

        // Assert
        assertThat(actual).isEqualTo(YieldSupplyAvailability.Unavailable)
    }

    /** A yield deeplink can name a token the wallet holds only in a joint account; the Safe makes no sends yet */
    @Test
    fun `GIVEN a joint account token WHEN invoke THEN unavailable without asking the repository`() = runTest {
        // Arrange
        val jointToken = token(network = network(derivationPath = Network.DerivationPath.Joint(JOINT_DERIVATION)))

        // Act
        val actual = useCase(jointToken).getOrNull()

        // Assert
        assertThat(actual).isEqualTo(YieldSupplyAvailability.Unavailable)
        coVerify(exactly = 0) { repository.getCachedMarkets() }
        coVerify(exactly = 0) { repository.getTokenStatus(any()) }
    }

    internal data class AvailabilityModel(
        val market: YieldMarketToken,
        val expected: YieldSupplyAvailability,
    )

    private fun market(
        hasCampaign: Boolean = false,
        isActive: Boolean = true,
        campaign: YieldIncentiveCampaign? = YieldIncentiveCampaign(
            hasActiveCampaign = hasCampaign,
            rewardsApr = BigDecimal("1.5"),
            totalApyEst = BigDecimal("5.5"),
        ),
    ) = YieldMarketToken(
        tokenAddress = TOKEN_ADDRESS,
        chainId = 1,
        apy = BigDecimal("4"),
        isActive = isActive,
        maxFeeNative = BigDecimal.ZERO,
        maxFeeUSD = BigDecimal.ZERO,
        backendId = NETWORK_ID,
        campaign = campaign,
    )

    private fun token(network: Network = network()) = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(NETWORK_ID),
            suffix = CryptoCurrency.ID.Suffix.RawID("usdc", TOKEN_ADDRESS),
        ),
        network = network,
        name = "USD Coin",
        symbol = "USDC",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = TOKEN_ADDRESS,
    )

    private fun coin() = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(NETWORK_ID),
            suffix = CryptoCurrency.ID.Suffix.RawID(NETWORK_ID),
        ),
        network = network(),
        name = "Ethereum",
        symbol = "ETH",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    private fun network(derivationPath: Network.DerivationPath = Network.DerivationPath.None) = Network(
        id = Network.ID(value = NETWORK_ID, derivationPath = derivationPath),
        name = "Ethereum",
        currencySymbol = "ETH",
        derivationPath = derivationPath,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    private companion object {
        const val TOKEN_ADDRESS = "0xabc"
        const val NETWORK_ID = "ethereum"
        const val JOINT_DERIVATION = "m/44'/60'/888888'/0/0"
    }
}