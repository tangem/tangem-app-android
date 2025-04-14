package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.ui.appsettings.api.AppSettingsComponent
import com.tangem.tap.features.details.ui.appsettings.model.AppSettingsModel
import com.tangem.tap.store
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultAppSettingsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : AppSettingsComponent, AppComponentContext by appComponentContext {

    private val model: AppSettingsModel = getOrCreateModel()

    init {

        doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        AppSettingsScreen(
            modifier = modifier,
            state = state,
            onBackClick = {
                store.dispatchNavigationAction(AppRouter::pop)
            },
        )
    }

    @AssistedFactory
    interface Factory : AppSettingsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAppSettingsComponent
    }
}