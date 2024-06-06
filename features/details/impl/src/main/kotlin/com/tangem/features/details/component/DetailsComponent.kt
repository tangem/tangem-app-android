package com.tangem.features.details.component

import androidx.compose.material3.SnackbarHostState
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.ComposableContentProvider
import com.tangem.domain.wallets.models.UserWalletId

interface DetailsComponent : ComposableContentProvider {

    val snackbarHostState: SnackbarHostState

    interface Factory {

        fun create(context: AppComponentContext, params: Params): DetailsComponent
    }

    data class Params(
        val selectedUserWalletId: UserWalletId,
    )
}