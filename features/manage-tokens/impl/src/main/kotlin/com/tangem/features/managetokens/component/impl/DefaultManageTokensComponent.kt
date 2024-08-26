package com.tangem.features.managetokens.component.impl

import androidx.activity.compose.BackHandler
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
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.managetokens.BottomSheetConfig
import com.tangem.features.managetokens.model.ManageTokensModel
import com.tangem.features.managetokens.ui.ManageTokensScreen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultManageTokensComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: ManageTokensComponent.Params,
    private val addCustomTokenComponentFactory: AddCustomTokenComponent.Factory,
) : ManageTokensComponent, AppComponentContext by context {

    private val model: ManageTokensModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = BottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        BackHandler(onBack = state.popBack)

        ManageTokensScreen(
            modifier = modifier,
            state = state,
        )

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: BottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        is BottomSheetConfig.AddCustomToken -> {
            addCustomTokenComponentFactory.create(
                context = childByContext(componentContext),
                params = AddCustomTokenComponent.Params(
                    userWalletId = config.userWalletId,
                    onDismiss = model.bottomSheetNavigation::dismiss,
                ),
            )
        }
    }

    @AssistedFactory
    interface Factory : ManageTokensComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ManageTokensComponent.Params,
        ): DefaultManageTokensComponent
    }
}