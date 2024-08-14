package com.tangem.features.managetokens.component

import androidx.compose.runtime.Composable
import com.tangem.domain.wallets.models.UserWalletId

interface AddCustomTokenComponent {

    @Composable
    fun BottomSheet(isVisible: Boolean, onDismiss: () -> Unit)

    data class Params(
        val userWalletId: UserWalletId,
    )

    interface Factory {
        fun create(params: Params): AddCustomTokenComponent
    }
}