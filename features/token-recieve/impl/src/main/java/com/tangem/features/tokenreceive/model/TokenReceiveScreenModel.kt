package com.tangem.features.tokenreceive.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.features.tokenreceive.component.TokenReceiveScreenComponent
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.entity.ReceiveScreenStateFactory
import com.tangem.features.tokenreceive.impl.R
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class TokenReceiveScreenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val messageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<TokenReceiveScreenComponent.Params>()
    private val config = params.config

    private val stateFactory = ReceiveScreenStateFactory(
        cryptoCurrency = config.cryptoCurrency,
        asset = config.asset,
        type = config.type,
        tokenName = params.tokenName,
        shouldShowMemoDisclaimer = config.shouldShowMemoDisclaimer,
        iconState = params.receiveState.value.iconState,
        warnings = params.warnings,
    )

    private var selectedAddressIndex = 0

    val state: StateFlow<ReceiveScreenUM> = params.receiveState
        .map(::createState)
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = createState(params.receiveState.value),
        )

    init {
        modelScope.launch {
            val loaded = params.receiveState.first { !it.isEnsResultLoading }
            val hasEns = loaded.addresses.any { it.type == ReceiveAddress.Type.Ens }
            analyticsEventHandler.send(
                TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                    token = params.tokenName,
                    blockchainName = config.cryptoCurrency.network.name,
                    ensStatus = if (hasEns) AnalyticsParam.EmptyFull.Full else AnalyticsParam.EmptyFull.Empty,
                ),
            )
        }
    }

    fun onShareClick() {
        val address = selectedAddress() ?: return
        params.callback.onShareClick(address.value)
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.ButtonShareAddress(
                token = params.tokenName,
                blockchainName = config.cryptoCurrency.network.name,
                source = TokenReceiveCopyActionSource.Receive,
            ),
        )
    }

    private fun createState(receiveState: TokenReceiveUM): ReceiveScreenUM = stateFactory.create(
        addresses = receiveState.addresses,
        onCopyAddressClick = { copy(it, TokenReceiveCopyActionSource.QrCode) },
        onCopySelectedAddressClick = { selectedAddress()?.let { copy(it, TokenReceiveCopyActionSource.CopyButton) } },
        onCopyEnsClick = { copy(it, TokenReceiveCopyActionSource.Receive) },
        onAddressPageChange = { selectedAddressIndex = it },
    )

    private fun selectedAddress(): ReceiveAddress? = params.receiveState.value.addresses
        .filter { it.type is ReceiveAddress.Type.Primary }
        .getOrNull(selectedAddressIndex)

    private fun copy(address: ReceiveAddress, source: TokenReceiveCopyActionSource) {
        params.callback.onCopyClick(address = address, source = source)
        messageSender.send(
            SnackbarMessage(
                startIconId = R.drawable.ic_check_24,
                message = resourceReference(R.string.wallet_notification_address_copied),
            ),
        )
    }
}