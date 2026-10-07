package com.tangem.features.yield.supply.impl.entry.model

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.tokens.model.details.NavigationAction
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import com.tangem.domain.yield.supply.promo.usecase.IsYieldBoostPromoEnabledForTokenUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEnterStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.features.yield.supply.api.YieldSupplyEntryComponent
import com.tangem.features.yield.supply.api.entry.YieldSupplyEntryRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyEntryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val enterStatusUseCase: YieldSupplyEnterStatusUseCase = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val isPromoEnabledUseCase: IsYieldBoostPromoEnabledForTokenUseCase = mockk()
    private val getTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase = mockk()

    private val accountStatusList: AccountStatusList = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(
            router, enterStatusUseCase, accountStatusListSupplier,
            isPromoEnabledUseCase, getTokenStatusUseCase,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        coEvery { accountStatusListSupplier.getSyncOrNull(USER_WALLET_ID) } returns accountStatusList
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
    }

    @Test
    fun `GIVEN currency status not found WHEN created THEN pops without navigating`() = runTest {
        // Arrange
        stubStatusLookup(none())

        // Act
        createModel(currency = token())

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        verify(exactly = 0) { router.replaceCurrent(any(), any()) }
    }

    @Test
    fun `GIVEN currency is not a token WHEN created THEN pops without navigating`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = false).some())

        // Act
        createModel(currency = coin())

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        verify(exactly = 0) { router.replaceCurrent(any(), any()) }
    }

    @Test
    fun `GIVEN pending enter status and active yield WHEN created THEN navigates to currency details active`() =
        runTest {
            // Arrange
            stubStatusLookup(status(isActive = true).some())
            coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns pendingEnter().right()

            // Act
            createModel(currency = token())

            // Assert
            val route = captureReplacedRoute()
            assertThat(route).isInstanceOf(AppRoute.CurrencyDetails::class.java)
            assertThat((route as AppRoute.CurrencyDetails).navigationAction)
                .isEqualTo(NavigationAction.YieldSupply(isActive = true))
            assertThat(route.userWalletId).isEqualTo(USER_WALLET_ID)
            assertThat(route.currency).isEqualTo(token())
        }

    @Test
    fun `GIVEN pending enter status and inactive yield WHEN created THEN currency details with inactive flag`() =
        runTest {
            // Arrange
            stubStatusLookup(status(isActive = false).some())
            coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns pendingEnter().right()

            // Act
            createModel(currency = token())

            // Assert
            val route = captureReplacedRoute()
            assertThat((route as AppRoute.CurrencyDetails).navigationAction)
                .isEqualTo(NavigationAction.YieldSupply(isActive = false))
        }

    @Test
    fun `GIVEN no pending status and active yield WHEN created THEN navigates to Active route`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = true).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()

        // Act
        createModel(currency = token())

        // Assert
        val route = captureReplacedRoute()
        assertThat(route).isInstanceOf(YieldSupplyEntryRoute.Active::class.java)
        assertThat((route as YieldSupplyEntryRoute.Active).cryptoCurrency).isEqualTo(token())
    }

    @Test
    fun `GIVEN enter status use case fails WHEN created THEN coerced to no pending and routes to Active`() = runTest {
        // Arrange — a Left is coerced to null by getOrNull, so it must NOT route to CurrencyDetails
        stubStatusLookup(status(isActive = true).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns Throwable("boom").left()

        // Act
        createModel(currency = token())

        // Assert
        assertThat(captureReplacedRoute()).isInstanceOf(YieldSupplyEntryRoute.Active::class.java)
    }

    @Test
    fun `GIVEN no pending status and inactive yield with promo enabled WHEN created THEN Promo route promo-enabled`() =
        runTest {
            // Arrange
            stubStatusLookup(status(isActive = false).some())
            coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()
            coEvery { isPromoEnabledUseCase(USER_WALLET_ID, any()) } returns true.right()
            coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = false).right()

            // Act
            createModel(currency = token())

            // Assert
            val route = captureReplacedRoute()
            assertThat(route).isInstanceOf(YieldSupplyEntryRoute.Promo::class.java)
            assertThat((route as YieldSupplyEntryRoute.Promo).isPromoEnabled).isTrue()
            assertThat(route.cryptoCurrency).isEqualTo(token())
        }

    @Test
    fun `GIVEN promo use case returns false WHEN created THEN Promo route with promo disabled`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = false).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()
        coEvery { isPromoEnabledUseCase(USER_WALLET_ID, any()) } returns false.right()

        // Act
        createModel(currency = token())

        // Assert
        assertThat((captureReplacedRoute() as YieldSupplyEntryRoute.Promo).isPromoEnabled).isFalse()
    }

    @Test
    fun `GIVEN promo enabled and active campaign WHEN created THEN Promo route carries the base apy`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = false).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()
        coEvery { isPromoEnabledUseCase(USER_WALLET_ID, any()) } returns true.right()
        coEvery { getTokenStatusUseCase(any()) } returns market(hasCampaign = true).right()

        // Act
        createModel(currency = token())

        // Assert
        assertThat((captureReplacedRoute() as YieldSupplyEntryRoute.Promo).apy).isEqualTo("4")
    }

    @Test
    fun `GIVEN promo enabled and no market WHEN created THEN Promo route falls back to the params apy`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = false).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()
        coEvery { isPromoEnabledUseCase(USER_WALLET_ID, any()) } returns true.right()
        coEvery { getTokenStatusUseCase(any()) } returns IllegalStateException("no market").left()

        // Act
        createModel(currency = token())

        // Assert
        assertThat((captureReplacedRoute() as YieldSupplyEntryRoute.Promo).apy).isEqualTo("5.0")
    }

    @Test
    fun `GIVEN promo disabled WHEN created THEN Promo route keeps the params apy without a market lookup`() = runTest {
        // Arrange
        stubStatusLookup(status(isActive = false).some())
        coEvery { enterStatusUseCase(USER_WALLET_ID, any()) } returns null.right()
        coEvery { isPromoEnabledUseCase(USER_WALLET_ID, any()) } returns false.right()

        // Act
        createModel(currency = token())

        // Assert
        assertThat((captureReplacedRoute() as YieldSupplyEntryRoute.Promo).apy).isEqualTo("5.0")
        coVerify(exactly = 0) { getTokenStatusUseCase(any()) }
    }

    private fun market(hasCampaign: Boolean) = YieldMarketToken(
        tokenAddress = "0xToken",
        chainId = 1,
        apy = BigDecimal("4"),
        isActive = true,
        maxFeeNative = BigDecimal.ZERO,
        maxFeeUSD = BigDecimal.ZERO,
        campaign = YieldIncentiveCampaign(
            hasActiveCampaign = hasCampaign,
            rewardsApr = BigDecimal("1.5"),
            totalApyEst = BigDecimal("5.5"),
        ),
    )

    private fun captureReplacedRoute(): Route {
        val slot = slot<Route>()
        verify { router.replaceCurrent(capture(slot), any()) }
        return slot.captured
    }

    private fun stubStatusLookup(result: arrow.core.Option<CryptoCurrencyStatus>) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns result
    }

    private fun createModel(currency: CryptoCurrency): YieldSupplyEntryModel = YieldSupplyEntryModel(
        paramsContainer = MutableParamsContainer(
            YieldSupplyEntryComponent.Params(userWalletId = USER_WALLET_ID, cryptoCurrency = currency, apy = "5.0"),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        router = router,
        yieldSupplyEnterStatusUseCase = enterStatusUseCase,
        singleAccountStatusListSupplier = accountStatusListSupplier,
        isYieldBoostPromoEnabledForTokenUseCase = isPromoEnabledUseCase,
        yieldSupplyGetTokenStatusUseCase = getTokenStatusUseCase,
    )

    private fun pendingEnter(): YieldSupplyPendingStatus = YieldSupplyPendingStatus.Enter(txIds = listOf("0xTx"))

    private fun status(isActive: Boolean): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = token(),
        value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.ZERO,
            fiatAmount = BigDecimal.ZERO,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = YieldSupplyStatus(
                isActive = isActive,
                isInitialized = true,
                isAllowedToSpend = true,
                effectiveProtocolBalance = null,
            ),
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "0x0000000000000000000000000000000000000000",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun token(): CryptoCurrency.Token = CryptoCurrency.Token(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_TOKEN",
        symbol = "TTK",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xToken",
    )

    private fun coin(): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        ),
        network = network(),
        name = "TEST_COIN",
        symbol = "ETH",
        decimals = 18,
        iconUrl = null,
        isCustom = false,
    )

    private fun network(): Network {
        val derivationPath = Network.DerivationPath.None
        return Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("abcdef012345")
    }
}