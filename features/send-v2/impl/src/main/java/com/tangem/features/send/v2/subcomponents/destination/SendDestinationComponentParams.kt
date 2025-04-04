package com.tangem.features.send.v2.subcomponents.destination

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.v2.send.SendRoute
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent.ModelCallback
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

internal sealed class SendDestinationComponentParams {

    abstract val state: DestinationUM
    abstract val analyticsCategoryName: String
    abstract val userWallet: UserWallet
    abstract val cryptoCurrency: CryptoCurrency

    data class DestinationParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val cryptoCurrency: CryptoCurrency,
        val isBalanceHidingFlow: StateFlow<Boolean>,
        val currentRoute: Flow<SendRoute.Destination>,
        val callback: ModelCallback,
        val isEditMode: Boolean,
    ) : SendDestinationComponentParams()

    data class DestinationBlockParams(
        override val state: DestinationUM,
        override val analyticsCategoryName: String,
        override val userWallet: UserWallet,
        override val cryptoCurrency: CryptoCurrency,
        val blockClickEnableFlow: StateFlow<Boolean>,
        val predefinedAddressValue: String?,
        val predefinedMemoValue: String?,
        val isPredefinedValues: Boolean,
    ) : SendDestinationComponentParams()
}