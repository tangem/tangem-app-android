package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.features.managetokens.component.CustomTokenDerivationInputComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.CustomTokenSelectorDialogConfig
import com.tangem.features.managetokens.model.CustomTokenSelectorModel
import com.tangem.features.managetokens.ui.CustomTokenSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultCustomTokenSelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: CustomTokenSelectorComponent.Params,
    private val customTokenDerivationInputComponentFactory: CustomTokenDerivationInputComponent.Factory,
) : CustomTokenSelectorComponent, AppComponentContext by context {

    private val model: CustomTokenSelectorModel = getOrCreateModel(params)
    private val dialogSlot = childSlot(
        source = model.dialogNavigation,
        serializer = CustomTokenSelectorDialogConfig.serializer(),
        childFactory = ::createDialog,
    )

    private fun createDialog(
        config: CustomTokenSelectorDialogConfig,
        context: ComponentContext,
    ): ComposableDialogComponent = when (config) {
        is CustomTokenSelectorDialogConfig.CustomDerivationInput -> customTokenDerivationInputComponentFactory.create(
            context = childByContext(context),
            params = CustomTokenDerivationInputComponent.Params(
                userWalletId = config.userWalletId,
                onConfirm = model::selectCustomDerivationPath,
                onDismiss = model.dialogNavigation::dismiss,
            ),
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val dialog by dialogSlot.subscribeAsState()

        CustomTokenSelectorContent(
            modifier = modifier,
            model = state,
        )

        dialog.child?.instance?.Dialog()
    }

    @AssistedFactory
    interface Factory : CustomTokenSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CustomTokenSelectorComponent.Params,
        ): DefaultCustomTokenSelectorComponent
    }
}