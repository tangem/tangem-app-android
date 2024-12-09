package com.tangem.features.onramp.swap.availablepairs

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.StateFlow

/** Token list component that present list of available tokens for swap */
@Stable
internal interface AvailableSwapPairsComponent : ComposableContentComponent {

    /** Component factory */
    interface Factory : ComponentFactory<Params, AvailableSwapPairsComponent>

    /**
     * Params
     *
     * @property userWalletId   id of multi-currency wallet
     * @property selectedStatus flow of selected status
     * @property onTokenClick   callback for token click
     */
    data class Params(
        val userWalletId: UserWalletId,
        val selectedStatus: StateFlow<CryptoCurrencyStatus?>,
        val onTokenClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    )
}