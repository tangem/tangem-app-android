package com.tangem.feature.wallet.presentation.wallet.deeplink

import arrow.core.Either
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetMultiCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.usecase.ActivateBitcoinPromocodeUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.deeplink.DefaultPromoDeeplinkHandler
import com.tangem.feature.wallet.impl.R
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.math.BigDecimal

class DefaultPromoDeeplinkHandlerTest {

    @MockK(relaxed = true)
    private lateinit var uiMessageSender: UiMessageSender

    @MockK
    private lateinit var getMultiCryptoCurrencyStatusUseCase: GetMultiCryptoCurrencyStatusUseCase

    @MockK
    private lateinit var activateBitcoinPromocodeUseCase: ActivateBitcoinPromocodeUseCase

    @MockK
    private lateinit var getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase

    private lateinit var messageSlot: CapturingSlot<UiMessage>
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        Dispatchers.setMain(testDispatcher)

        MockKAnnotations.init(this)
        messageSlot = slot()
        every { uiMessageSender.send(capture(messageSlot)) } just runs

        Timber.uprootAll() // Disable Timber logging for tests
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun `GIVEN empty promo code WHEN init THEN invalid promo code dialog is shown`() = runTest {
        val queryParams = emptyMap<String, String>()

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_invalid_code))
    }

    @Test
    fun `GIVEN wallet fetch error WHEN getSelectedWalletUseCase THEN failed dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Left(GetUserWalletError.UserWalletNotFound)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error))
    }

    @Test
    fun `GIVEN token status error WHEN getWalletCurrenciesScreen THEN failed dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        coEvery { getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWallet.walletId) } returns Either.Left(
            TokenListError.DataError(Exception("boom")),
        )

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        advanceUntilIdle()

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_error))
    }

    @Test
    fun `GIVEN no bitcoin address WHEN getWalletCurrenciesScreen THEN no bitcoin address dialog is shown`() = runTest {
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to "PROMO123")
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        // Return only ETH status so BTC is not found
        val ethStatus = buildStatusForNetwork(rawNetworkId = "ethereum", address = "0x123")
        coEvery { getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWallet.walletId) } returns Either.Right(
            listOf(ethStatus),
        )

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        advanceUntilIdle()

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_no_address))
    }

    @Test
    fun `GIVEN activation success WHEN activatePromoCode THEN activated dialog is shown`() = runTest {
        val promoCode = "PROMO123"
        val queryParams = mapOf(DeeplinkConst.PROMO_CODE_KEY to promoCode)
        val userWallet = mockUserWallet("ABCDEF")
        every { getSelectedWalletSyncUseCase.invoke() } returns Either.Right(userWallet)
        val btcStatus = buildStatusForNetwork(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
        coEvery { getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWallet.walletId) } returns Either.Right(
            listOf(btcStatus),
        )
        coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qxyz", promoCode) } returns Either.Right("ok")

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        advanceUntilIdle()

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success_title))
        Truth.assertThat(sent.message).isEqualTo(resourceReference(R.string.bitcoin_promo_activation_success))
    }

    @Test
    fun `GIVEN activation invalid code WHEN activatePromoCode THEN invalid promo code dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.InvalidPromoCode,
            expectedTitle = resourceReference(R.string.bitcoin_promo_invalid_code_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_invalid_code),
        )
    }

    @Test
    fun `GIVEN activation failed WHEN activatePromoCode THEN failed dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.ActivationFailed,
            expectedTitle = resourceReference(R.string.bitcoin_promo_activation_error_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_activation_error),
        )
    }

    @Test
    fun `GIVEN activation no address WHEN activatePromoCode THEN no bitcoin address dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.NoBitcoinAddress,
            expectedTitle = resourceReference(R.string.bitcoin_promo_no_address_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_no_address),
        )
    }

    @Test
    fun `GIVEN activation already used WHEN activatePromoCode THEN already activated dialog is shown`() = runTest {
        runActivationErrorCase(
            error = ActivatePromoCodeError.PromocodeAlreadyUsed,
            expectedTitle = resourceReference(R.string.bitcoin_promo_already_activated_title),
            expectedMessage = resourceReference(R.string.bitcoin_promo_already_activated),
        )
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
        val btcStatus = buildStatusForNetwork(rawNetworkId = Blockchain.Bitcoin.id, address = "bc1qxyz")
        coEvery { getMultiCryptoCurrencyStatusUseCase.invokeMultiWalletSync(userWallet.walletId) } returns Either.Right(
            listOf(btcStatus),
        )
        coEvery { activateBitcoinPromocodeUseCase.invoke("bc1qxyz", promoCode) } returns Either.Left(error)

        DefaultPromoDeeplinkHandler(
            scope = this,
            queryParams = queryParams,
            uiMessageSender = uiMessageSender,
            getMultiCryptoCurrencyStatusUseCase = getMultiCryptoCurrencyStatusUseCase,
            activateBitcoinPromocodeUseCase = activateBitcoinPromocodeUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )

        advanceUntilIdle()

        val sent = messageSlot.captured as DialogMessage
        Truth.assertThat(sent.title).isEqualTo(expectedTitle)
        Truth.assertThat(sent.message).isEqualTo(expectedMessage)
    }

    private fun mockUserWallet(id: String): UserWallet {
        val userWallet = mockk<UserWallet>(relaxed = true)
        every { userWallet.walletId } returns UserWalletId(id)
        return userWallet
    }

    private fun buildStatusForNetwork(rawNetworkId: String, address: String): CryptoCurrencyStatus {
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
        )

        val currencyId = CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId(rawNetworkId),
            suffix = CryptoCurrency.ID.Suffix.RawID(rawNetworkId),
        )

        val coin = CryptoCurrency.Coin(
            id = currencyId,
            network = network,
            name = rawNetworkId,
            symbol = network.currencySymbol,
            decimals = 8,
            iconUrl = null,
            isCustom = false,
        )

        val value = CryptoCurrencyStatus.NoQuote(
            amount = BigDecimal.ZERO,
            yieldBalance = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = address,
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )

        return CryptoCurrencyStatus(
            currency = coin,
            value = value,
        )
    }
}