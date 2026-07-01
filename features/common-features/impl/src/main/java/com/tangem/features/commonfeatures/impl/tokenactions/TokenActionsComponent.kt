package com.tangem.features.commonfeatures.impl.tokenactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsBSContentUM
import com.tangem.common.ui.markets.action.TokenActionsContext
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.commonfeatures.api.tokenactions.BottomAction
import com.tangem.features.commonfeatures.impl.tokenactions.model.TokenActionsModel
import com.tangem.features.commonfeatures.impl.tokenactions.ui.TokenActionsContent
import com.tangem.features.commonfeatures.impl.tokenactions.ui.TokenActionsContentV2
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class TokenActionsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: TokenActionsModel = getOrCreateModel(params)
    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state = model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        val tokenActionsUM = state.value ?: return
        if (LocalRedesignEnabled.current || params.isRedesignForced) {
            TokenActionsContentV2(
                modifier = modifier,
                state = tokenActionsUM,
            )
        } else {
            TokenActionsContent(
                modifier = modifier,
                state = tokenActionsUM,
            )
        }
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun bottomSheetChild(
        config: TokenReceiveConfig,
        componentContext: ComponentContext,
    ): ComposableBottomSheetComponent = tokenReceiveComponentFactory.create(
        context = childByContext(componentContext),
        params = TokenReceiveComponent.Params(
            config = config,
            onDismiss = model.bottomSheetNavigation::dismiss,
        ),
    )

    data class Params(
        val data: Flow<CryptoCurrencyData>,
        val callbacks: Callbacks,
        val bottomAction: Flow<BottomAction> = flowOf(BottomAction.None),
        val isRedesignForced: Boolean = false,
        val isCompact: Boolean = false,
        val context: TokenActionsContext = TokenActionsContext.Markets,
    )

    interface Callbacks {
        fun onBottomActionClick(bottomAction: BottomAction)
        fun onQuickActionClick(action: TokenActionsBSContentUM.Action, shouldDismiss: Boolean) {}
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Params, TokenActionsComponent> {
        override fun create(context: AppComponentContext, params: Params): TokenActionsComponent
    }
}