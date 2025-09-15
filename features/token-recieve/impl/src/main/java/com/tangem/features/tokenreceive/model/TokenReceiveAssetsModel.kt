package com.tangem.features.tokenreceive.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.tokens.model.analytics.TokenReceiveCopyActionSource
import com.tangem.domain.tokens.model.analytics.TokenReceiveNewAnalyticsEvent
import com.tangem.features.tokenreceive.component.TokenReceiveAssetsComponent
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class TokenReceiveAssetsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<TokenReceiveAssetsComponent.TokenReceiveAssetsParams>()

    init {
        analyticsEventHandler.send(
            TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                token = params.tokenName,
                blockchainName = params.fullName,
                ensStatus = configureEnsStatus(),
            ),
        )
    }

    internal val state: StateFlow<ReceiveAssetsUM>
        field = MutableStateFlow<ReceiveAssetsUM>(
            ReceiveAssetsUM(
                onCopyClick = {
                    params.callback.onCopyClick(
                        address = it,
                        source = TokenReceiveCopyActionSource.Receive,
                    )
                },
                onOpenQrCodeClick = params.callback::onQrCodeClick,
                addresses = params.addresses,
                showMemoDisclaimer = params.showMemoDisclaimer,
                isEnsResultLoading = false,
                notificationConfigs = params.notificationConfigs,
            ),
        )

    private fun configureEnsStatus(): AnalyticsParam.EnsStatus {
        val hasEnsAddress = params.addresses.any { it.type == ReceiveAddress.Type.Ens }
        return if (hasEnsAddress) {
            AnalyticsParam.EnsStatus.FULL
        } else {
            AnalyticsParam.EnsStatus.EMPTY
        }
    }
}