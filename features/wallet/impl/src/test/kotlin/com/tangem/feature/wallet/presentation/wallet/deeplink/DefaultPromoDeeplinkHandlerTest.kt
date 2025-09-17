@file:Suppress("FunctionSignature")

package com.tangem.feature.wallet.presentation.wallet.deeplink

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.wallets.PromoCodeActivationResult
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.usecase.ActivateBitcoinPromocodeUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.DefaultPromoDeeplinkHandler
import com.tangem.feature.wallet.deeplink.analytics.PromoActivationAnalytics
import com.tangem.feature.wallet.impl.R
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPromoDeeplinkHandlerTest {

    @MockK(relaxed = true)
    private lateinit var uiMessageSender: UiMessageSender

    @MockK
    private lateinit var multiNetworkStatusSupplier: MultiNetworkStatusSupplier

    @MockK
    private lateinit var multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier

    @MockK
    private lateinit var activateBitcoinPromocodeUseCase: ActivateBitcoinPromocodeUseCase

    @MockK
    private lateinit var getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase

    @MockK
    private lateinit var analyticsEventHandler: AnalyticsEventHandler

    private lateinit var messages: MutableList<UiMessage>

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { analyticsEventHandler.send(any()) } returns Unit
        messages = mutableListOf()
        every { uiMessageSender.send(capture(messages)) } just runs

        Timber.uprootAll()
    }

    @Test
    fun `GIVEN empty and non-BTC then BTC WHEN supplier emits THEN activated dialog is shown`() = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

        val ethStatus = buildNetworkStatus(rawNetworkId = "ethereum", address = "0x123")
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flow {
            emit(emptySet())
            emit(setOf(ethStatus))
            emit(setOf(btcStatus))
        }
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id)
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qxyz", promoCode) } returns Either.Right("ok")
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success))

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.Activated),
            )
        }
    }

    @Test
    fun `GIVEN empty promo code WHEN init THEN invalid promo code dialog is shown`() = runTest {
        val queryParams = emptyMap<String, String>()

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code))

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.InvalidPromoCode),
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
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error))

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.Failed),
            )
        }
    }

    @Test
    fun `GIVEN no bitcoin address WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcUnreachable = buildUnreachableNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id)
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcUnreachable))
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id)
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
            )
        }
    }

    @Test
    fun `GIVEN activation success WHEN activatePromoCode THEN activated dialog is shown`() = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
        val btcCoin = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id)
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoin)
        coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qxyz", promoCode) } returns Either.Right("ok")
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            analyticsEventsHandler = analyticsEventHandler,
            dispatchers = dispatcherProvider,
        )

        advanceUntilIdle()

        val sent = messages.last { it is DialogMessage } as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success))

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.Activated),
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

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.InvalidPromoCode),
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

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.Failed),
            )
        }
    }

    @Test
    fun `GIVEN activation no address WHEN activatePromoCode THEN no bitcoin address dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.NoBitcoinAddress,
            expectedTitle = resourceReference(R.string.bitcoin_promo_no_address_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_no_address),
        )

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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

        verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.PromoCodeAlreadyUsed),
            )
        }
    }

    private fun runActivationErrorCase(
        error: ActivatePromoCodeError,
        expectedTitle: TextReference,
        expectedMessage: TextReference,
    ) = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
        coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatus))
        val btcCurrency = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id)
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCurrency)
        coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qxyz", promoCode) } returns Either.Left(error)
        val dispatcherProvider = testDispatcherProvider(testScheduler)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            multiNetworkStatusSupplier = multiNetworkStatusSupplier,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
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
            val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
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
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))
        }

    @Test
    fun `GIVEN multiple statuses emissions and currencies without BTC WHEN findBitcoinAddress THEN no bitcoin address dialog is shown`() =
        runTest {
            val promoCode = "PROMO123"
            val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
            val userWallet = mockUserWallet("ABCDEF")
            every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)

            val ethStatus = buildNetworkStatus(rawNetworkId = "ethereum", address = "0x123")
            val btcStatus = buildNetworkStatus(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
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
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard, btcStatusCustom))

            val btcCoinCustom = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCustom)
            val btcCoinCard = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(
                btcCoinCustom,
                btcCoinCard,
            )
            coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qcustom", promoCode) } returns Either.Right("ok")

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success))

            coVerify(exactly = 1) { activateBitcoinPromocodeUseCase.invoke("bc1qcustom", promoCode) }
            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke("bc1qcard", promoCode) }
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard, btcStatusCustom))

            val btcCoinCard = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCard)

            coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qcard", promoCode) } returns Either.Right("ok")

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success))

            coVerify(exactly = 1) { activateBitcoinPromocodeUseCase.invoke("bc1qcard", promoCode) }
            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke("bc1qcustom", promoCode) }

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.Activated),
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            val btcStatusCustom = buildNetworkStatus(
                rawNetworkId = Blockchain.Bitcoin.id,
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
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke(any(), any()) }

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard))

            val btcCoinCustom = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCustom)
            val btcCoinCard = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCard)
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
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke(any(), any()) }

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcustom",
                derivationPath = dpCustom,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCustom))

            val btcCoinCard = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCard)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCard)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke(any(), any()) }

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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
                rawNetworkId = Blockchain.Bitcoin.id,
                address = "bc1qcard",
                derivationPath = dpCard,
            )
            coEvery { multiNetworkStatusSupplier.invoke(any()) } returns flowOf(setOf(btcStatusCard))

            val btcCoinCustom = buildCryptoCurrency(rawNetworkId = Blockchain.Bitcoin.id, derivationPath = dpCustom)
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any()) } returns setOf(btcCoinCustom)

            val dispatcherProvider = testDispatcherProvider(testScheduler)

            DefaultPromoDeeplinkHandler(
                scope = this,
                queryParams = queryParams,
                uiMessageSender = uiMessageSender,
                multiNetworkStatusSupplier = multiNetworkStatusSupplier,
                multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
                activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
                getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
                analyticsEventsHandler = analyticsEventHandler,
                dispatchers = dispatcherProvider,
            )

            advanceUntilIdle()

            val sent = messages.last { it is DialogMessage } as DialogMessage
            Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
            Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))

            coVerify(exactly = 0) { activateBitcoinPromocodeUseCase.invoke(any(), any()) }

            verify(exactly = 1) { analyticsEventHandler.send(PromoActivationAnalytics.PromoDeepLinkActivationStart) }
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    PromoActivationAnalytics.PromoActivation(PromoCodeActivationResult.NoBitcoinAddress),
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
        val networkId = Network.ID(Network.RawID(rawNetworkId), derivationPath)
        val network = Network(
            id = networkId,
            backendId = rawNetworkId,
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
        val networkId = Network.ID(Network.RawID(rawNetworkId), Network.DerivationPath.None)
        val network = Network(
            id = networkId,
            backendId = rawNetworkId,
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
    ): CryptoCurrency.Coin {
        val networkId = Network.ID(Network.RawID(rawNetworkId), derivationPath)
        val network = Network(
            id = networkId,
            backendId = rawNetworkId,
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
            symbol = rawNetworkId.take(3).uppercase(),
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )
    }

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
}