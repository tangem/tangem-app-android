package com.tangem.features.managetokens.component

import androidx.compose.foundation.lazy.LazyListScope
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

internal interface CustomTokenFormComponent {

    fun content(scope: LazyListScope)

    data class Params(
        val userWalletId: UserWalletId,
        val networkId: Network.ID,
    )

    interface Factory {
        fun create(params: Params): CustomTokenFormComponent
    }
}