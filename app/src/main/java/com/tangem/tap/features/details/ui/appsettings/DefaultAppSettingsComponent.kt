package com.tangem.tap.features.details.ui.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.tap.features.details.ui.appsettings.api.AppSettingsComponent
import com.tangem.tap.features.details.ui.appsettings.model.AppSettingsDialogConfig
import com.tangem.tap.features.details.ui.appsettings.model.AppSettingsModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAppSettingsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Suppress("UnusedPrivateMember") @Assisted params: Unit,
) : AppSettingsComponent, AppComponentContext by appComponentContext {

    private val model: AppSettingsModel = getOrCreateModel()

    private val dialogSlot = childSlot(
        source = model.dialogNavigation,
        serializer = AppSettingsDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = { config, _ -> config },
    )

    init {
        doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val dialog by dialogSlot.subscribeAsState()

        AppSettingsScreen(
            modifier = modifier,
            state = state,
            onBackClick = model::onBackClick,
        )

        dialog.child?.instance?.let { config ->
            when (config) {
                is AppSettingsDialogConfig.ThemeModeSelector -> SettingsSelectorDialog(
                    config = config,
                    onSelect = model::onThemeModeSelected,
                    onDismiss = model::dismissDialog,
                )
            }
        }
    }

    @AssistedFactory
    interface Factory : AppSettingsComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultAppSettingsComponent
    }
}