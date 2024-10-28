package com.tangem.feature.wallet.presentation.selecttoken

import androidx.annotation.StringRes
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Select token component
 *
[REDACTED_AUTHOR]
 */
interface SelectTokenComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, SelectTokenComponent>

    /**
     * Params
     *
     * @property hasSearchBar flag that indicates if search bar should be shown
     * @property userWalletId id of multi-currency wallet
     * @property titleResId   resource id of title
     * @property onTokenClick callback for token click
     */
    data class Params(
        val hasSearchBar: Boolean,
        val userWalletId: UserWalletId,
        @StringRes val titleResId: Int,
        val onTokenClick: (CryptoCurrencyStatus) -> Unit,
    )
}