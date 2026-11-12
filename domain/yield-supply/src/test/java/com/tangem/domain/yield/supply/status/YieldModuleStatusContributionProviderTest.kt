package com.tangem.domain.yield.supply.status

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModuleStatusContributionProviderTest {

    private val yieldModuleStatusSupplier: YieldModuleStatusSupplier = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()

    private val provider = YieldModuleStatusContributionProvider(
        yieldModuleStatusSupplier = yieldModuleStatusSupplier,
        tokensFeatureToggles = tokensFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }

    // contract address is mixed-case on purpose: statuses are keyed by the normalized (lowercase) address
    private val token = MockYieldSupply.token(contractAddress = "0xTokenAbC")

    @BeforeEach
    fun setUp() {
        clearMocks(yieldModuleStatusSupplier, tokensFeatureToggles)
        every { tokensFeatureToggles.isYieldContributionEnabled } returns true
    }

    @Test
    fun `GIVEN yield contribution disabled WHEN contributions THEN empty resolver without reading statuses`() =
        runTest {
            // Arrange
            every { tokensFeatureToggles.isYieldContributionEnabled } returns false

            // Act
            val actual = provider.contributions(userWallet).first()

            // Assert
            assertThat(actual).isSameInstanceAs(ContributionResolver.Empty)
            verify(exactly = 0) { yieldModuleStatusSupplier(any()) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN statuses WHEN resolve THEN contribution of the matching status`(model: ResolveModel) = runTest {
        // Arrange
        every {
            yieldModuleStatusSupplier(YieldModuleStatusProducer.Params(userWalletId = userWalletId))
        } returns flowOf(model.statuses)

        // Act
        val actual = provider.contributions(userWallet).first().resolve(
            currency = model.currency,
            networkStatus = null,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SpendableDelta {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a position and a network status WHEN resolve THEN spendable delta makes amount the paired total`(
            model: SpendableModel,
        ) = runTest {
            // Arrange
            every {
                yieldModuleStatusSupplier(YieldModuleStatusProducer.Params(userWalletId = userWalletId))
            } returns flowOf(setOf(model.position))

            // Act
            val actual = provider.contributions(userWallet).first()
                .resolve(currency = token, networkStatus = model.networkStatus)
                ?.spendableDeltaCryptoAmount()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = spendableModels()
    }

    private fun spendableModels() = listOf(
        SpendableModel(
            name = "network amount read with the position: the supplied part",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = verified(amount = BigDecimal("3")),
            expected = BigDecimal("7"),
        ),
        SpendableModel(
            name = "network amount observed after an exit: nothing on top",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = verified(amount = BigDecimal("10")),
            expected = BigDecimal.ZERO,
        ),
        SpendableModel(
            name = "network amount ahead of the position: negative",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = verified(amount = BigDecimal("12")),
            expected = BigDecimal("-2"),
        ),
        SpendableModel(
            name = "network amount already holds the supplied funds",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = verified(
                amount = BigDecimal("10"),
                yieldStatus = YieldSupplyStatus(
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = BigDecimal("7"),
                ),
            ),
            expected = BigDecimal.ZERO,
        ),
        SpendableModel(
            name = "position stored without the paired total: the supplied part",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = null),
            networkStatus = verified(amount = BigDecimal("3")),
            expected = BigDecimal("7"),
        ),
        SpendableModel(
            name = "inactive position",
            position = status(isActive = false, effectiveProtocolBalance = null),
            networkStatus = verified(amount = BigDecimal("3")),
            expected = BigDecimal.ZERO,
        ),
        SpendableModel(
            name = "network status unreachable",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = NetworkStatus(network = token.network, value = NetworkStatus.Unreachable(address = null)),
            expected = BigDecimal.ZERO,
        ),
        SpendableModel(
            name = "no network status",
            position = status(effectiveProtocolBalance = BigDecimal("7"), effectiveBalance = BigDecimal("10")),
            networkStatus = null,
            expected = BigDecimal.ZERO,
        ),
    )

    data class SpendableModel(
        val name: String,
        val position: YieldModuleStatus,
        val networkStatus: NetworkStatus?,
        val expected: BigDecimal,
    ) {
        override fun toString(): String = name
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        ResolveModel(
            name = "active status",
            statuses = setOf(status(isActive = true, effectiveProtocolBalance = BigDecimal("7"))),
            expected = YieldSupplyContribution(
                status = YieldSupplyStatus(
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = BigDecimal("7"),
                ),
                source = StatusSource.ONLY_CACHE,
            ),
        ),
        ResolveModel(
            name = "inactive status",
            statuses = setOf(status(isActive = false, effectiveProtocolBalance = null)),
            expected = YieldSupplyContribution(
                status = YieldSupplyStatus(
                    isActive = false,
                    isInitialized = true,
                    isAllowedToSpend = true,
                    effectiveProtocolBalance = null,
                ),
                source = StatusSource.ONLY_CACHE,
            ),
        ),
        ResolveModel(
            name = "no statuses",
            statuses = emptySet(),
            expected = null,
        ),
        ResolveModel(
            name = "status of another token",
            statuses = setOf(status(tokenAddress = "0xother")),
            expected = null,
        ),
        ResolveModel(
            name = "status on another derivation of the network",
            statuses = setOf(
                status(
                    networkId = Network.ID(
                        value = MockYieldSupply.NETWORK_ID,
                        derivationPath = Network.DerivationPath.Custom("m/44'/60'/1'/0/0"),
                    ),
                ),
            ),
            expected = null,
        ),
        ResolveModel(
            name = "network coin",
            currency = MockYieldSupply.coin(),
            statuses = setOf(status()),
            expected = null,
        ),
    )

    private fun status(
        networkId: Network.ID = token.network.id,
        tokenAddress: String = "0xtokenabc",
        isActive: Boolean = true,
        effectiveProtocolBalance: BigDecimal? = BigDecimal.ONE,
        effectiveBalance: BigDecimal? = null,
    ) = YieldModuleStatus(
        networkId = networkId,
        tokenAddress = tokenAddress,
        isActive = isActive,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = effectiveProtocolBalance,
        effectiveBalance = effectiveBalance,
        source = StatusSource.ONLY_CACHE,
    )

    /** Verified network status of [token]; [yieldStatus] non-null is the SDK still reporting the effective balance. */
    private fun verified(amount: BigDecimal, yieldStatus: YieldSupplyStatus? = null) = NetworkStatus(
        network = token.network,
        value = NetworkStatus.Verified(
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
            ),
            amounts = mapOf(token.id to NetworkStatus.Amount.Loaded(value = amount)),
            pendingTransactions = emptyMap(),
            yieldSupplyStatuses = mapOf(token.id to yieldStatus),
            source = StatusSource.ACTUAL,
        ),
    )

    data class ResolveModel(
        val name: String,
        val statuses: Set<YieldModuleStatus>,
        val expected: YieldSupplyContribution?,
        val currency: CryptoCurrency = MockYieldSupply.token(contractAddress = "0xTokenAbC"),
    ) {
        override fun toString(): String = name
    }
}