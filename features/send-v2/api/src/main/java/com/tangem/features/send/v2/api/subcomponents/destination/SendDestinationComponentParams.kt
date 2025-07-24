package com.tangem.features.send.v2.api.subcomponents.destination

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class SendDestinationComponentParams {

    abstract val state: DestinationUM
    abstract val analyticsCategoryName: String
    abstract val userWalletId: UserWalletId
    abstract val cryptoCurrency: CryptoCurrency

    data class DestinationParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val cryptoCurrency: CryptoCurrency,
        override val userWalletId: UserWalletId,
        val title: TextReference,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val currentRoute: Flow<DestinationRoute>,
        val callback: SendDestinationComponent.ModelCallback,
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