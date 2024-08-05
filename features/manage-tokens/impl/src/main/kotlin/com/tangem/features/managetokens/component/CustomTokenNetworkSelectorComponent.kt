package com.tangem.features.managetokens.component

import androidx.compose.foundation.lazy.LazyListScope
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.entity.SelectedNetworkUM

internal interface CustomTokenNetworkSelectorComponent {

    fun content(scope: LazyListScope)

    data class Params(
        val userWalletId: UserWalletId,
        val selectedNetwork: SelectedNetworkUM?,
        val onNetworkSelected: (SelectedNetworkUM) -> Unit,
    )

    interface Factory {
        fun create(params: Params): CustomTokenNetworkSelectorComponent
    }
}
