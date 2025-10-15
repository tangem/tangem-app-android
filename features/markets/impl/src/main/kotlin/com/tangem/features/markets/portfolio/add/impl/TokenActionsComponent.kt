package com.tangem.features.markets.portfolio.add.impl

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
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.features.markets.portfolio.add.impl.model.TokenActionsModel
import com.tangem.features.markets.portfolio.add.impl.ui.TokenActionsContent
import com.tangem.features.markets.portfolio.impl.analytics.PortfolioAnalyticsEvent
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.tokenreceive.TokenReceiveComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow

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
        TokenActionsContent(
            modifier = modifier,
            state = tokenActionsUM,
        )
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
        val eventBuilder: PortfolioAnalyticsEvent.EventBuilder,
        val data: Flow<PortfolioData.CryptoCurrencyData>,
        val callbacks: Callbacks,
    )

    interface Callbacks {
        fun onLaterClick()
    }

    @AssistedFactory
    interface Factory : ComponentFactory<Params, TokenActionsComponent> {
        override fun create(context: AppComponentContext, params: Params): TokenActionsComponent
    }
}