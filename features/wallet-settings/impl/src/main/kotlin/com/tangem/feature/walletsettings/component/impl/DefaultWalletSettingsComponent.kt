package com.tangem.feature.walletsettings.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.feature.walletsettings.component.RenameWalletComponent
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.model.WalletSettingsModel
import com.tangem.feature.walletsettings.ui.WalletSettingsScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultWalletSettingsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: WalletSettingsComponent.Params,
    private val renameWalletComponentFactory: RenameWalletComponent.Factory,
) : WalletSettingsComponent, AppComponentContext by context {

    private val model: WalletSettingsModel = getOrCreateModel(params)

    private val dialog = childSlot(
        source = model.dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val dialog by dialog.subscribeAsState()

        WalletSettingsScreen(
            modifier = modifier,
            state = state,
            dialog = { dialog.child?.instance?.Dialog() },
        )
    }

    private fun dialogChild(
        dialogConfig: DialogConfig,
        componentContext: ComponentContext,
    ): ComposableDialogComponent = when (dialogConfig) {
        is DialogConfig.RenameWallet -> {
            renameWalletComponentFactory.create(
                context = childByContext(componentContext),
                params = RenameWalletComponent.Params(
                    userWalletId = dialogConfig.userWalletId,
                    currentName = dialogConfig.currentName,
                    onDismiss = model.dialogNavigation::dismiss,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : WalletSettingsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletSettingsComponent.Params,
        ): DefaultWalletSettingsComponent
    }
}