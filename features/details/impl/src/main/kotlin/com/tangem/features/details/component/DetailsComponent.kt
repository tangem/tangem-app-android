package com.tangem.features.details.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.wallets.models.UserWalletId

interface DetailsComponent {

    @Composable
    @Suppress("TopLevelComposableFunctions") // TODO: Remove this check
    fun View(modifier: Modifier)

    interface Factory {

        fun create(context: AppComponentContext, params: Params): DetailsComponent
    }

    data class Params(
        val selectedUserWalletId: UserWalletId,
    )
}