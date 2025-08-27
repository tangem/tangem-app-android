package com.tangem.feature.tokendetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.NavigationBar3ButtonsScrim
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsModel
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.TokenDetailsScreen
import com.tangem.features.markets.token.block.TokenMarketBlockComponent
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.tokenreceive.TokenReceiveComponent
import com.tangem.features.txhistory.component.TxHistoryComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class DefaultTokenDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: TokenDetailsComponent.Params,
    tokenMarketBlockComponentFactory: TokenMarketBlockComponent.Factory,
    txHistoryComponentFactory: TxHistoryComponent.Factory,
    private val tokenReceiveComponentFactory: TokenReceiveComponent.Factory,
) : TokenDetailsComponent, AppComponentContext by appComponentContext {

    private val model: TokenDetailsModel = getOrCreateModel(params)
    private val txHistoryComponent = txHistoryComponentFactory.create(
        context = child("txHistoryComponent"),
        params = TxHistoryComponent.Params(
            userWalletId = params.userWalletId,
            currency = params.currency,
            openExplorer = model::onExploreClick,
        ),
    )

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = TokenReceiveConfig.serializer(),
        handleBackButton = false,
        childFactory = ::bottomSheetChild,
    )

    init {
        lifecycle.subscribe(
            onPause = model::onPause,
            onResume = model::onResume,
        )
    }

    private val tokenMarketBlockComponent = params.currency.toTokenMarketParam()?.let { tokenMarketParams ->
        tokenMarketBlockComponentFactory.create(
            appComponentContext = child("tokenMarketBlockComponent"),
            params = tokenMarketParams,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val bottomSheet by bottomSheetSlot.subscribeAsState()
        NavigationBar3ButtonsScrim()
        TokenDetailsScreen(
            state = state,
            tokenMarketBlockComponent = tokenMarketBlockComponent,
            txHistoryComponent = txHistoryComponent,
        )
        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun CryptoCurrency.toTokenMarketParam(): TokenMarketBlockComponent.Params? {
        id.rawCurrencyId ?: return null // token price is not available

        return TokenMarketBlockComponent.Params(cryptoCurrency = this)
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

    @AssistedFactory
    interface Factory : TokenDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TokenDetailsComponent.Params,
        ): DefaultTokenDetailsComponent
    }
}