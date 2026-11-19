package com.tangem.features.tokenreceive.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.toast.TangemToast
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.TangemToastMessage
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.features.tokenreceive.component.TokenReceiveScreenComponent
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenReceiveScreenModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val callback: TokenReceiveScreenComponent.ModelCallback = mockk(relaxed = true)

    private val defaultAddress = ReceiveAddress(
        value = "0x3987734c3379A3E2e3eF21c3C8090Cc7aBCD6260",
        type = ReceiveAddress.Type.Primary.Default(displayName = stringReference("Default")),
    )
    private val ensAddress = ReceiveAddress(value = "cryptomonster.eth", type = ReceiveAddress.Type.Ens)
    private val currency = createEthereum()
    private val legacyAddress = ReceiveAddress(
        value = "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
        type = ReceiveAddress.Type.Primary.Legacy(displayName = stringReference("Legacy")),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, messageSender, callback)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN addresses WHEN model created THEN sends receive screen opened with ens status`(model: OpenedModel) {
        // Arrange
        val event = slot<AnalyticsEvent>()

        // Act
        val screenModel = createModel(addresses = model.addresses)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(capture(event)) }
        assertThat(event.captured.id).isEqualTo("[Token / Receive] Receive Screen Opened")
        assertThat(event.captured.params).isEqualTo(
            mapOf("Token" to "ETH", "Blockchain" to "Ethereum", "ENS" to model.expectedEns),
        )
        screenModel.onDestroy()
    }

    private fun provideTestModels() = listOf(
        OpenedModel(addresses = listOf(defaultAddress, ensAddress), expectedEns = "Full"),
        OpenedModel(addresses = listOf(defaultAddress), expectedEns = "Empty"),
    )

    @Test
    fun `GIVEN ens still loading WHEN ens resolved THEN sends receive screen opened once with full ens`() {
        // Arrange
        val receiveState = MutableStateFlow(createReceiveState(listOf(defaultAddress), isEnsResultLoading = true))
        val screenModel = createModel(receiveState = receiveState)
        verify(exactly = 0) { analyticsEventHandler.send(any()) }

        // Act
        receiveState.value = createReceiveState(listOf(defaultAddress, ensAddress), isEnsResultLoading = false)
        receiveState.value = createReceiveState(listOf(defaultAddress), isEnsResultLoading = false)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                    token = "ETH",
                    blockchainName = "Ethereum",
                    ensStatus = AnalyticsParam.EmptyFull.Full,
                ),
            )
        }
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
        screenModel.onDestroy()
    }

    @Test
    fun `GIVEN address WHEN copy address button THEN copies it with copy button source and shows copied toast`() {
        // Arrange
        val screenModel = createModel(addresses = listOf(defaultAddress, ensAddress))

        // Act
        screenModel.state.value.onCopyAddressClick()

        // Assert
        verify(exactly = 1) { callback.onCopyClick(defaultAddress, TokenReceiveCopyActionSource.CopyButton) }
        assertThat(sentToasts()).containsExactly(copiedToast())
        screenModel.onDestroy()
    }

    @Test
    fun `GIVEN address card WHEN tap qr or address THEN copies it with qr code source and shows copied toast`() {
        // Arrange
        val screenModel = createModel(addresses = listOf(defaultAddress, legacyAddress))

        // Act
        screenModel.state.value.addresses[1].onCopyClick()

        // Assert
        verify(exactly = 1) { callback.onCopyClick(legacyAddress, TokenReceiveCopyActionSource.QrCode) }
        assertThat(sentToasts()).containsExactly(copiedToast())
        screenModel.onDestroy()
    }

    @Test
    fun `GIVEN ens name WHEN copy ens THEN copies it from receive and shows copied toast`() {
        // Arrange
        val screenModel = createModel(addresses = listOf(defaultAddress, ensAddress))

        // Act
        screenModel.state.value.onCopyEnsClick()

        // Assert
        verify(exactly = 1) { callback.onCopyClick(ensAddress, TokenReceiveCopyActionSource.Receive) }
        assertThat(sentToasts()).containsExactly(copiedToast())
        screenModel.onDestroy()
    }

    @Test
    fun `GIVEN address WHEN share THEN shares the shown address and sends share event`() {
        // Arrange
        val screenModel = createModel(addresses = listOf(defaultAddress, ensAddress))

        // Act
        screenModel.onShareClick()

        // Assert
        verify(exactly = 1) { callback.onShareClick(defaultAddress.value) }
        verify(exactly = 1) {
            analyticsEventHandler.send(
                TokenReceiveNewAnalyticsEvent.ButtonShareAddress(
                    token = "ETH",
                    blockchainName = "Ethereum",
                    source = TokenReceiveCopyActionSource.Receive,
                ),
            )
        }
        screenModel.onDestroy()
    }

    @Test
    fun `GIVEN legacy page selected WHEN share and copy address THEN both take the legacy address`() {
        // Arrange
        val screenModel = createModel(addresses = listOf(defaultAddress, legacyAddress))

        // Act
        screenModel.state.value.onAddressPageChange(1)
        screenModel.onShareClick()
        screenModel.state.value.onCopyAddressClick()

        // Assert
        verify(exactly = 1) { callback.onShareClick(legacyAddress.value) }
        verify(exactly = 1) { callback.onCopyClick(legacyAddress, TokenReceiveCopyActionSource.CopyButton) }
        screenModel.onDestroy()
    }

    data class OpenedModel(val addresses: List<ReceiveAddress>, val expectedEns: String)

    private fun sentToasts(): List<CopiedToast> {
        val messages = mutableListOf<EventMessage>()
        verify { messageSender.send(capture(messages)) }
        return messages.filterIsInstance<TangemToastMessage>().map {
            CopiedToast(label = it.label, appearance = it.appearance, isClosable = it.isClosable)
        }
    }

    private fun copiedToast() = CopiedToast(
        label = resourceReference(R.string.wallet_notification_address_copied),
        appearance = TangemToast.Appearance.Success,
        isClosable = false,
    )

    data class CopiedToast(val label: TextReference, val appearance: TangemToast.Appearance, val isClosable: Boolean)

    private fun createReceiveState(addresses: List<ReceiveAddress>, isEnsResultLoading: Boolean = false) =
        TokenReceiveUM(
            network = currency.network.name,
            iconState = CurrencyIconState.Loading,
            addresses = addresses.toImmutableList(),
            isEnsResultLoading = isEnsResultLoading,
            notificationConfigs = persistentListOf(),
        )

    private fun createModel(addresses: List<ReceiveAddress>): TokenReceiveScreenModel =
        createModel(receiveState = MutableStateFlow(createReceiveState(addresses)))

    private fun createModel(receiveState: MutableStateFlow<TokenReceiveUM>): TokenReceiveScreenModel {
        return TokenReceiveScreenModel(
            paramsContainer = MutableParamsContainer(
                TokenReceiveScreenComponent.Params(
                    config = TokenReceiveConfig(
                        shouldShowWarning = false,
                        cryptoCurrency = currency,
                        userWalletId = UserWalletId("011"),
                        shouldShowMemoDisclaimer = false,
                        receiveAddress = emptyList(),
                    ),
                    tokenName = currency.symbol,
                    receiveState = receiveState,
                    warnings = persistentListOf(),
                    callback = callback,
                ),
            ),
            dispatchers = TestingCoroutineDispatcherProvider(),
            analyticsEventHandler = analyticsEventHandler,
            messageSender = messageSender,
        )
    }

    private fun createEthereum(): CryptoCurrency.Coin {
        val blockchain = Blockchain.Ethereum
        return CryptoCurrency.Coin(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(blockchain.toNetworkId()),
                suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
            ),
            network = Network(
                id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
                name = blockchain.fullName,
                currencySymbol = blockchain.currency,
                derivationPath = Network.DerivationPath.None,
                isTestnet = false,
                standardType = Network.StandardType.Unspecified(blockchain.name),
                hasFiatFeeRate = true,
                canHandleTokens = true,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = blockchain.fullName,
            symbol = blockchain.currency,
            decimals = 18,
            iconUrl = null,
            isCustom = false,
        )
    }
}