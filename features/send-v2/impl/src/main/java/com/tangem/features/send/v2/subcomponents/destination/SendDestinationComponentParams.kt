package com.tangem.features.send.v2.subcomponents.destination

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.PredefinedValues
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent.ModelCallback
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SendDestinationComponentParams {

    abstract val state: DestinationUM
    abstract val analyticsCategoryName: String
    abstract val userWalletId: UserWalletId
    abstract val cryptoCurrency: CryptoCurrency

    data class DestinationParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val cryptoCurrency: CryptoCurrency,
        override val userWalletId: UserWalletId,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val currentRoute: Flow<CommonSendRoute.Destination>,
        val callback: ModelCallback,
        val onBackClick: () -> Unit,
        val onNextClick: () -> Unit,
    ) : SendDestinationComponentParams()

    data class DestinationBlockParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val userWalletId: UserWalletId,
        override val cryptoCurrency: CryptoCurrency,
        val blockClickEnableFlow: StateFlow<Boolean>,
        val predefinedValues: PredefinedValues,
    ) : SendDestinationComponentParams()
}