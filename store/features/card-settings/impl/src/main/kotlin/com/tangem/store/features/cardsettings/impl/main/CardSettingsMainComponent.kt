package com.tangem.store.features.cardsettings.impl.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.store.features.cardsettings.api.CardSettingsComponent
import com.tangem.store.features.cardsettings.impl.main.model.CardSettingsModel
import com.tangem.store.features.cardsettings.impl.main.ui.CardSettingsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CardSettingsMainComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: CardSettingsComponent.Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: CardSettingsModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        CardSettingsScreen(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: CardSettingsComponent.Params): CardSettingsMainComponent
    }
}