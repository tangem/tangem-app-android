package com.tangem.feature.tokendetails.presentation.tokendetails.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.dynamicaddresses.CreateConsolidationTransactionUseCase
import com.tangem.domain.dynamicaddresses.DisableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesError
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.dynamicaddresses.model.DynamicAddressesStatus
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.TokenDetailsAnalyticsEvent
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.dynamicaddresses.DynamicAddressesBottomSheetConfig
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val TEST_XPUB = "xpub-test-value"
private const val TOKEN_SYMBOL = "ETH"
private const val BLOCKCHAIN_NAME = "Ethereum"
private const val TEST_ADDRESS = "0xTestAddress"

@OptIn(ExperimentalCoroutinesApi::class)
internal class DynamicAddressesDelegateTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val enableDynamicAddressesUseCase: EnableDynamicAddressesUseCase = mockk()
    private val disableDynamicAddressesUseCase: DisableDynamicAddressesUseCase = mockk()
    private val createConsolidationTransactionUseCase: CreateConsolidationTransactionUseCase = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    private val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    private val getDerivedXpubUseCase: GetDerivedXpubUseCase = mockk()
    private val dynamicAddressesRepository: DynamicAddressesRepository = mockk(relaxed = true)
    private val getExtendedPublicKeyUseCase: GetExtendedPublicKeyForCurrencyUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val network: Network = mockk(relaxed = true) {
        every { name } returns BLOCKCHAIN_NAME
    }
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { symbol } returns TOKEN_SYMBOL
        every { this@mockk.network } returns this@DynamicAddressesDelegateTest.network
    }
    private val cryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
        every { currency } returns cryptoCurrency
    }

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val userWallet: UserWallet = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    private val showBottomSheet: () -> Unit = mockk(relaxed = true)
    private val dismissBottomSheet: () -> Unit = mockk(relaxed = true)
    private val onDynamicAddressesStateChanged: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN currency is available WHEN onDynamicAddressesClick THEN DynamicAddressesScreenOpened event is sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.DISABLED)
            coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns false
            coEvery { getDerivedXpubUseCase(userWalletId, network) } returns TEST_XPUB
            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            val eventSlot = slot<AnalyticsEvent>()
            every { analyticsEventHandler.send(capture(eventSlot)) } returns Unit

            // WHEN
            delegate.onDynamicAddressesClick()

            // THEN
            val event = eventSlot.captured as TokenDetailsAnalyticsEvent.DynamicAddressesScreenOpened
            assertThat(event.category).isEqualTo("Token")
            assertThat(event.event).isEqualTo("Dynamic Addresses Screen Opened")
            assertThat(event.params).containsEntry("Token", TOKEN_SYMBOL)
            assertThat(event.params).containsEntry("Blockchain", BLOCKCHAIN_NAME)
        }

    @Test
    fun `GIVEN no currency WHEN onDynamicAddressesClick THEN no event is sent`() = runTest {
        // GIVEN
        val delegate = createDelegate(cryptoCurrencyStatus = null)

        // WHEN
        delegate.onDynamicAddressesClick()

        // THEN
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN DISABLED status AND conflicts WHEN onDynamicAddressesClick THEN Notice DynamicAddressesUnavailable is sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.DISABLED)
            coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns true
            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)

            // WHEN
            delegate.onDynamicAddressesClick()

            // THEN
            verify {
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.Notice.DynamicAddressesUnavailable> {
                        it.event == "Notice - Dynamic Addresses Unavailable" &&
                            it.params["Token"] == TOKEN_SYMBOL &&
                            it.params["Blockchain"] == BLOCKCHAIN_NAME
                    },
                )
            }
        }

    @Test
    fun `GIVEN DISABLED status WHEN enable button clicked AND enable succeeds THEN ButtonEnable and DynamicAddressesEnabled are sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.DISABLED)
            coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns false
            coEvery { getDerivedXpubUseCase(userWalletId, network) } returns TEST_XPUB
            coEvery { getExtendedPublicKeyUseCase(userWalletId, network) } returns TEST_XPUB.right()
            coEvery { enableDynamicAddressesUseCase(userWalletId, network, TEST_XPUB) } returns Unit.right()

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.Enable).onEnableClick()

            // THEN
            verify {
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.ButtonEnableDynamicAddresses> {
                        it.event == "Button - Enable Dynamic Addresses" &&
                            it.params["Token"] == TOKEN_SYMBOL
                    },
                )
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.DynamicAddressesEnabled> {
                        it.event == "Dynamic Addresses Enabled" &&
                            it.params["Token"] == TOKEN_SYMBOL
                    },
                )
            }
            coVerify { enableDynamicAddressesUseCase(userWalletId, network, TEST_XPUB) }
        }

    @Test
    fun `GIVEN DISABLED status WHEN enable button clicked AND xpub retrieval fails THEN Error DynamicAddressesUnavailable is sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.DISABLED)
            coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns false
            coEvery { getDerivedXpubUseCase(userWalletId, network) } returns TEST_XPUB
            coEvery { getExtendedPublicKeyUseCase(userWalletId, network) } returns
                IllegalStateException("xpub fail").left()

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.Enable).onEnableClick()

            // THEN
            verify {
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable> {
                        it.event == "Error - Dynamic Addresses Unavailable" &&
                            it.params["Token"] == TOKEN_SYMBOL
                    },
                )
            }
        }

    @Test
    fun `GIVEN DISABLED status WHEN enable button clicked AND user cancels xpub derivation THEN Error event is NOT sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.DISABLED)
            coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns false
            coEvery { getDerivedXpubUseCase(userWalletId, network) } returns TEST_XPUB
            coEvery { getExtendedPublicKeyUseCase(userWalletId, network) } returns
                TangemSdkError.UserCancelled().left()

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.Enable).onEnableClick()

            // THEN
            verify(exactly = 0) {
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable>())
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.DynamicAddressesEnabled>())
            }
        }

    @Test
    fun `GIVEN DISABLED status WHEN enable useCase fails THEN Error DynamicAddressesUnavailable is sent`() = runTest {
        // GIVEN
        every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
            flowOf(DynamicAddressesStatus.DISABLED)
        coEvery { dynamicAddressesRepository.hasConflictingCustomTokens(userWalletId, network) } returns false
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns TEST_XPUB
        coEvery { getExtendedPublicKeyUseCase(userWalletId, network) } returns TEST_XPUB.right()
        coEvery { enableDynamicAddressesUseCase(userWalletId, network, TEST_XPUB) } returns
            EnableDynamicAddressesError.ServiceError(RuntimeException("boom")).left()

        val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
        delegate.onDynamicAddressesClick()

        // WHEN
        (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.Enable).onEnableClick()

        // THEN
        verify {
            analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable>())
        }
        verify(exactly = 0) {
            analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.DynamicAddressesEnabled>())
        }
    }

    @Test
    fun `GIVEN ENABLED status AND no consolidation WHEN simple disable clicked THEN ButtonDisable and DynamicAddressesDisabled are sent`() =
        runTest {
            // GIVEN
            every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
                flowOf(DynamicAddressesStatus.ENABLED)
            coEvery { disableDynamicAddressesUseCase(userWalletId, network) } returns false.right()
            coEvery { dynamicAddressesRepository.disable(userWalletId, network) } returns Unit

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.DisableWithoutConsolidation)
                .onDisableClick()

            // THEN
            verify {
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.ButtonDisableDynamicAddresses> {
                        it.event == "Button - Disable Dynamic Addresses"
                    },
                )
                analyticsEventHandler.send(
                    match<TokenDetailsAnalyticsEvent.DynamicAddressesDisabled> {
                        it.event == "Dynamic Addresses Disabled"
                    },
                )
            }
        }

    @Test
    fun `GIVEN consolidation required AND fee fails WHEN load fee THEN NotEnoughFee with DynamicAddresses source is sent`() =
        runTest {
            // GIVEN: consolidation required, status provides balance and address, fee load fails
            setupConsolidationFlow()
            coEvery {
                getFeeUseCase(any<BigDecimal>(), any<String>(), userWallet, cryptoCurrency)
            } returns mockk<GetFeeError>(relaxed = true).left()
            val events = mutableListOf<AnalyticsEvent>()
            every { analyticsEventHandler.send(capture(events)) } returns Unit

            // WHEN
            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // THEN
            val notEnoughFee = events
                .filterIsInstance<TokenDetailsAnalyticsEvent.Notice.NotEnoughFee>()
                .single()
            assertThat(notEnoughFee.event).isEqualTo("Notice - Not Enough Fee")
            assertThat(notEnoughFee.params).containsEntry("Source", "Dynamic Addresses")
            assertThat(notEnoughFee.params).containsEntry("Token", TOKEN_SYMBOL)
            assertThat(notEnoughFee.params).containsEntry("Blockchain", BLOCKCHAIN_NAME)
        }

    @Test
    fun `GIVEN consolidation required AND fee fails WHEN load fee retried THEN NotEnoughFee is sent only once`() =
        runTest {
            // GIVEN
            setupConsolidationFlow()
            coEvery {
                getFeeUseCase(any<BigDecimal>(), any<String>(), userWallet, cryptoCurrency)
            } returns mockk<GetFeeError>(relaxed = true).left()
            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)

            // WHEN: initial load + refresh
            delegate.onDynamicAddressesClick()
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.DisableWithConsolidation)
                .onRefreshFee()

            // THEN: one-time event sender collapses repeated errors
            verify(exactly = 1) {
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.Notice.NotEnoughFee>())
            }
        }

    @Test
    fun `GIVEN consolidation flow WHEN disable clicked AND tx succeeds THEN ButtonDisable and DynamicAddressesDisabled are sent`() =
        runTest {
            // GIVEN
            setupConsolidationFlow()
            coEvery {
                getFeeUseCase(any<BigDecimal>(), any<String>(), userWallet, cryptoCurrency)
            } returns mockk<GetFeeError>(relaxed = true).left()
            val txData = mockk<TransactionData>(relaxed = true)
            coEvery { createConsolidationTransactionUseCase(userWalletId, network) } returns txData.right()
            coEvery { sendTransactionUseCase(txData, userWallet, network) } returns "tx-hash".right()
            coEvery { dynamicAddressesRepository.disable(userWalletId, network) } returns Unit

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.DisableWithConsolidation)
                .onDisableClick()

            // THEN
            verify {
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.ButtonDisableDynamicAddresses>())
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.DynamicAddressesDisabled>())
            }
            coVerify { sendTransactionUseCase(txData, userWallet, network) }
        }

    @Test
    fun `GIVEN consolidation flow WHEN disable clicked AND tx cancelled by user THEN DynamicAddressesDisabled is NOT sent`() =
        runTest {
            // GIVEN
            setupConsolidationFlow()
            coEvery {
                getFeeUseCase(any<BigDecimal>(), any<String>(), userWallet, cryptoCurrency)
            } returns mockk<GetFeeError>(relaxed = true).left()
            val txData = mockk<TransactionData>(relaxed = true)
            coEvery { createConsolidationTransactionUseCase(userWalletId, network) } returns txData.right()
            coEvery { sendTransactionUseCase(txData, userWallet, network) } returns
                SendTransactionError.UserCancelledError.left()

            val delegate = createDelegate(cryptoCurrencyStatus = cryptoCurrencyStatus)
            delegate.onDynamicAddressesClick()

            // WHEN
            (delegate.bottomSheetConfig.value as DynamicAddressesBottomSheetConfig.DisableWithConsolidation)
                .onDisableClick()

            // THEN: ButtonDisable is sent on click, but success event is not
            verify { analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.ButtonDisableDynamicAddresses>()) }
            verify(exactly = 0) {
                analyticsEventHandler.send(ofType<TokenDetailsAnalyticsEvent.DynamicAddressesDisabled>())
            }
        }

    @Test
    fun `GIVEN Notice category event WHEN sent THEN id starts with Token category`() {
        // GIVEN
        val notice = TokenDetailsAnalyticsEvent.Notice.DynamicAddressesUnavailable(cryptoCurrency)
        val error = TokenDetailsAnalyticsEvent.Error.DynamicAddressesUnavailable(cryptoCurrency)

        // THEN
        assertThat(notice.category).isEqualTo("Token")
        assertThat(notice.event).isEqualTo("Notice - Dynamic Addresses Unavailable")
        assertThat(error.category).isEqualTo("Token")
        assertThat(error.event).isEqualTo("Error - Dynamic Addresses Unavailable")
    }

    private fun setupConsolidationFlow() {
        every { dynamicAddressesRepository.getStatus(userWalletId, network) } returns
            flowOf(DynamicAddressesStatus.ENABLED)
        coEvery { disableDynamicAddressesUseCase(userWalletId, network) } returns true.right()
        val address = NetworkAddress.Address(value = TEST_ADDRESS, type = NetworkAddress.Address.Type.Primary)
        every { cryptoCurrencyStatus.value } returns mockk(relaxed = true) {
            every { amount } returns BigDecimal.ONE
            every { fiatRate } returns BigDecimal.ONE
            every { networkAddress } returns NetworkAddress.Single(defaultAddress = address)
        }
    }

    private fun createDelegate(cryptoCurrencyStatus: CryptoCurrencyStatus?): DynamicAddressesDelegate {
        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        return DynamicAddressesDelegate(
            enableDynamicAddressesUseCase = enableDynamicAddressesUseCase,
            disableDynamicAddressesUseCase = disableDynamicAddressesUseCase,
            createConsolidationTransactionUseCase = createConsolidationTransactionUseCase,
            getFeeUseCase = getFeeUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            getDerivedXpubUseCase = getDerivedXpubUseCase,
            dynamicAddressesRepository = dynamicAddressesRepository,
            getExtendedPublicKeyUseCase = getExtendedPublicKeyUseCase,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
            dispatchers = TestingCoroutineDispatcherProvider(),
            userWallet = userWallet,
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            appCurrencyProvider = Provider { mockk<AppCurrency>(relaxed = true) },
            coroutineScope = scope,
            showBottomSheet = showBottomSheet,
            dismissBottomSheet = dismissBottomSheet,
            onDynamicAddressesStateChanged = onDynamicAddressesStateChanged,
        )
    }
}