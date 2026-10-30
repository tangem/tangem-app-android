@file:Suppress("FunctionSignature")

package com.tangem.feature.wallet.presentation.wallet.deeplink

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.domain.wallets.models.PromoCodeActivation
import com.tangem.domain.wallets.models.PromoPayout
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.ActivatePromoCodeUseCase
import com.tangem.domain.wallets.usecase.BindRefcodeWithWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.DefaultPromoDeeplinkHandler
import com.tangem.feature.wallet.deeplink.analytics.PromoActivationError
import com.tangem.feature.wallet.deeplink.analytics.PromoBannerAnalytics
import com.tangem.feature.wallet.impl.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPromoDeeplinkHandlerTest {

    @MockK(relaxed = true)
    private lateinit var uiMessageSender: UiMessageSender

    @MockK
    private lateinit var multiNetworkStatusSupplier: MultiNetworkStatusSupplier

    @MockK
    private lateinit var multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier

    @MockK
    private lateinit var activatePromoCodeUseCase: ActivatePromoCodeUseCase

    @MockK
    private lateinit var getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase

    @MockK
    private lateinit var analyticsEventHandler: AnalyticsEventHandler

    @MockK
    private lateinit var bindRefcodeWithWalletUseCase: BindRefcodeWithWalletUseCase

    @MockK
    private lateinit var appsFlyerRepository: AppsFlyerRepository

    private lateinit var messages: MutableList<UiMessage>

    private val stellarPayout = PromoPayout(networkId = "stellar", currencySymbol = "XLM")
    private val stellarQueryParams = mapOf(
        DeeplinkConst.PROMO_CODE_KEY to "PROMO123",
        "payment_network" to "stellar",
        "payment_currency" to "XLM",
    )
    private val activation = PromoCodeActivation(programId = "program-id")
    private val bitcoinSuccessMessage = resourceReference(
        id = R.string.promo_code_activation_success,
        formatArgs = wrappedList("Bitcoin"),
    )
    private val bitcoinNoAddressTitle = resourceReference(
        id = R.string.promo_code_no_address_title,
        formatArgs = wrappedList("Bitcoin"),
    )
    private val bitcoinNoAddressMessage = resourceReference(
        id = R.string.promo_code_no_address,
        formatArgs = wrappedList("Bitcoin"),
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { analyticsEventHandler.send(any()) } returns Unit
        messages = mutableListOf()
        every { uiMessageSender.send(capture(messages)) } just runs
    }

    @Test
    fun `GIVEN empty and non-BTC then BTC WHEN supplier emits THEN activated dialog is shown`() = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

        val ethStatus = buildNetworkStatus(rawNetworkId = "ethereum", address = "0x123")
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flow {
            emit(emptySet())
            emit(setOf(ethStatus))
            emit(setOf(btcStatus))
        }
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        coEvery { activatePromoCodeUseCase.invoke(any(), "bc1qxyz", promoCode, null) } returns Either.Right(activation)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
        Truth.assertThat(sent.message).isEqualTo(bitcoinSuccessMessage)

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                ofType<PromoBannerAnalytics.ActivationSuccess>(),
            )
        }
    }

    @Test
    fun `GIVEN empty promo code WHEN init THEN invalid promo code dialog is shown`() = runTest {
        val queryParams = emptyMap<String, String>()
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code))

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.InvalidCode),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("incompletePayoutLinks")
    fun `GIVEN link with only one payout param WHEN init THEN invalid code dialog is shown without activation`(
        payoutParams: Map<String, String>,
    ) = runTest {
        // Arrange
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123") + payoutParams
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))

        // Act
        createHandler(queryParams)
        advanceUntilIdle()

        // Assert
        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code))
        coVerify(exactly = 0) { multiNetworkStatusSupplier.invoke(any()) }
        coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }
        verify(exactly = 0) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.ActivationAttempt>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                match<PromoBannerAnalytics.ActivationFailed> {
                    it.params["Error Code"] == PromoActivationError.InvalidCode.analyticsCode &&
                        it.params["Network"] != Blockchain.Bitcoin.toNetworkId()
                },
            )
        }
    }

    @Test
    fun `GIVEN wallet fetch error WHEN getSelectedWalletUseCase THEN failed dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Left(GetUserWalletError.UserWalletNotFound)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error))

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.InternalError),
            )
        }
    }

    @Test
    fun `GIVEN no bitcoin address WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcUnreachable = buildUnreachableNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcUnreachable))
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
        Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.AddressMissing),
            )
        }
    }

    @Test
    fun `GIVEN activation success WHEN activatePromoCode THEN activated dialog is shown`() = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        coEvery { activatePromoCodeUseCase.invoke(any(), "bc1qxyz", promoCode, null) } returns Either.Right(activation)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
        Truth.assertThat(sent.message).isEqualTo(bitcoinSuccessMessage)

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                ofType<PromoBannerAnalytics.ActivationSuccess>(),
            )
        }
    }

    @Test
    fun `GIVEN activation invalid code WHEN activatePromoCode THEN invalid promo code dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.InvalidPromoCode,
            expectedTitle = resourceReference(R.string.bitcoin_promo_invalid_code_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_invalid_code),
        )

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.InvalidCode),
            )
        }
    }

    @Test
    fun `GIVEN activation failed WHEN activatePromoCode THEN failed dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.ActivationFailed,
            expectedTitle = resourceReference(R.string.bitcoin_promo_activation_error_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_activation_error),
        )

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.InternalError),
            )
        }
    }

    @Test
    fun `GIVEN activation already used WHEN activatePromoCode THEN already activated dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.PromocodeAlreadyUsed,
            expectedTitle = resourceReference(R.string.bitcoin_promo_already_activated_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_already_activated),
        )

        verify(
            exactly = 1,
        ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.AlreadyUsed),
            )
        }
    }

    @Test
    fun `GIVEN stellar link with XLM WHEN activation succeeds THEN payout is sent and success is shown`() =
        runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(
                setOf(buildNetworkStatus(rawNetworkId = "stellar", address = "GSTELLAR")),
            )
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
                buildCryptoCurrency(rawNetworkId = "stellar", symbol = "XLM"),
            )
            coEvery {
                activatePromoCodeUseCase.invoke(any(), "GSTELLAR", "PROMO123", stellarPayout)
            } returns Either.Right(PromoCodeActivation(programId = "program-id"))

            // Act
            createHandler(queryParams = stellarQueryParams)
            advanceUntilIdle()

            // Assert
            val sent = messages.last { it is DialogMessage } as DialogMessage
            val expectedMessage = resourceReference(
                id = R.string.promo_code_activation_success,
                formatArgs = wrappedList("Stellar"),
            )
            Truth.assertThat(sent.message).isEqualTo(expectedMessage)
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match<PromoBannerAnalytics.ActivationSuccess> {
                        it.params["Campaign Id"] == "program-id" &&
                            it.params["Network"] == "stellar" &&
                            it.params["Token"] == "XLM"
                    },
                )
            }
        }

    @Test
    fun `GIVEN stellar link and only Stellar token with other symbol WHEN init THEN address missing dialog is shown`() =
        runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(
                setOf(buildNetworkStatus(rawNetworkId = "stellar", address = "GSTELLAR")),
            )
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
                buildCryptoCurrency(rawNetworkId = "stellar", symbol = "USDC"),
            )

            // Act
            createHandler(queryParams = stellarQueryParams)
            advanceUntilIdle()

            // Assert
            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(
                resourceReference(id = R.string.promo_code_no_address_title, formatArgs = wrappedList("Stellar")),
            )
            Truth.assertThat(sent.message).isEqualTo(
                resourceReference(id = R.string.promo_code_no_address, formatArgs = wrappedList("Stellar")),
            )
            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match<PromoBannerAnalytics.ActivationFailed> {
                        it.params["Error Code"] == PromoActivationError.AddressMissing.analyticsCode &&
                            it.params["Network"] == "stellar" &&
                            it.params["Token"] == "XLM"
                    },
                )
            }
        }

    @Test
    fun `GIVEN USDC payout link and only XLM added WHEN init THEN address missing dialog names USDC`() = runTest {
        // Arrange
        val queryParams = stellarQueryParams + ("payment_currency" to "USDC")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(
            setOf(buildNetworkStatus(rawNetworkId = "stellar", address = "GSTELLAR")),
        )
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
            buildCryptoCurrency(rawNetworkId = "stellar", symbol = "XLM"),
        )

        // Act
        createHandler(queryParams = queryParams)
        advanceUntilIdle()

        // Assert
        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(
            resourceReference(id = R.string.promo_code_no_address_title, formatArgs = wrappedList("USDC")),
        )
        Truth.assertThat(sent.message).isEqualTo(
            resourceReference(id = R.string.promo_code_no_address, formatArgs = wrappedList("USDC")),
        )
        coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }
        verify(exactly = 1) { analyticsEventHandler.send(failedWith(PromoActivationError.AddressMissing)) }
    }

    @Test
    fun `GIVEN stellar link WHEN campaign expired THEN error dialog is shown and failure tracked`() = runTest {
        // Arrange
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(
            setOf(buildNetworkStatus(rawNetworkId = "stellar", address = "GSTELLAR")),
        )
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
            buildCryptoCurrency(rawNetworkId = "stellar", symbol = "XLM"),
        )
        coEvery {
            activatePromoCodeUseCase.invoke(any(), "GSTELLAR", "PROMO123", stellarPayout)
        } returns Either.Left(ActivatePromoCodeError.CampaignExpired)

        // Act
        createHandler(queryParams = stellarQueryParams)
        advanceUntilIdle()

        // Assert
        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error_title))
        verify(exactly = 1) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.ActivationAttempt>()) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                failedWith(PromoActivationError.CampaignExpired),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReferralBinding {

        private val promoCode = "PROMO123"
        private val refcode = "REFCODE"

        @BeforeEach
        fun resetMocks() {
            clearMocks(
                appsFlyerRepository,
                bindRefcodeWithWalletUseCase,
                activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase,
                multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier,
            )
        }

        @Test
        fun `GIVEN ref without promo code WHEN init THEN refcode is stored and bound`() = runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            arrangeBinding()
            val expected = AppsFlyerConversionData(refcode = refcode, campaign = null)

            // Act
            createHandler(queryParams = mapOf(DeeplinkConst.REF_KEY to refcode))
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                appsFlyerRepository.storeIfAbsent(expected)
                bindRefcodeWithWalletUseCase.invoke(expected)
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN invalid ref WHEN init THEN refcode is neither stored nor bound`(invalidRef: String) = runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))

            // Act
            createHandler(queryParams = mapOf(DeeplinkConst.REF_KEY to invalidRef))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { appsFlyerRepository.storeIfAbsent(any()) }
            coVerify(exactly = 0) { bindRefcodeWithWalletUseCase.invoke(any()) }
        }

        @Test
        fun `GIVEN valid ref and campaign with promo code WHEN activation succeeds THEN refcode is stored and bound`() =
            runTest {
                // Arrange
                arrangeBitcoinActivation(result = Either.Right(activation))
                arrangeBinding()
                val expected = AppsFlyerConversionData(refcode = refcode, campaign = "spring")

                // Act
                createHandler(
                    queryParams = mapOf(
                        DeeplinkConst.PROMO_CODE_KEY to promoCode,
                        DeeplinkConst.REF_KEY to refcode,
                        DeeplinkConst.CAMPAIGN_KEY to "spring",
                    ),
                )
                advanceUntilIdle()

                // Assert
                coVerifyOrder {
                    appsFlyerRepository.storeIfAbsent(expected)
                    bindRefcodeWithWalletUseCase.invoke(expected)
                }
            }

        @Test
        fun `GIVEN valid ref and null literal campaign WHEN init THEN refcode is bound without campaign`() = runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            arrangeBinding()
            val expected = AppsFlyerConversionData(refcode = refcode, campaign = null)

            // Act
            createHandler(queryParams = mapOf(DeeplinkConst.REF_KEY to refcode, DeeplinkConst.CAMPAIGN_KEY to "null"))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeIfAbsent(expected) }
            coVerify(exactly = 1) { bindRefcodeWithWalletUseCase.invoke(expected) }
        }

        @Test
        fun `GIVEN ref with surrounding spaces WHEN init THEN trimmed refcode is bound`() = runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            arrangeBinding()
            val expected = AppsFlyerConversionData(refcode = refcode, campaign = null)

            // Act
            createHandler(queryParams = mapOf(DeeplinkConst.REF_KEY to " $refcode "))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeIfAbsent(expected) }
            coVerify(exactly = 1) { bindRefcodeWithWalletUseCase.invoke(expected) }
        }

        @Test
        fun `GIVEN valid ref WHEN promo activation fails THEN refcode is still stored and bound`() = runTest {
            // Arrange
            arrangeBitcoinActivation(result = Either.Left(ActivatePromoCodeError.InvalidPromoCode))
            arrangeBinding()
            val expected = AppsFlyerConversionData(refcode = refcode, campaign = null)

            // Act
            createHandler(
                queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode, DeeplinkConst.REF_KEY to refcode),
            )
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { appsFlyerRepository.storeIfAbsent(expected) }
            coVerify(exactly = 1) { bindRefcodeWithWalletUseCase.invoke(expected) }
        }

        private fun provideTestModels() = listOf("null", "NULL", " ", "")

        private fun arrangeBitcoinActivation(result: Either<ActivatePromoCodeError, PromoCodeActivation>) {
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(mockUserWallet("ABCDEF"))
            val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
            val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
            coEvery { activatePromoCodeUseCase.invoke(any(), "bc1qxyz", promoCode, null) } returns result
        }

        private fun arrangeBinding() {
            coEvery { appsFlyerRepository.storeIfAbsent(any()) } just runs
            coEvery { bindRefcodeWithWalletUseCase.invoke(any()) } returns Either.Right(Unit)
        }
    }

    private fun TestScope.createHandler(queryParams: Map<String, String>) = DefaultPromoDeeplinkHandler(
        scope = this,
        queryParams = queryParams,
        uiMessageSender = uiMessageSender,
        multiNetworkStatusSupplier = multiNetworkStatusSupplier,
        multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        activatePromoCodeUseCase = activatePromoCodeUseCase,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
        appsFlyerRepository = appsFlyerRepository,
        analyticsEventsHandler = analyticsEventHandler,
        dispatchers = testDispatcherProvider(testScheduler),
    )

    private fun runActivationErrorCase(
        error: ActivatePromoCodeError,
        expectedTitle: TextReference,
        expectedMessage: TextReference,
    ) = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
        val btcCurrency = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId())
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCurrency)
        coEvery { activatePromoCodeUseCase.invoke(any(), "bc1qxyz", promoCode, null) } returns Either.Left(error)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activatePromoCodeUseCase = activatePromoCodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
            appsFlyerRepository = appsFlyerRepository,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(expectedTitle)
        Truth.assertThat(sent.message).isEqualTo(expectedMessage)
    }

    @Test
    fun `GIVEN BTC status but currencies without BTC WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
            val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
            val ethOnly = buildCryptoCurrency(rawNetworkId = "ethereum")
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(ethOnly)
            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)
        }

    @Test
    fun `GIVEN multiple statuses emissions and currencies without BTC WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val ethStatus = buildNetworkStatus(rawNetworkId = "ethereum", address = "0x123")
            val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), address = "bc1qxyz")
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flow {
                emit(emptySet())
                emit(setOf(ethStatus))
                emit(setOf(ethStatus, btcStatus))
            }

            val ethOnly = buildCryptoCurrency(rawNetworkId = "ethereum")
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(ethOnly)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    failedWith(PromoActivationError.AddressMissing),
                )
            }
        }

    @Test
    fun `GIVEN two BTC statuses with different derivation AND two BTC currencies WHEN activate THEN activated`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCard = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard, btcStatusCustom))

            val btcCoinCustom =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCustom)
            val btcCoinCard =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
                btcCoinCustom,
                btcCoinCard,
            )
            coEvery {
                activatePromoCodeUseCase.invoke(
                    any(),
                    "bc1qcustom",
                    promoCode,
                    null,
                )
            } returns Either.Right(activation)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
            Truth.assertThat(sent.message).isEqualTo(bitcoinSuccessMessage)

            coVerify(exactly = 1) { activatePromoCodeUseCase.invoke(any(), "bc1qcustom", promoCode, null) }
            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), "bc1qcard", promoCode, null) }
        }

    @Test
    fun `GIVEN two BTC statuses with different derivation AND one matching BTC currency WHEN activate THEN activated`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCard = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard, btcStatusCustom))

            val btcCoinCard =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCard)

            coEvery {
                activatePromoCodeUseCase.invoke(any(), "bc1qcard", promoCode, null)
            } returns Either.Right(activation)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
            Truth.assertThat(sent.message).isEqualTo(bitcoinSuccessMessage)

            coVerify(exactly = 1) { activatePromoCodeUseCase.invoke(any(), "bc1qcard", promoCode, null) }
            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), "bc1qcustom", promoCode, null) }

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    ofType<PromoBannerAnalytics.ActivationSuccess>(),
                )
            }
        }

    @Test
    fun `GIVEN two BTC statuses with different derivation AND no BTC currencies WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCard = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard, btcStatusCustom))

            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns emptySet()

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    failedWith(PromoActivationError.AddressMissing),
                )
            }
        }

    @Test
    fun `GIVEN two BTC currencies first derivation mismatched AND one matching status WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCard = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard))

            val btcCoinCustom =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCustom)
            val btcCoinCard =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
                btcCoinCustom,
                btcCoinCard,
            )

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    failedWith(PromoActivationError.AddressMissing),
                )
            }
        }

    @Test
    fun `GIVEN only BTC status with CUSTOM derivation AND only BTC currency with CARD derivation WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCustom))

            val btcCoinCard =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCard)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    failedWith(PromoActivationError.AddressMissing),
                )
            }
        }

    @Test
    fun `GIVEN only BTC status with CARD derivation AND only BTC currency with CUSTOM derivation WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val dpCard = Network.DerivationPath.Card("m/44'/0'/0'")
            val dpCustom = Network.DerivationPath.Custom("m/84'/0'/0'")

            val btcStatusCard = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.toNetworkId(),
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard))

            val btcCoinCustom =
                buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.toNetworkId(), derivationPath = dpCustom)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCustom)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activatePromoCodeUseCase = activatePromoCodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                bindRefcodeWithWalletUseCase = bindRefcodeWithWalletUseCase,
                appsFlyerRepository = appsFlyerRepository,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(bitcoinNoAddressTitle)
            Truth.assertThat(sent.message).isEqualTo(bitcoinNoAddressMessage)

            coVerify(exactly = 0) { activatePromoCodeUseCase.invoke(any(), any(), any(), any()) }

            verify(
                exactly = 1,
            ) { analyticsEventHandler.send(ofType<PromoBannerAnalytics.BannerOpen>()) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    failedWith(PromoActivationError.AddressMissing),
                )
            }
        }

    private fun mockUserWallet(id: String): UserWallet {
        val userWallet = mockk<UserWallet>(relaxed = true)
        every { userWallet.walletId } returns UserWalletId(id)
        return userWallet
    }

    private fun buildNetworkStatus(
        rawNetworkId: String,
        address: String,
        derivationPath: Network.DerivationPath = Network.DerivationPath.None,
    ): NetworkStatus {
        val networkId = Network.ID(value = rawNetworkId, derivationPath = derivationPath)
        val network = Network(
            id = networkId,
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        val value = NetworkStatus.Verified(
            address = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = address,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            amounts = emptyMap(),
            pendingTransactions = emptyMap(),
            source = StatusSource.ACTUAL,
            yieldSupplyStatuses = emptyMap(),
        )

        return NetworkStatus(network = network, value = value)
    }

    private fun buildUnreachableNetworkStatus(rawNetworkId: String): NetworkStatus {
        val networkId = Network.ID(value = rawNetworkId, derivationPath = Network.DerivationPath.None)
        val network = Network(
            id = networkId,
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        val value = NetworkStatus.Unreachable(address = null)

        return NetworkStatus(network = network, value = value)
    }

    private fun buildCryptoCurrency(
        rawNetworkId: String,
        derivationPath: Network.DerivationPath = Network.DerivationPath.None,
        symbol: String = rawNetworkId.take(3).uppercase(),
    ): CryptoCurrency.Coin {
        val networkId = Network.ID(value = rawNetworkId, derivationPath = derivationPath)
        val network = Network(
            id = networkId,
            name = rawNetworkId,
            currencySymbol = rawNetworkId.take(3).uppercase(),
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified("UNSPECIFIED"),
            hasFiatFeeRate = false,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )

        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
            ),
            network = network,
            name = rawNetworkId,
            symbol = symbol,
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    }

    private fun MockKMatcherScope.failedWith(error: PromoActivationError) =
        match<PromoBannerAnalytics.ActivationFailed> { it.params["Error Code"] == error.analyticsCode }

    private fun testDispatcherProvider(scheduler: TestCoroutineScheduler): CoroutineDispatcherProvider {
        val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler)
        return object : CoroutineDispatcherProvider {
            override val main: CoroutineDispatcher = dispatcher
            override val mainImmediate: CoroutineDispatcher = dispatcher
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val single: CoroutineDispatcher = dispatcher
        }
    }

    private companion object {
        @JvmStatic
        fun incompletePayoutLinks() = listOf(
            mapOf("payment_network" to "stellar"),
            mapOf("payment_currency" to "XLM"),
            mapOf("payment_network" to "stellar", "payment_currency" to " "),
            mapOf("payment_network" to "", "payment_currency" to "XLM"),
            mapOf("payment_network" to ""),
            mapOf("payment_network" to "", "payment_currency" to " "),
        )
    }
}