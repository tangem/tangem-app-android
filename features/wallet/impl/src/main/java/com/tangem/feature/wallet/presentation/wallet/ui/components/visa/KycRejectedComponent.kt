package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.wallet.UserWalletId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class KycRejectedComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: KycRejectedModel = getOrCreateModel(params = params)

    override fun dismiss() {
        model.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.uiState.collectAsStateWithLifecycle()
        MessageBottomSheetV2(state = state, onDismissRequest = ::dismiss)
    }

    data class Params(
        val callbacks: KycRejectedCallbacks,
        val walletId: UserWalletId,
        val customerId: String,
        val onDismiss: () -> Unit,
    )

    @AssistedFactory
    interface Factory {
        fun create(appComponentContext: AppComponentContext, params: Params): KycRejectedComponent
    }
}

internal interface KycRejectedCallbacks {
    fun onClickYourProfile(userWalletId: UserWalletId)
    fun onClickGoToSupport(customerId: String)
    fun onClickHideKyc(userWalletId: UserWalletId)
}