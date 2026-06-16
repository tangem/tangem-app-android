package com.tangem.features.tangempay.hotwallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.res.ForceDarkTheme
import com.tangem.features.tangempay.components.TangemPayHotWalletOnboardingComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultTangemPayHotWalletOnboardingComponent @AssistedInject constructor(
    @Assisted private val context: AppComponentContext,
    @Assisted private val params: Unit,
) : TangemPayHotWalletOnboardingComponent, AppComponentContext by context {

    private val model: TangemPayHotWalletOnboardingModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        SystemBarsIconsDisposable(darkIcons = false)
        ForceDarkTheme {
            TangemPayHotWalletOnboardingScreen(
                state = state,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayHotWalletOnboardingComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultTangemPayHotWalletOnboardingComponent
    }
}