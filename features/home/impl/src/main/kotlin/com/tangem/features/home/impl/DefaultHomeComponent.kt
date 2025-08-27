package com.tangem.features.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.home.api.HomeComponent
import com.tangem.features.home.impl.model.HomeModel
import com.tangem.features.home.impl.ui.Home
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultHomeComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: HomeComponent.Params,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : HomeComponent, AppComponentContext by appComponentContext {

    private val model: HomeModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        Home(
            state = state,
            modifier = modifier,
            isV2StoriesEnabled = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @AssistedFactory
    interface Factory : HomeComponent.Factory {
        override fun create(context: AppComponentContext, params: HomeComponent.Params): DefaultHomeComponent
    }
}