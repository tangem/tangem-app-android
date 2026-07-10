package com.tangem.features.send.api.subcomponents.destination

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import kotlinx.coroutines.flow.StateFlow

sealed class SendDestinationComponentParams {

    abstract val state: DestinationUM
    abstract val analyticsCategoryName: String
    abstract val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource
    abstract val userWalletId: UserWalletId
    abstract val cryptoCurrency: CryptoCurrency
    abstract val isAllowSelfSend: Boolean

    data class DestinationParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val cryptoCurrency: CryptoCurrency,
        override val userWalletId: UserWalletId,
        val title: TextReference,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val route: DestinationRoute,
        val callback: SendDestinationComponent.ModelCallback,
        override val isAllowSelfSend: Boolean = false,
    ) : SendDestinationComponentParams()

    data class DestinationBlockParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val analyticsSendSource: CommonSendAnalyticEvents.CommonSendSource,
        override val userWalletId: UserWalletId,
        override val cryptoCurrency: CryptoCurrency,
        val blockClickEnableFlow: StateFlow<Boolean>,
        val predefinedValues: PredefinedValues,
        override val isAllowSelfSend: Boolean = false,
        val isAddContactAvailable: Boolean = false,
    ) : SendDestinationComponentParams()
}