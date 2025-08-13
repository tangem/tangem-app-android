package com.tangem.features.onramp.tokenlist

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation

/** Token list component that present list of token for multi-currency wallet */
@Stable
internal interface OnrampTokenListComponent : ComposableContentComponent {

    /** Component factory */
    interface Factory : ComponentFactory<Params, OnrampTokenListComponent>

    /**
     * Params
     *
     * @property filterOperation        operation that is used to filter tokens by availability
     * @property userWalletId           user wallet id of multi-currency wallet
     * @property onTokenClick           callback for token click
     * @property onTokenListInitialized callback for token list initialization
     */
    data class Params(
        val filterOperation: OnrampOperation,
        val userWalletId: UserWalletId,
        val onTokenClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
        val onTokenListInitialized: () -> Unit = {},
    )
}