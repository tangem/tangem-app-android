package com.tangem.features.earn.staking.impl

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.staking.impl.fixtures.coin
import com.tangem.features.earn.staking.impl.fixtures.currencyStatus
import com.tangem.features.earn.staking.impl.fixtures.polygonToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class PooledStakingEarnProductTest {

    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val stakingRepository: StakingRepository = mockk()
    private val p2pEthPoolRepository: P2PEthPoolRepository = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val singleStakingBalanceFetcher: SingleStakingBalanceFetcher = mockk()

    private val product = PooledStakingEarnProduct(
        accountStatusListSupplier = accountStatusListSupplier,
        stakingRepository = stakingRepository,
        p2pEthPoolRepository = p2pEthPoolRepository,
        stakingIdFactory = stakingIdFactory,
        singleStakingBalanceFetcher = singleStakingBalanceFetcher,
        mapper = PooledStakingMapper(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            accountStatusListSupplier,
            stakingRepository,
            p2pEthPoolRepository,
            stakingIdFactory,
            singleStakingBalanceFetcher,
        )
    }

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val ethereum = coin(Blockchain.Ethereum)
    private val secondEthereum = secondAccountOf(ethereum)
    private val available = StakingAvailability.Available(
        StakingOption.P2PEthPool(vaults = listOf(MockStaking.vault())),
    )

    @Test
    fun `GIVEN product WHEN reading metadata THEN identity, title, provider and legacy block flag of pooled staking`() {
        // Assert
        assertThat(product.id).isEqualTo(EarnProductId("staking"))
        assertThat(product.rank).isEqualTo(1)
        assertThat(product.title).isEqualTo(stringReference("Pooled staking"))
        assertThat(product.provider).isEqualTo(
            EarnProvider(title = stringReference("P2P.org"), icon = ImageReference.Res(R.drawable.ic_p2p_logo)),
        )
        assertThat(product.hasLegacyBlock).isTrue()
        assertThat(product.analytics).isEqualTo(EarnProductAnalytics(category = "ETH Staking", product = "Staking"))
    }

    @Test
    fun `GIVEN two ETH coins and a Solana coin and a token WHEN summaries THEN entries only for the two ETH coins`() =
        runTest {
            // Arrange
            val solana = currencyStatus(currency = coin(Blockchain.Solana))
            val token = currencyStatus(currency = polygonToken())
            val staked = status(ethereum, MockStaking.p2pBalance(accounts = listOf(MockStaking.p2pAccount())))
            val unstaked = status(secondEthereum)
            every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(staked, unstaked, solana, token))
            every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns flowOf(available)
            every { stakingRepository.getStakingAvailability(userWalletId, secondEthereum) } returns flowOf(available)

            // Act
            val actual = product.summaries(userWalletId).first()

            // Assert
            assertThat(actual.keys).containsExactly(ethereum.id, secondEthereum.id)
            assertThat(actual.getValue(ethereum.id).position).isEqualTo(EarnPositionSummary(BigDecimal.ONE))
            assertThat(actual.getValue(secondEthereum.id).position).isNull()
            assertThat(actual.getValue(secondEthereum.id).route).isEqualTo(stakingRoute(userWalletId, secondEthereum))
            verify(exactly = 0) { stakingRepository.getStakingAvailability(userWalletId, solana.currency) }
            verify(exactly = 0) { stakingRepository.getStakingAvailability(userWalletId, token.currency) }
        }

    @Test
    fun `GIVEN no pool currency WHEN summaries THEN empty map`() = runTest {
        // Arrange
        every { accountStatusListSupplier(userWalletId) } returns
            flowOf(accountList(currencyStatus(currency = coin(Blockchain.Solana))))

        // Act
        val actual = product.summaries(userWalletId).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN availability flips from Available to TemporaryUnavailable WHEN summaries THEN second map is empty`() =
        runTest {
            // Arrange
            val availability = MutableSharedFlow<StakingAvailability>(replay = 1)
            availability.tryEmit(available)
            every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(status(ethereum)))
            every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns availability

            // Act & Assert
            product.summaries(userWalletId).test {
                assertThat(awaitItem().keys).containsExactly(ethereum.id)
                availability.emit(StakingAvailability.TemporaryUnavailable)
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN a status tick without new currencies WHEN summaries THEN availability is kept and position updates`() =
        runTest {
            // Arrange
            val ticks = MutableSharedFlow<AccountStatusList>(replay = 1)
            ticks.tryEmit(accountList(status(ethereum)))
            every { accountStatusListSupplier(userWalletId) } returns ticks
            every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns flowOf(available)
            val staked = status(ethereum, MockStaking.p2pBalance(accounts = listOf(MockStaking.p2pAccount())))

            // Act & Assert
            product.summaries(userWalletId).test {
                assertThat(awaitItem().getValue(ethereum.id).position).isNull()
                ticks.emit(accountList(staked))
                assertThat(awaitItem().getValue(ethereum.id).position).isEqualTo(EarnPositionSummary(BigDecimal.ONE))
                verify(exactly = 1) { stakingRepository.getStakingAvailability(userWalletId, ethereum) }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GIVEN availability WHEN state THEN Loading first then Ready and Unavailable after the flip`() = runTest {
        // Arrange
        val params = EarnProduct.Params(userWalletId, ethereum)
        val availability = MutableSharedFlow<StakingAvailability>(replay = 1)
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(status(ethereum)))
        every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns availability

        // Act & Assert
        product.state(params).test {
            assertThat(awaitItem()).isEqualTo(EarnProductState.Loading)
            availability.emit(available)
            assertThat(awaitItem()).isInstanceOf(EarnProductState.Ready::class.java)
            availability.emit(StakingAvailability.TemporaryUnavailable)
            assertThat(awaitItem()).isEqualTo(EarnProductState.Unavailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN currency not in the account list WHEN state THEN Loading then Unavailable`() = runTest {
        // Arrange
        val params = EarnProduct.Params(userWalletId, ethereum)
        every { accountStatusListSupplier(userWalletId) } returns
            flowOf(accountList(currencyStatus(currency = coin(Blockchain.Solana))))
        every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns flowOf(available)

        // Act & Assert
        product.state(params).test {
            assertThat(awaitItem()).isEqualTo(EarnProductState.Loading)
            assertThat(awaitItem()).isEqualTo(EarnProductState.Unavailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN currency removed from the account list WHEN state THEN Ready then Unavailable`() = runTest {
        // Arrange
        val params = EarnProduct.Params(userWalletId, ethereum)
        val accountLists = MutableSharedFlow<AccountStatusList>(replay = 1)
        accountLists.tryEmit(accountList(status(ethereum)))
        every { accountStatusListSupplier(userWalletId) } returns accountLists
        every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns flowOf(available)

        // Act & Assert
        product.state(params).test {
            assertThat(awaitItem()).isEqualTo(EarnProductState.Loading)
            assertThat(awaitItem()).isInstanceOf(EarnProductState.Ready::class.java)
            accountLists.emit(accountList())
            assertThat(awaitItem()).isEqualTo(EarnProductState.Unavailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN any action WHEN actionHandler onActionClick THEN navigator gets exactly AppRoute Staking`() = runTest {
        // Arrange
        val params = EarnProduct.Params(userWalletId, ethereum)
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(status(ethereum)))
        every { stakingRepository.getStakingAvailability(userWalletId, ethereum) } returns flowOf(available)
        val summary = product.summaries(userWalletId).first().getValue(ethereum.id)
        val ready = product.state(params).first { it is EarnProductState.Ready } as EarnProductState.Ready
        val navigator: EarnNavigator = mockk(relaxed = true)
        val expectedRoute = stakingRoute(userWalletId, ethereum)

        // Act
        val handler = product.actionHandler(params, navigator)
        handler.onActionClick(summary.onClickAction)
        handler.onActionClick(ready.primary.action)
        handler.onActionClick(Deposit())

        // Assert
        assertThat(summary.route).isEqualTo(expectedRoute)
        verify(exactly = 3) { navigator.navigate(expectedRoute) }
        confirmVerified(navigator)
    }

    @Test
    fun `GIVEN two handlers with different navigators WHEN each is clicked THEN only its own navigator is called`() {
        // Arrange
        val params = EarnProduct.Params(userWalletId, ethereum)
        val first: EarnNavigator = mockk(relaxed = true)
        val second: EarnNavigator = mockk(relaxed = true)
        val action = PooledStakingAction(stringReference("Manage"))
        val firstHandler = product.actionHandler(params, first)
        val secondHandler = product.actionHandler(params, second)

        // Act
        firstHandler.onActionClick(action)

        // Assert
        verify(exactly = 1) { first.navigate(any()) }
        verify(exactly = 0) { second.navigate(any()) }

        // Act
        secondHandler.onActionClick(action)

        // Assert
        verify(exactly = 1) { first.navigate(any()) }
        verify(exactly = 1) { second.navigate(any()) }
    }

    @Test
    fun `GIVEN any period WHEN apyHistory THEN Right with empty history`() = runTest {
        // Act
        val actual = product.apyHistory(
            params = EarnProduct.Params(userWalletId, ethereum),
            period = EarnPeriod(id = "1m", label = stringReference("1M")),
        )

        // Assert
        assertThat(actual).isEqualTo(EarnApyHistory(points = emptyList(), average = null).right())
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Refresh {

        private val stakingId = StakingID(integrationId = MockStaking.P2P_ID, address = MockStaking.DELEGATOR_ADDRESS)
        private val params = EarnProduct.Params(userWalletId, ethereum)

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a failing step WHEN refresh THEN Left`(model: RefreshModel) = runTest {
            // Arrange
            arrange(model)

            // Act
            val actual = product.refresh(params)

            // Assert
            assertThat(actual.isLeft()).isTrue()
        }

        @Test
        fun `GIVEN all steps succeed WHEN refresh THEN Right and the fetcher got the StakingID of the wallet`() =
            runTest {
                // Arrange
                arrange(RefreshModel(name = "success"))

                // Act
                val actual = product.refresh(params)

                // Assert
                assertThat(actual).isEqualTo(Unit.right())
                coVerify(exactly = 1) {
                    singleStakingBalanceFetcher(SingleStakingBalanceFetcher.Params(userWalletId, stakingId))
                }
            }

        @Test
        fun `GIVEN fetchVaults throws CancellationException WHEN refresh THEN the exception is rethrown`() = runTest {
            // Arrange
            arrange(RefreshModel(name = "cancelled", vaultsFailure = CancellationException("cancelled")))

            // Act
            val thrown = runCatching { product.refresh(params) }.exceptionOrNull()

            // Assert
            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

        private fun arrange(model: RefreshModel) {
            coEvery { p2pEthPoolRepository.fetchVaults(any()) } answers {
                model.vaultsFailure?.let { throw it }
                model.vaultsError?.left() ?: Unit.right()
            }
            coEvery { p2pEthPoolRepository.fetchVaultLimits() } answers {
                model.limitsFailure?.let { throw it }
                model.limitsError?.left() ?: Unit.right()
            }
            coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns
                (model.idFailure?.left() ?: stakingId.right())
            coEvery { singleStakingBalanceFetcher(any()) } returns
                (model.fetcherFailure?.left() ?: Unit.right())
        }

        @Test
        fun `GIVEN an unsupported staking id WHEN refresh THEN Right and the balance is not fetched`() = runTest {
            // Arrange
            arrange(RefreshModel(name = "unsupported", idFailure = StakingIdFactory.Error.UnsupportedCurrency))

            // Act
            val actual = product.refresh(params)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) { singleStakingBalanceFetcher(any()) }
        }

        private fun provideTestModels() = listOf(
            RefreshModel(name = "fetchVaults throws", vaultsFailure = IllegalStateException("vaults")),
            RefreshModel(name = "fetchVaultLimits throws", limitsFailure = IllegalStateException("limits")),
            RefreshModel(name = "vaults request fails", vaultsError = StakingError.DomainError("vaults")),
            RefreshModel(name = "limits request fails", limitsError = StakingError.DomainError("limits")),
            RefreshModel(
                name = "no address",
                idFailure = StakingIdFactory.Error.UnableToGetAddress(StakingIntegrationID.P2PEthPool),
            ),
            RefreshModel(name = "balance fetcher fails", fetcherFailure = IllegalStateException("balance")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RefreshOfOtherCurrencies {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a currency that is not a pool currency WHEN refresh THEN Right and nothing is fetched`(
            model: CurrencyModel,
        ) = runTest {
            // Act
            val actual = product.refresh(EarnProduct.Params(userWalletId, model.currency))

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) { p2pEthPoolRepository.fetchVaults(any()) }
            coVerify(exactly = 0) { p2pEthPoolRepository.fetchVaultLimits() }
            coVerify(exactly = 0) { stakingIdFactory.create(any<UserWalletId>(), any<CryptoCurrency>()) }
            coVerify(exactly = 0) { singleStakingBalanceFetcher(any()) }
        }

        private fun provideTestModels() = listOf(
            CurrencyModel(name = "Bitcoin coin", currency = coin(Blockchain.Bitcoin)),
            CurrencyModel(name = "Solana coin", currency = coin(Blockchain.Solana)),
            CurrencyModel(name = "Ethereum token", currency = polygonToken()),
        )
    }

    private fun status(currency: CryptoCurrency, position: StakingBalance? = null): CryptoCurrencyStatus =
        currencyStatus(currency = currency, stakingBalance = position)

    private fun accountList(vararg statuses: CryptoCurrencyStatus): AccountStatusList = mockk {
        every { flattenCurrencies() } returns statuses.toList()
    }

    private fun secondAccountOf(currency: CryptoCurrency.Coin): CryptoCurrency.Coin {
        val id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                rawId = currency.id.rawNetworkId,
                derivationPath = SECOND_ACCOUNT_PATH,
            ),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawId = requireNotNull(currency.id.rawCurrencyId).value),
        )
        return currency.copy(id = id)
    }

    internal data class CurrencyModel(val name: String, val currency: CryptoCurrency) {
        override fun toString(): String = name
    }

    internal data class RefreshModel(
        val name: String,
        val vaultsFailure: Throwable? = null,
        val limitsFailure: Throwable? = null,
        val vaultsError: StakingError? = null,
        val limitsError: StakingError? = null,
        val idFailure: StakingIdFactory.Error? = null,
        val fetcherFailure: Throwable? = null,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val SECOND_ACCOUNT_PATH = "m/44'/60'/1'/0/0"
    }
}