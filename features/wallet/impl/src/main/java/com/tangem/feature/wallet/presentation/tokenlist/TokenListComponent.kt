package com.tangem.feature.wallet.presentation.tokenlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId

/** Token list component that present list of token for multi-currency wallet */
@Stable
internal interface TokenListComponent {

    @Composable
    fun Content(contentPadding: PaddingValues, modifier: Modifier)

    /** Component factory */
    interface Factory : ComponentFactory<Params, TokenListComponent>

    /**
     * Params
     *
     * @property hasSearchBar   flag that indicates if search bar should be shown
     * @property userWalletId   id of multi-currency wallet
     * @property onTokenClick   callback for token click
     */
    data class Params(
        val hasSearchBar: Boolean,
        val userWalletId: UserWalletId,
        val onTokenClick: (CryptoCurrencyStatus) -> Unit,
    )
}