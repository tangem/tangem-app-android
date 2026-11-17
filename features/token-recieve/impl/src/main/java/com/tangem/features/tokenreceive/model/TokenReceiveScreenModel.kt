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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ModelScoped
internal class TokenReceiveScreenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    analyticsEventHandler: AnalyticsEventHandler,
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

    private val selectedAddressIndex = MutableStateFlow(0)

    val state: StateFlow<ReceiveScreenUM> = combine(params.receiveState, selectedAddressIndex, ::createState)
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = createState(params.receiveState.value, selectedAddressIndex.value),
        )

    init {
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                token = params.tokenName,
                blockchainName = config.cryptoCurrency.network.name,
                ensStatus = if (state.value.ensName != null) {
                    AnalyticsParam.EmptyFull.Full
                } else {
                    AnalyticsParam.EmptyFull.Empty
                },
            ),
        )
    }

    fun onShareClick() {
        val addresses = state.value.addresses
        params.callback.onShareClick(addresses[selectedAddressIndex.value.coerceIn(addresses.indices)].value)
    }

    private fun createState(receiveState: TokenReceiveUM, selectedIndex: Int): ReceiveScreenUM = stateFactory.create(
        addresses = receiveState.addresses,
        selectedIndex = selectedIndex,
        onCopyAddressClick = ::copy,
        onCopyEnsClick = ::copy,
        onAddressPageChange = { selectedAddressIndex.value = it },
    )

    private fun copy(address: ReceiveAddress) {
        params.callback.onCopyClick(address = address, source = TokenReceiveCopyActionSource.Receive)
        messageSender.send(
            SnackbarMessage(
                startIconId = R.drawable.ic_check_24,
                message = resourceReference(R.string.wallet_notification_address_copied),
            ),
        )
    }
}