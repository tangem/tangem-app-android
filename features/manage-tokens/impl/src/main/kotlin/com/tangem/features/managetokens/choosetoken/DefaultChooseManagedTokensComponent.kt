package com.tangem.features.managetokens.choosetoken

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.managetokens.choosetoken.entity.ChooseManageTokensBottomSheetConfig
import com.tangem.features.managetokens.choosetoken.model.ChooseManagedTokensModel
import com.tangem.features.managetokens.choosetoken.ui.ChooseManagedTokenContent
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultChooseManagedTokensComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: ChooseManagedTokensComponent.Params,
) : ChooseManagedTokensComponent, AppComponentContext by context {

    private val model: ChooseManagedTokensModel = getOrCreateModel(params)

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = ChooseManageTokensBottomSheetConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        ChooseManagedTokenContent(
            state = uiState,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    @Suppress("UnusedPrivateMember")
    private fun bottomSheetChild(
        config: ChooseManageTokensBottomSheetConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = when (config) {
        else -> getStubComponent()
    }

    private fun getStubComponent() = StubComponent()

    class StubComponent : ComposableBottomSheetComponent {
        override fun dismiss() {}

        @Composable
        override fun BottomSheet() {
            /* no-op */
        }
    }

    @AssistedFactory
    interface Factory : ChooseManagedTokensComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: ChooseManagedTokensComponent.Params,
        ): DefaultChooseManagedTokensComponent
    }
}