package com.tangem.features.onramp.tokenlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.onramp.entity.OnrampOperation

/** Token list component that present list of token for multi-currency wallet */
@Stable
internal interface OnrampTokenListComponent {

    @Composable
    fun Content(contentPadding: PaddingValues, modifier: Modifier)

    /** Component factory */
    interface Factory : ComponentFactory<Params, OnrampTokenListComponent>

    /**
     * Params
     *
     * @property filterOperation operation that is used to filter tokens by availability
     * @property hasSearchBar    flag that indicates if search bar should be shown
     * @property userWalletId    id of multi-currency wallet
     * @property onTokenClick    callback for token click
     */
    data class Params(
        val filterOperation: OnrampOperation,
        val hasSearchBar: Boolean,
        val userWalletId: UserWalletId,
        val onTokenClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    )
}