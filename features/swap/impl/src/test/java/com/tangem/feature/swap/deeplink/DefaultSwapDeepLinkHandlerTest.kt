package com.tangem.feature.swap.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_AMOUNT_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_USER_ACCOUNT_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.FROM_USER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.PROVIDER_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TO_TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.account.status.usecase.GetWalletTotalBalanceUseCase
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.logging.TangemLogger
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSwapDeepLinkHandlerTest {

    private val router: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val selectWalletUseCase: SelectWalletUseCase = mockk()
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val rampStateManager: RampStateManager = mockk()
    private val swapInteractor: SwapInteractor = mockk()
    private val swapFeatureToggles: SwapFeatureToggles = mockk()

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        mockkObject(TangemLogger)
        every { TangemLogger.e(any<String>()) } just Runs
        every { TangemLogger.e(any<String>(), any()) } just Runs
        every { swapFeatureToggles.isSwapDeeplinkEnabled } returns true
        every { getSelectedWalletSyncUseCase() } returns Either.Right(wallet(walletId))
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId))
        coEvery { selectWalletUseCase(any()) } answers { Either.Right(wallet(firstArg())) }
        every { getWalletTotalBalanceUseCase(any()) } returns flowOf(emptyMap<UserWalletId, TotalFiatBalance>().lceContent())
        every { getUserWalletUseCase(any()) } answers { Either.Right(wallet(firstArg())) }
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) } returns null
        coEvery { rampStateManager.availableForSwap(any(), any<List<CryptoCurrency>>()) } answers {
            secondArg<List<CryptoCurrency>>().associateWith { ScenarioUnavailabilityReason.None }
        }
        coEvery { swapInteractor.getPair(any(), any(), any()) } returns Either.Right(emptyList())
        coEvery { swapInteractor.findProvidersForPairWithCheck(any(), any(), any()) } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    private fun wallet(id: UserWalletId, locked: Boolean = false): UserWallet = mockk<UserWallet.Cold> {
        every { this@mockk.walletId } returns id
        every { isLocked } returns locked
    }

    private fun bareSwap(id: UserWalletId) = AppRoute.Swap(
        userWalletId = id,
        screenSource = AnalyticsParam.ScreensSources.Main.value,
    )

    /**
     * Builds a real coin [CryptoCurrency] matching [tokenId]/[networkId] via the ID/network raw fields.
     * [decimals] does not participate in [matchesToken] matching — vary it across accounts in
     * multi-account tests so two same-token/network instances remain distinguishable by full equality.
     */
    private fun currency(tokenId: String, networkId: String, decimals: Int = 8): CryptoCurrency.Coin {
        val network = Network(
            id = Network.ID(value = networkId, derivationPath = Network.DerivationPath.None),
            name = networkId,
            currencySymbol = tokenId,
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified(networkId),
            hasFiatFeeRate = false,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(networkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(tokenId),
            ),
            network = network,
            name = tokenId,
            symbol = tokenId,
            decimals = decimals,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun currencyStatus(currency: CryptoCurrency, fiatAmount: BigDecimal = BigDecimal.TEN): CryptoCurrencyStatus {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(value = "0x0", type = NetworkAddress.Address.Type.Primary),
        )
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = fiatAmount,
                fiatAmount = fiatAmount,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                stakingBalance = null,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = networkAddress,
                sources = CryptoCurrencyStatus.Sources(),
            ),
        )
    }

    /** Stubs [singleAccountStatusListSupplier] to return a single crypto-portfolio account with [statuses]. */
    private fun givenPortfolio(id: AccountId = mainAccountId, statuses: List<CryptoCurrencyStatus>) {
        val account: Account.CryptoPortfolio = mockk { every { accountId } returns id }
        val portfolio: AccountStatus.CryptoPortfolio = mockk {
            every { this@mockk.account } returns account
            every { flattenCurrencies() } returns statuses
        }
        val accountList: AccountStatusList = mockk { every { accountStatuses } returns listOf(portfolio) }
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(walletId) } returns accountList
    }

    private val mainAccountId = AccountId.forMainCryptoPortfolio(walletId)
    private val secondAccountId = AccountId.forCryptoPortfolio(walletId, value = "b".repeat(64)).getOrNull()!!

    /** Stubs [singleAccountStatusListSupplier] with two crypto-portfolio accounts ([mainAccountId] + [secondAccountId]). */
    private fun givenMultiAccountPortfolio(
        accountAStatuses: List<CryptoCurrencyStatus>,
        accountBStatuses: List<CryptoCurrencyStatus>,
    ) {
        fun portfolioOf(id: AccountId, statuses: List<CryptoCurrencyStatus>): AccountStatus.CryptoPortfolio {
            val account: Account.CryptoPortfolio = mockk { every { accountId } returns id }
            return mockk {
                every { this@mockk.account } returns account
                every { flattenCurrencies() } returns statuses
            }
        }
        val accountList: AccountStatusList = mockk {
            every { accountStatuses } returns listOf(
                portfolioOf(mainAccountId, accountAStatuses),
                portfolioOf(secondAccountId, accountBStatuses),
            )
        }
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(walletId) } returns accountList
    }

    private fun createHandler(scope: TestScope, queryParams: Map<String, String>): DefaultSwapDeepLinkHandler =
        DefaultSwapDeepLinkHandler(
            scope = scope,
            queryParams = queryParams,
            router = router,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            getWalletsUseCase = getWalletsUseCase,
            selectWalletUseCase = selectWalletUseCase,
            getWalletTotalBalanceUseCase = getWalletTotalBalanceUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            singleAccountStatusListSupplier = singleAccountStatusListSupplier,
            rampStateManager = rampStateManager,
            swapInteractor = swapInteractor,
            swapFeatureToggles = swapFeatureToggles,
        )

    @Test
    fun `GIVEN toggle OFF WHEN handle THEN bare swap for selected`() = runTest {
        // Arrange
        every { swapFeatureToggles.isSwapDeeplinkEnabled } returns false
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { selectWalletUseCase(any()) }
    }

    @Test
    fun `GIVEN toggle OFF and no selected wallet WHEN handle THEN nothing happens`() = runTest {
        // Arrange
        every { swapFeatureToggles.isSwapDeeplinkEnabled } returns false
        every { getSelectedWalletSyncUseCase() } returns Either.Left(GetUserWalletError.UserWalletNotFound)

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
    }

    @Test
    fun `GIVEN no params and wallet selected WHEN handle THEN bare swap for selected`() = runTest {
        // Arrange
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { selectWalletUseCase(any()) }
    }

    @Test
    fun `GIVEN broadcast wallet id for existing other wallet WHEN handle THEN switches and bare swap for it`() =
        runTest {
            // Arrange
            val other = UserWalletId("022")
            every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(other))
            val expected = bareSwap(other)

            // Act
            createHandler(this, mapOf(WALLET_ID_KEY to other.stringValue))
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
            coVerify(exactly = 1) { selectWalletUseCase(other) }
        }

    @Test
    fun `GIVEN broadcast wallet id ne existing from wallet id WHEN handle THEN main for selected without switch`() =
        runTest {
            // Arrange
            val other = UserWalletId("022")
            every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(other))
            val expected = bareSwap(walletId)

            // Act
            createHandler(
                this,
                mapOf(WALLET_ID_KEY to walletId.stringValue, FROM_USER_WALLET_ID_KEY to other.stringValue),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
            coVerify(exactly = 0) { selectWalletUseCase(any()) }
        }

    @Test
    fun `GIVEN non-existent from wallet id WHEN handle THEN degrades to bare swap for selected`() = runTest {
        // Arrange
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, mapOf(FROM_USER_WALLET_ID_KEY to "099"))
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { selectWalletUseCase(any()) }
    }

    @Test
    fun `GIVEN cold-start WHEN handle THEN picks wallet with largest balance and switches`() = runTest {
        // Arrange: first emission is Loading for both wallets (as at a real cold-start), settled Loaded
        // comes second — the handler must await the settled emission, not grab the first (Loading) one.
        val richer = UserWalletId("022")
        every { getSelectedWalletSyncUseCase() } returns Either.Left(GetUserWalletError.UserWalletNotFound)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(richer))
        every { getWalletTotalBalanceUseCase(any()) } returns flowOf(
            mapOf(
                walletId to TotalFiatBalance.Loading,
                richer to TotalFiatBalance.Loading,
            ).lceContent(),
            mapOf(
                walletId to TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
                richer to TotalFiatBalance.Loaded(amount = BigDecimal("1000"), source = StatusSource.ACTUAL),
            ).lceContent(),
        )
        val expected = bareSwap(richer)

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 1) { selectWalletUseCase(richer) }
    }

    @Test
    fun `GIVEN cold-start and select fails WHEN handle THEN bare swap for target without switch confirmation`() =
        runTest {
            // Arrange
            val richer = UserWalletId("022")
            every { getSelectedWalletSyncUseCase() } returns Either.Left(GetUserWalletError.UserWalletNotFound)
            every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(richer))
            every { getWalletTotalBalanceUseCase(any()) } returns flowOf(
                mapOf(
                    richer to TotalFiatBalance.Loaded(amount = BigDecimal("1000"), source = StatusSource.ACTUAL),
                ).lceContent(),
            )
            coEvery { selectWalletUseCase(richer) } returns Either.Left(mockk(relaxed = true))
            val expected = bareSwap(richer)

            // Act
            createHandler(this, emptyMap())
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    @Test
    fun `GIVEN no wallets at all WHEN handle THEN nothing happens`() = runTest {
        // Arrange
        every { getWalletsUseCase.invokeSync() } returns emptyList()

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
    }

    @Test
    fun `GIVEN cold-start with a locked richer wallet WHEN handle THEN selects the unlocked wallet`() = runTest {
        // Arrange: W2 is locked and would look richer, but must be excluded from the balance lookup
        // entirely — only the unlocked W1 is a valid cold-start pick.
        val locked = UserWalletId("022")
        every { getSelectedWalletSyncUseCase() } returns Either.Left(GetUserWalletError.UserWalletNotFound)
        every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(locked, locked = true))
        every { getWalletTotalBalanceUseCase(any()) } returns flowOf(
            mapOf(
                walletId to TotalFiatBalance.Loaded(amount = BigDecimal.TEN, source = StatusSource.ACTUAL),
            ).lceContent(),
        )
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, emptyMap())
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 1) { selectWalletUseCase(walletId) }
        coVerify(exactly = 0) { selectWalletUseCase(locked) }
        coVerify(exactly = 1) { getWalletTotalBalanceUseCase(setOf(walletId)) }
    }

    @Test
    fun `GIVEN explicit user_wallet_id points to a locked wallet WHEN handle THEN falls back to current wallet without switching`() =
        runTest {
            // Arrange
            val locked = UserWalletId("022")
            every { getWalletsUseCase.invokeSync() } returns listOf(wallet(walletId), wallet(locked, locked = true))
            val expected = bareSwap(walletId)

            // Act
            createHandler(this, mapOf(WALLET_ID_KEY to locked.stringValue))
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
            coVerify(exactly = 0) { selectWalletUseCase(locked) }
        }

    // region Task 5: token resolution + matrix gating + amount + position

    @Test
    fun `GIVEN from token without network WHEN handle THEN main`() = runTest {
        // Arrange (case 2)
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, mapOf(FROM_TOKEN_ID_KEY to "btc"))
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN both tokens in portfolio WHEN handle THEN swap with both and position FROM`() = runTest {
        // Arrange (cases 3-4, both resolved)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(
            statuses = listOf(
                currencyStatus(fromCurrency, fiatAmount = BigDecimal.TEN),
                currencyStatus(toCurrency, fiatAmount = BigDecimal.ONE),
            ),
        )
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
        )

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN from not in portfolio and to in portfolio WHEN handle THEN from null to concrete position TO`() =
        runTest {
            // Arrange (case 4, FROM missing)
            val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
            givenPortfolio(statuses = listOf(currencyStatus(toCurrency)))
            val expected = bareSwap(walletId).copy(
                toCryptoCurrency = toCurrency,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
            )

            // Act
            createHandler(
                this,
                mapOf(
                    FROM_TOKEN_ID_KEY to "btc",
                    FROM_NETWORK_ID_KEY to "bitcoin",
                    TO_TOKEN_ID_KEY to "eth",
                    TO_NETWORK_ID_KEY to "ethereum",
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    @Test
    fun `GIVEN neither token in portfolio WHEN handle THEN main`() = runTest {
        // Arrange (case 4, both missing)
        givenPortfolio(statuses = emptyList())
        val expected = bareSwap(walletId)

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN amount only WHEN handle THEN main`() = runTest {
        // Arrange (case 6)
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, mapOf(FROM_AMOUNT_KEY to "10"))
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN provider only WHEN handle THEN main`() = runTest {
        // Arrange (case 9)
        val expected = bareSwap(walletId)

        // Act
        createHandler(this, mapOf(PROVIDER_ID_KEY to "changelly"))
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { singleAccountStatusListSupplier.getSyncOrNull(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN to and amount WHEN handle THEN from null amount dropped`() = runTest {
        // Arrange (case 7)
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(statuses = listOf(currencyStatus(toCurrency)))
        val expected = bareSwap(walletId).copy(
            toCryptoCurrency = toCurrency,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
        )

        // Act
        createHandler(
            this,
            mapOf(TO_TOKEN_ID_KEY to "eth", TO_NETWORK_ID_KEY to "ethereum", FROM_AMOUNT_KEY to "10"),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN from and amount WHEN handle THEN to null amount dropped position ANY`() = runTest {
        // Arrange (case 8)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency)))
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.ANY,
        )

        // Act
        createHandler(
            this,
            mapOf(FROM_TOKEN_ID_KEY to "btc", FROM_NETWORK_ID_KEY to "bitcoin", FROM_AMOUNT_KEY to "10"),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN both concrete and amount WHEN handle THEN from amount applied`() = runTest {
        // Arrange (A2: full pair + amount)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency), currencyStatus(toCurrency)))
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            fromAmount = BigDecimal("10"),
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
        )

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
                FROM_AMOUNT_KEY to "10",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    // endregion

    // region Task 6: provider pre-check

    /** Minimal [SwapProvider] with the given [id], only [SwapProvider.providerId] matters for these tests. */
    private fun swapProvider(id: String): SwapProvider = mockk { every { providerId } returns id }

    @Test
    fun `GIVEN both concrete and provider in pair providers WHEN handle THEN providerId applied`() = runTest {
        // Arrange (§7: provider available for the resolved pair)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency), currencyStatus(toCurrency)))
        val pair: SwapPairLeast = mockk()
        coEvery { swapInteractor.getPair(any(), any(), any()) } returns Either.Right(listOf(pair))
        coEvery { swapInteractor.findProvidersForPairWithCheck(any(), any(), listOf(pair)) } returns
            listOf(swapProvider("changelly"))
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            providerId = "changelly",
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
        )

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
                PROVIDER_ID_KEY to "CHANGELLY",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN both concrete and provider not in pair providers WHEN handle THEN main`() = runTest {
        // Arrange (§7: requested provider doesn't serve the resolved pair)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency), currencyStatus(toCurrency)))
        val pair: SwapPairLeast = mockk()
        coEvery { swapInteractor.getPair(any(), any(), any()) } returns Either.Right(listOf(pair))
        coEvery { swapInteractor.findProvidersForPairWithCheck(any(), any(), listOf(pair)) } returns
            listOf(swapProvider("changelly"))
        val expected = bareSwap(walletId)

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
                PROVIDER_ID_KEY to "1inch",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
    }

    @Test
    fun `GIVEN both concrete and no provider WHEN handle THEN providerId null and no pair lookup`() = runTest {
        // Arrange
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        val toCurrency = currency(tokenId = "eth", networkId = "ethereum")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency), currencyStatus(toCurrency)))
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
        )

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                TO_TOKEN_ID_KEY to "eth",
                TO_NETWORK_ID_KEY to "ethereum",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { swapInteractor.getPair(any<SwapCurrencyStatus>(), any<SwapCurrencyStatus>(), any()) }
    }

    // endregion

    // region assumptions A1 / A3: degraded TO / dropped provider with only FROM concrete

    @Test
    fun `GIVEN from concrete and to token without network WHEN handle THEN to null from applied position ANY`() =
        runTest {
            // Arrange (A1: TO requested but no network -> degrades to manual, FROM still applied)
            val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
            givenPortfolio(statuses = listOf(currencyStatus(fromCurrency)))
            val expected = bareSwap(walletId).copy(
                fromCryptoCurrency = fromCurrency,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.ANY,
            )

            // Act
            createHandler(
                this,
                mapOf(
                    FROM_TOKEN_ID_KEY to "btc",
                    FROM_NETWORK_ID_KEY to "bitcoin",
                    TO_TOKEN_ID_KEY to "eth",
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    @Test
    fun `GIVEN from concrete only and provider WHEN handle THEN provider dropped and no pair lookup`() = runTest {
        // Arrange (A3: no concrete pair -> provider_id is best-effort dropped, not gated to Main)
        val fromCurrency = currency(tokenId = "btc", networkId = "bitcoin")
        givenPortfolio(statuses = listOf(currencyStatus(fromCurrency)))
        val expected = bareSwap(walletId).copy(
            fromCryptoCurrency = fromCurrency,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.ANY,
        )

        // Act
        createHandler(
            this,
            mapOf(
                FROM_TOKEN_ID_KEY to "btc",
                FROM_NETWORK_ID_KEY to "bitcoin",
                PROVIDER_ID_KEY to "changelly",
            ),
        )
        advanceUntilIdle()

        // Assert
        verify { router.push(route = expected, onComplete = any()) }
        coVerify(exactly = 0) { swapInteractor.getPair(any<SwapCurrencyStatus>(), any<SwapCurrencyStatus>(), any()) }
    }

    // endregion

    // region Task 7 (spec case 5): AI-MCP account scoping across multiple crypto-portfolio accounts

    @Test
    fun `GIVEN from_user_account_id matches account B WHEN handle THEN from resolves in account B not richer account A`() =
        runTest {
            // Arrange: btc exists in both accounts, account A is more funded, but the explicit
            // from_user_account_id pins resolution to account B.
            val fromInA = currency(tokenId = "btc", networkId = "bitcoin", decimals = 8)
            val fromInB = currency(tokenId = "btc", networkId = "bitcoin", decimals = 9)
            givenMultiAccountPortfolio(
                accountAStatuses = listOf(currencyStatus(fromInA, fiatAmount = BigDecimal("100"))),
                accountBStatuses = listOf(currencyStatus(fromInB, fiatAmount = BigDecimal.ONE)),
            )
            val expected = bareSwap(walletId).copy(
                fromCryptoCurrency = fromInB,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.ANY,
            )

            // Act
            createHandler(
                this,
                mapOf(
                    FROM_TOKEN_ID_KEY to "btc",
                    FROM_NETWORK_ID_KEY to "bitcoin",
                    FROM_USER_ACCOUNT_ID_KEY to secondAccountId.value,
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    @Test
    fun `GIVEN from_user_account_id does not exist WHEN handle THEN degrades to most funded instance in wallet`() =
        runTest {
            // Arrange: the pinned account id isn't any real account -> search widens to the whole
            // wallet and picks the most-funded instance (account A here), not Main.
            val fromInA = currency(tokenId = "btc", networkId = "bitcoin", decimals = 8)
            val fromInB = currency(tokenId = "btc", networkId = "bitcoin", decimals = 9)
            givenMultiAccountPortfolio(
                accountAStatuses = listOf(currencyStatus(fromInA, fiatAmount = BigDecimal("100"))),
                accountBStatuses = listOf(currencyStatus(fromInB, fiatAmount = BigDecimal.ONE)),
            )
            val expected = bareSwap(walletId).copy(
                fromCryptoCurrency = fromInA,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.ANY,
            )

            // Act
            createHandler(
                this,
                mapOf(
                    FROM_TOKEN_ID_KEY to "btc",
                    FROM_NETWORK_ID_KEY to "bitcoin",
                    FROM_USER_ACCOUNT_ID_KEY to "does-not-exist",
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    @Test
    fun `GIVEN to exists in both accounts WHEN handle THEN to resolves in from's account not the richer one`() =
        runTest {
            // Arrange: FROM only exists in account B; TO (eth) exists in both accounts, with account A
            // more funded. TO must still resolve to account B's instance (FROM's account takes priority
            // over the fiat-amount heuristic).
            val fromInB = currency(tokenId = "btc", networkId = "bitcoin")
            val toInA = currency(tokenId = "eth", networkId = "ethereum", decimals = 8)
            val toInB = currency(tokenId = "eth", networkId = "ethereum", decimals = 9)
            givenMultiAccountPortfolio(
                accountAStatuses = listOf(currencyStatus(toInA, fiatAmount = BigDecimal("100"))),
                accountBStatuses = listOf(
                    currencyStatus(fromInB, fiatAmount = BigDecimal.ONE),
                    currencyStatus(toInB, fiatAmount = BigDecimal.ONE),
                ),
            )
            val expected = bareSwap(walletId).copy(
                fromCryptoCurrency = fromInB,
                toCryptoCurrency = toInB,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
            )

            // Act
            createHandler(
                this,
                mapOf(
                    FROM_TOKEN_ID_KEY to "btc",
                    FROM_NETWORK_ID_KEY to "bitcoin",
                    TO_TOKEN_ID_KEY to "eth",
                    TO_NETWORK_ID_KEY to "ethereum",
                ),
            )
            advanceUntilIdle()

            // Assert
            verify { router.push(route = expected, onComplete = any()) }
        }

    // endregion
}