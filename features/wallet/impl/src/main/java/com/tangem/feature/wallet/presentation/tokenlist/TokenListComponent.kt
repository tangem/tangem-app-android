package com.tangem.feature.wallet.presentation.tokenlist

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId

/** Token list component that present list of token for multi-currency wallet */
@Stable
internal interface TokenListComponent : ComposableContentComponent {

    /** Component factory */
    interface Factory : ComponentFactory<Params, TokenListComponent>

    /**
     * Params
     *
     * @property hasSearchBar flag that indicates if search bar should be shown
     * @property userWalletId id of multi-currency wallet
     * @property onTokenClick callback for token click
     */
    data class Params(
        val hasSearchBar: Boolean,
        val userWalletId: UserWalletId,
        val onTokenClick: (CryptoCurrencyStatus) -> Unit,
    )
}
