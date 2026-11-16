package com.tangem.features.tokenreceive.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.features.tokenreceive.component.TokenReceiveScreenComponent
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.entity.ReceiveScreenStateFactory
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import com.tangem.features.tokenreceive.ui.state.TokenReceiveUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@ModelScoped
internal class TokenReceiveScreenModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    analyticsEventHandler: AnalyticsEventHandler,
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

    val state: StateFlow<ReceiveScreenUM> = params.receiveState
        .map(::createState)
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = createState(params.receiveState.value),
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
        params.callback.onShareClick(state.value.address)
    }

    private fun createState(receiveState: TokenReceiveUM): ReceiveScreenUM = stateFactory.create(
        addresses = receiveState.addresses,
        onCopyAddressClick = ::copy,
        onCopyEnsClick = ::copy,
    )

    private fun copy(address: ReceiveAddress) {
        params.callback.onCopyClick(address = address, source = TokenReceiveCopyActionSource.Receive)
    }
}