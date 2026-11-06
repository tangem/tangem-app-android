package com.tangem.tap.features.details.ui.cardsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.model.CardSettingsModel
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
        val state by model.screenState.collectAsStateWithLifecycle()
        CardSettingsScreen(modifier = modifier, state = state)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: CardSettingsComponent.Params): CardSettingsMainComponent
    }
}