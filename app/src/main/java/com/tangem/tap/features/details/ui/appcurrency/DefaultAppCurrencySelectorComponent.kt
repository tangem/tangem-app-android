package com.tangem.tap.features.details.ui.appcurrency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.tap.features.details.ui.appcurrency.api.AppCurrencySelectorComponent
import com.tangem.tap.features.details.ui.appcurrency.model.AppCurrencySelectorModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultAppCurrencySelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AppCurrencySelectorComponent, AppComponentContext by appComponentContext {

    private val model: AppCurrencySelectorModel = getOrCreateModel()

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        NavigationBar3ButtonsScrim()
        AppCurrencySelectorScreen(
            modifier = modifier,
            state = uiState,
        )
    }

    @AssistedFactory
    interface Factory : AppCurrencySelectorComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAppCurrencySelectorComponent
    }
}