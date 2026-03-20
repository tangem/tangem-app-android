package com.tangem.feature.wallet.child.organizetokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.organizetokens.model.OrganizeTokensModel
import com.tangem.feature.wallet.child.organizetokens.ui.OrganizeTokensContent

internal class OrganizeTokensComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: OrganizeTokensModel = getOrCreateModel(params)

    override fun dismiss() {
        params.callback.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        OrganizeTokensContent(
            organizeTokensUM = uiState,
            dragAndDropIntents = model.dragAndDropAdapter,
            onDismiss = ::dismiss,
        )
    }

    interface Callback {
        fun onDismiss()
    }

    data class Params(
        val userWalletId: UserWalletId,
        val callback: Callback,
    )
}