package com.tangem.feature.tokendetails.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeeplinkConst.DERIVATION_PATH_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TRANSACTION_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.error.SelectWalletError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionTrigger
import com.tangem.utils.logging.TangemLogger
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTokenDetailsDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk()
    private val selectWalletUseCase: SelectWalletUseCase = mockk()
    private val getSelectedWalletSync: GetSelectedWalletSyncUseCase = mockk()
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk()
    private val tokenDetailsDeepLinkActionTrigger: TokenDetailsDeepLinkActionTrigger = mockk()
    private val walletDeepLinkActionTrigger: WalletDeepLinkActionTrigger = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val walletBalanceFetcher: WalletBalanceFetcher = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val singleAccountListFetcher: SingleAccountListFetcher = mockk()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(TangemLogger)
        every { analyticsEventHandler.send(any()) } just Runs
        every { appRouter.push(any(), any()) } just Runs
        every { appRouter.popTo(route = any(), onComplete = any()) } just Runs
        coEvery { singleAccountListFetcher.invoke(any()) } returns Either.Right(Unit)
        val userWallet: UserWallet = mockk()
        every { userWallet.walletId } returns mockk()
        every { getSelectedWalletSync() } returns Either.Right(
            value = userWallet
        )
    }

    @Test
    fun `GIVEN error instead of user wallet WHEN handle deeplink THEN get error`() = runTest {
        val queryParams = mapOf(WALLET_ID_KEY to "011")
        every {
            getUserWalletUseCase.invoke(
                userWalletId = UserWalletId(
                    "011"
                )
            )
        } returns Either.Left(
            value = GetUserWalletError.UserWalletNotFound
        )
        every { TangemLogger.e("Error on getting user wallet") } just Runs
        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify { TangemLogger.e("Error on getting user wallet") }
    }

    @Test
    fun `GIVEN locked user wallet WHEN handle deeplink THEN get error`() = runTest {
        val queryParams = mapOf(WALLET_ID_KEY to "011")
        every {
            getUserWalletUseCase.invoke(
                userWalletId = UserWalletId(
                    "011"
                )
            )
        } returns Either.Right(
            value = mockk { every { isLocked } returns true }
        )
        every { TangemLogger.e("Error on getting user wallet") } just Runs
        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify { TangemLogger.e("Error on getting user wallet") }
    }

    @Test
    fun `GIVEN error instead select wallet WHEN handle deeplink THEN get error`() = runTest {
        val queryParams = mapOf(WALLET_ID_KEY to "011")
        val userWalletId = UserWalletId("011")
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk { every { isLocked } returns false }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Left(
            value = SelectWalletError.UnableToSelectUserWallet
        )
        every { TangemLogger.e("Error on selecting user wallet") } just Runs
        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify { TangemLogger.e("Error on selecting user wallet") }
    }

    @Test
    fun `GIVEN no crypto by wallet WHEN handle deeplink THEN get error`() = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
        )
        val userWalletId = UserWalletId("011")
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null
        val expectedErrorText = """
                        Could not get crypto currency for
                        |- $NETWORK_ID_KEY: 123
                        |- $TOKEN_ID_KEY: 321
                    """.trimIndent()
        every { TangemLogger.e(messageString = expectedErrorText) } just Runs
        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify { TangemLogger.e(messageString = expectedErrorText) }
    }

    @Test
    fun `GIVEN multicurrency wallet WHEN handle deeplink THEN push new route`() = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777"
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        val expectedRoute = AppRoute.CurrencyDetails(
            userWalletId = userWalletId,
            currency = expectedCryptoCurrency,
        )
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns true
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )

        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify {
            appRouter.push(
                route = expectedRoute,
                onComplete = any(),
            )
        }
    }

    @Test
    fun `GIVEN single currency wallet WHEN handle deeplink THEN push new route`() = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777"
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )
        every { walletDeepLinkActionTrigger.selectWallet(userWalletId) } just Runs

        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        verify {
            walletDeepLinkActionTrigger.selectWallet(userWalletId)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["swap_status_update", "onramp_status_update"])
    fun `GIVEN type WHEN handle deeplink THEN token details deeplink triggered`(type: String) = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777",
            TRANSACTION_ID_KEY to "000",
            TYPE_KEY to type,
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )
        every { walletDeepLinkActionTrigger.selectWallet(userWalletId) } just Runs
        coEvery { tokenDetailsDeepLinkActionTrigger.trigger("000") } just Runs

        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        coVerify {
            tokenDetailsDeepLinkActionTrigger.trigger("000")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["income_transaction", "promo", "unknown"])
    fun `GIVEN type WHEN handle deeplink THEN token details deeplink not triggered`(type: String) = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777",
            TRANSACTION_ID_KEY to "000",
            TYPE_KEY to type,
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )
        every { walletDeepLinkActionTrigger.selectWallet(userWalletId) } just Runs
        coEvery { tokenDetailsDeepLinkActionTrigger.trigger("000") } just Runs

        createHandler(scope = this, queryParams)
        advanceUntilIdle()
        coVerify(exactly = 0) {
            tokenDetailsDeepLinkActionTrigger.trigger("000")
        }
    }

    @Test
    fun `GIVEN multicurrency wallet AND isFromOnNewIntent WHEN handle deeplink THEN fetch currency`() = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777"
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns true
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )
        every {
            cryptoCurrencyBalanceFetcher.invoke(userWalletId = userWalletId, currency = expectedCryptoCurrency)
        } just Runs

        createHandler(scope = this, queryParams, isFromOnNewIntent = true)
        advanceUntilIdle()
        verify { cryptoCurrencyBalanceFetcher.invoke(userWalletId = userWalletId, currency = expectedCryptoCurrency) }
    }

    @Test
    fun `GIVEN single currency wallet AND isFromOnNewIntent WHEN handle deeplink THEN fetch currency`() = runTest {
        val queryParams = mapOf(
            WALLET_ID_KEY to "011",
            NETWORK_ID_KEY to "123",
            TOKEN_ID_KEY to "321",
            DERIVATION_PATH_KEY to "777"
        )
        val userWalletId = UserWalletId("011")
        val expectedCryptoCurrency = mockk<CryptoCurrency> {
            every { network } returns mockk {
                every { rawId } returns "123"
                every { derivationPath } returns Network.DerivationPath.Card(value = "777")
            }
            every { id } returns CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                    rawId = "321",
                    derivationPath = "777"
                ),
                suffix = CryptoCurrency.ID.Suffix.RawID("321")
            )
        }
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            }
        )
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { walletId } returns userWalletId
            }
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(expectedCryptoCurrency),
        )
        coEvery {
            walletBalanceFetcher.invoke(
                WalletBalanceFetcher.Params(
                    userWalletId = userWalletId,
                )
            )
        } returns mockk()
        every { walletDeepLinkActionTrigger.selectWallet(userWalletId) } just Runs

        createHandler(scope = this, queryParams, isFromOnNewIntent = true)
        advanceUntilIdle()
        coEvery {
            walletBalanceFetcher.invoke(
                WalletBalanceFetcher.Params(
                    userWalletId = userWalletId,
                )
            )
        }
    }

    @Test
    fun `GIVEN multicurrency wallet AND isFromOnNewIntent WHEN handle deeplink THEN refresh wallet accounts`() =
        runTest {
            val userWalletId = UserWalletId("011")
            val cryptoCurrency = mockCryptoCurrency()
            mockMultiCurrencyWallet(userWalletId)
            mockSelectWallet(userWalletId)
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(cryptoCurrency),
            )
            every {
                cryptoCurrencyBalanceFetcher.invoke(userWalletId = userWalletId, currency = cryptoCurrency)
            } just Runs

            createHandler(scope = this, defaultQueryParams(), isFromOnNewIntent = true)
            advanceUntilIdle()

            coVerify { singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId)) }
        }

    @Test
    fun `GIVEN multicurrency wallet AND NOT isFromOnNewIntent WHEN handle deeplink THEN do not refresh accounts`() =
        runTest {
            val userWalletId = UserWalletId("011")
            val cryptoCurrency = mockCryptoCurrency()
            mockMultiCurrencyWallet(userWalletId)
            mockSelectWallet(userWalletId)
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(cryptoCurrency),
            )

            createHandler(scope = this, defaultQueryParams(), isFromOnNewIntent = false)
            advanceUntilIdle()

            coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
        }

    @Test
    fun `GIVEN single currency wallet AND isFromOnNewIntent WHEN handle deeplink THEN do not refresh accounts`() =
        runTest {
            val userWalletId = UserWalletId("011")
            val cryptoCurrency = mockCryptoCurrency()
            mockSingleCurrencyWallet(userWalletId)
            mockSelectWallet(userWalletId)
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
                userWalletId = userWalletId,
                cryptoCurrencies = listOf(cryptoCurrency),
            )
            every { walletDeepLinkActionTrigger.selectWallet(userWalletId) } just Runs
            coEvery {
                walletBalanceFetcher.invoke(WalletBalanceFetcher.Params(userWalletId = userWalletId))
            } returns mockk()

            createHandler(scope = this, defaultQueryParams(), isFromOnNewIntent = true)
            advanceUntilIdle()

            coVerify(exactly = 0) { singleAccountListFetcher.invoke(any()) }
        }

    @Test
    fun `GIVEN crypto not found WHEN handle deeplink THEN redirect to main`() = runTest {
        val userWalletId = UserWalletId("011")
        mockMultiCurrencyWallet(userWalletId)
        mockSelectWallet(userWalletId)
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null

        createHandler(scope = this, defaultQueryParams(), isFromOnNewIntent = true)
        advanceUntilIdle()

        verify { appRouter.popTo(route = AppRoute.Wallet, onComplete = any()) }
    }

    @Test
    fun `GIVEN refresh failed AND token in cache WHEN handle deeplink THEN push new route`() = runTest {
        val userWalletId = UserWalletId("011")
        val cryptoCurrency = mockCryptoCurrency()
        mockMultiCurrencyWallet(userWalletId)
        mockSelectWallet(userWalletId)
        coEvery {
            singleAccountListFetcher.invoke(SingleAccountListFetcher.Params(userWalletId))
        } returns Either.Left(IllegalStateException("service unavailable"))
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns AccountList.empty(
            userWalletId = userWalletId,
            cryptoCurrencies = listOf(cryptoCurrency),
        )
        every {
            cryptoCurrencyBalanceFetcher.invoke(userWalletId = userWalletId, currency = cryptoCurrency)
        } just Runs
        val expectedRoute = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = cryptoCurrency)

        createHandler(scope = this, defaultQueryParams(), isFromOnNewIntent = true)
        advanceUntilIdle()

        verify {
            appRouter.push(route = expectedRoute, onComplete = any())
        }
    }

    private fun defaultQueryParams() = mapOf(
        WALLET_ID_KEY to "011",
        NETWORK_ID_KEY to "123",
        TOKEN_ID_KEY to "321",
        DERIVATION_PATH_KEY to "777",
    )

    private fun mockCryptoCurrency() = mockk<CryptoCurrency> {
        every { network } returns mockk {
            every { rawId } returns "123"
            every { derivationPath } returns Network.DerivationPath.Card(value = "777")
        }
        every { id } returns CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(rawId = "321", derivationPath = "777"),
            suffix = CryptoCurrency.ID.Suffix.RawID("321"),
        )
    }

    private fun mockMultiCurrencyWallet(userWalletId: UserWalletId) {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns true
                every { walletId } returns userWalletId
                every { isLocked } returns false
            },
        )
    }

    private fun mockSingleCurrencyWallet(userWalletId: UserWalletId) {
        every { getUserWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk {
                every { isMultiCurrency } returns false
                every { walletId } returns userWalletId
                every { isLocked } returns false
            },
        )
    }

    private fun mockSelectWallet(userWalletId: UserWalletId) {
        coEvery { selectWalletUseCase.invoke(userWalletId) } returns Either.Right(
            value = mockk { every { walletId } returns userWalletId },
        )
    }

    private fun createHandler(
        scope: CoroutineScope,
        queryParams: Map<String, String>,
        isFromOnNewIntent: Boolean = false,
    ) {
        DefaultTokenDetailsDeepLinkHandler(
            scope = scope,
            queryParams = queryParams,
            isFromOnNewIntent = isFromOnNewIntent,
            appRouter = appRouter,
            selectWalletUseCase = selectWalletUseCase,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
            tokenDetailsDeepLinkActionTrigger = tokenDetailsDeepLinkActionTrigger,
            walletDeepLinkActionTrigger = walletDeepLinkActionTrigger,
            analyticsEventHandler = analyticsEventHandler,
            getUserWalletUseCase = getUserWalletUseCase,
            walletBalanceFetcher = walletBalanceFetcher,
            singleAccountListSupplier = singleAccountListSupplier,
            singleAccountListFetcher = singleAccountListFetcher,
            getSelectedWalletSyncUseCase = getSelectedWalletSync,
        )
    }
}