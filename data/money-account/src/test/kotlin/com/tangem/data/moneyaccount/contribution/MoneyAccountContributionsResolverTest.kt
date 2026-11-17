package com.tangem.data.moneyaccount.contribution

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionProvider
import com.tangem.domain.models.currency.balance.ContributionResolver
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class MoneyAccountContributionsResolverTest {

    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()

    private val userWallet: UserWallet = mockk { every { walletId } returns USER_WALLET_ID }
    private val currencyId: CryptoCurrency.ID = mockk()
    private val currency: CryptoCurrency.Token = mockk {
        every { id } returns currencyId
        every { network } returns mockk { every { id } returns NETWORK_ID }
    }
    private val networkStatus: NetworkStatus = mockk {
        every { network } returns mockk { every { id } returns NETWORK_ID }
    }

    private val yieldResolverFrames = MutableStateFlow(yieldResolver(spendableDelta = SUPPLIED))
    private val yieldProvider = FakeProvider(frames = yieldResolverFrames)

    private val contributions = MoneyAccountContributionsResolver(
        contributionProviders = listOf(yieldProvider),
        multiNetworkStatusSupplier = multiNetworkStatusSupplier,
        userWalletsListRepository = userWalletsListRepository,
        policy = MoneyAccountContributionPolicy(
            moneyAccountFeatureToggles = moneyAccountFeatureToggles,
            tokensFeatureToggles = tokensFeatureToggles,
        ),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiNetworkStatusSupplier, userWalletsListRepository, moneyAccountFeatureToggles, tokensFeatureToggles)
        every { moneyAccountFeatureToggles.isYieldModeEnabled } returns true
        every { tokensFeatureToggles.isYieldContributionEnabled } returns true
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { networkStatus.value } returns verified(amount = WALLET_AMOUNT)
        every {
            multiNetworkStatusSupplier(MultiNetworkStatusProducer.Params(userWalletId = USER_WALLET_ID))
        } returns flowOf(setOf(networkStatus))
        yieldResolverFrames.value = yieldResolver(spendableDelta = SUPPLIED)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class WithContributions {

        @Test
        fun `GIVEN an active yield position WHEN withContributions THEN amount includes it and it is attached`() =
            runTest {
                // Act
                val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

                // Assert
                val expectedContribution = yieldContribution(spendableDelta = SUPPLIED)
                assertThat(actual).isEqualTo(
                    account(cryptoAmount = WALLET_AMOUNT + SUPPLIED, contributions = listOf(expectedContribution)),
                )
            }

        @Test
        fun `GIVEN an active yield position WHEN withContributions THEN the currency status carries it`() = runTest {
            // Act
            val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

            // Assert
            val value = (actual as MoneyAccountStatusValue.Account).cryptoCurrencyStatus?.value
            assertThat(value?.contributions).containsExactly(yieldContribution(spendableDelta = SUPPLIED))
            assertThat(value?.amount).isEqualTo(WALLET_AMOUNT + SUPPLIED)
        }

        @Test
        fun `GIVEN a refreshed network amount WHEN withContributions THEN the delta is folded into it`() =
            runTest {
                // Arrange
                every { networkStatus.value } returns verified(amount = REFRESHED_WALLET_AMOUNT)

                // Act
                val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

                // Assert
                val expectedContribution = yieldContribution(spendableDelta = SUPPLIED)
                assertThat(actual).isEqualTo(
                    account(
                        cryptoAmount = REFRESHED_WALLET_AMOUNT + SUPPLIED,
                        contributions = listOf(expectedContribution),
                    ),
                )
            }

        @Test
        fun `GIVEN a network status from cache only WHEN withContributions THEN the account too`() = runTest {
            // Arrange
            every { networkStatus.value } returns verified(amount = WALLET_AMOUNT, source = StatusSource.ONLY_CACHE)

            // Act
            val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

            // Assert
            assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
        }

        @Test
        fun `GIVEN an active zero-delta yield position from cache only WHEN withContributions THEN the account too`() =
            runTest {
                // Arrange
                yieldResolverFrames.value = yieldResolver(
                    spendableDelta = BigDecimal.ZERO,
                    source = StatusSource.ONLY_CACHE,
                )

                // Act
                val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

                // Assert
                assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
            }

        @Test
        fun `GIVEN an inactive yield position from cache only WHEN withContributions THEN the account source kept`() =
            runTest {
                // Arrange
                yieldResolverFrames.value = yieldResolver(
                    spendableDelta = SUPPLIED,
                    status = ACTIVE_STATUS.copy(isActive = false),
                    source = StatusSource.ONLY_CACHE,
                )

                // Act
                val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

                // Assert
                assertThat(actual.source).isEqualTo(StatusSource.ACTUAL)
            }

        @Test
        fun `GIVEN the yield position changes WHEN collecting THEN the account re-emits without a refetch`() = runTest {
            contributions.withContributions(USER_WALLET_ID, account()).test {
                // Arrange
                awaitItem()

                // Act
                yieldResolverFrames.value = yieldResolver(spendableDelta = BigDecimal.ZERO)

                // Assert
                val actual = awaitItem() as MoneyAccountStatusValue.Account
                assertThat(actual.balance?.cryptoAmount).isEqualTo(WALLET_AMOUNT)
            }
        }

        @Test
        fun `GIVEN no network status of the account network WHEN withContributions THEN resolved without one`() =
            runTest {
                // Arrange
                every {
                    multiNetworkStatusSupplier(MultiNetworkStatusProducer.Params(userWalletId = USER_WALLET_ID))
                } returns flowOf(emptySet())

                // Act
                val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

                // Assert — the fake resolver contributes only against the account network status
                assertThat(actual).isEqualTo(account())
            }

        @Test
        fun `GIVEN yield mode disabled WHEN withContributions THEN the value as stored`() = runTest {
            // Arrange
            every { moneyAccountFeatureToggles.isYieldModeEnabled } returns false

            // Act
            val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

            // Assert
            assertThat(actual).isEqualTo(account())
        }

        @Test
        fun `GIVEN contribution mode disabled WHEN withContributions THEN the value as stored`() = runTest {
            // Arrange
            every { tokensFeatureToggles.isYieldContributionEnabled } returns false

            // Act
            val actual = contributions.withContributions(USER_WALLET_ID, account()).first()

            // Assert
            assertThat(actual).isEqualTo(account())
        }

        @Test
        fun `GIVEN no account WHEN withContributions THEN the value as stored`() = runTest {
            // Act
            val actual = contributions.withContributions(USER_WALLET_ID, MoneyAccountStatusValue.Empty).first()

            // Assert
            assertThat(actual).isEqualTo(MoneyAccountStatusValue.Empty)
        }
    }

    private fun account(
        cryptoAmount: BigDecimal = WALLET_AMOUNT,
        contributions: List<YieldSupplyContribution> = emptyList(),
    ) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = currency,
        balance = MoneyAccountStatusValue.Balance(
            cryptoAmount = cryptoAmount,
            depositAddress = "0xdeposit",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
            contributions = contributions,
        ),
        customerId = "customer",
        productInstanceId = "instance",
        error = null,
        isKycAdditionalInfoRequired = false,
    )

    private fun verified(
        amount: BigDecimal,
        source: StatusSource = StatusSource.ACTUAL,
    ): NetworkStatus.Verified = mockk {
        every { amounts } returns mapOf(currencyId to NetworkStatus.Amount.Loaded(amount))
        every { this@mockk.source } returns source
    }

    private fun yieldResolver(
        spendableDelta: BigDecimal,
        status: YieldSupplyStatus = ACTIVE_STATUS,
        source: StatusSource = StatusSource.ACTUAL,
    ) = ContributionResolver { resolved, resolvedStatus ->
        yieldContribution(spendableDelta = spendableDelta, status = status, source = source)
            .takeIf { resolved == currency && resolvedStatus == networkStatus }
    }

    private fun yieldContribution(
        spendableDelta: BigDecimal,
        status: YieldSupplyStatus = ACTIVE_STATUS,
        source: StatusSource = StatusSource.ACTUAL,
    ) = YieldSupplyContribution(status = status, source = source, spendableDelta = spendableDelta)

    private class FakeProvider(private val frames: Flow<ContributionResolver>) : BalanceContributionProvider {

        override fun contributions(userWallet: UserWallet): Flow<ContributionResolver> = frames
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val NETWORK_ID = Network.ID(
            value = "polygon-pos",
            derivationPath = Network.DerivationPath.Custom("m/44'/60'/999998'/0/0"),
        )
        val WALLET_AMOUNT = BigDecimal("5")
        val REFRESHED_WALLET_AMOUNT = BigDecimal("8")
        val SUPPLIED = BigDecimal("10")
        val ACTIVE_STATUS = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = SUPPLIED,
        )
    }
}