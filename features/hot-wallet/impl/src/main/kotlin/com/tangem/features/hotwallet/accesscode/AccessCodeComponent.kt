package com.tangem.features.hotwallet.accesscode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.security.DisableScreenshotsDisposableEffect
import com.tangem.features.hotwallet.accesscode.ui.AccessCode
import com.tangem.domain.models.wallet.UserWalletId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class AccessCodeComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Params,
) : ComposableContentComponent, AppComponentContext by context {

    private val model: AccessCodeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        DisableScreenshotsDisposableEffect()

        AccessCode(
            modifier = modifier,
            state = state,
        )
    }

    interface ModelCallbacks {
        fun onNewAccessCodeInput(userWalletId: UserWalletId, accessCode: String)
        fun onAccessCodeUpdated(userWalletId: UserWalletId)
    }

    data class Params(
        val accessCodeToConfirm: String? = null,
        val userWalletId: UserWalletId,
        val callbacks: ModelCallbacks,
    )

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): AccessCodeComponent
    }
}