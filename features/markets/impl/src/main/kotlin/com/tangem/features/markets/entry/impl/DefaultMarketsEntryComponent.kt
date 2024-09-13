package com.tangem.features.markets.entry.impl

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.domain.markets.toSerializableParam
import com.tangem.features.markets.entry.impl.MarketsEntryChildFactory.Child
import com.tangem.features.markets.entry.impl.ui.EntryBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val marketsEntryChildFactory: MarketsEntryChildFactory,
    private val uiDependencies: UiDependencies,
) : MarketsEntryComponent, AppComponentContext by context {

    private val stackNavigation = StackNavigation<Child>()

    val stack: Value<ChildStack<Child, Any>> = childStack(
        key = "main",
        source = stackNavigation,
        serializer = Child.serializer(),
        initialConfiguration = Child.TokenList,
        handleBackButton = true,
        childFactory = { configuration, factoryContext ->
            marketsEntryChildFactory.createChild(
                child = configuration,
                appComponentContext = childByContext(
                    componentContext = factoryContext,
                    router = createRouter(configuration),
                ),
                onTokenSelected = ::marketsListTokenSelected,
            )
        },
    )

    @Suppress("LongMethod")
    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        EntryBottomSheetContent(
            bottomSheetState = bottomSheetState,
            onHeaderSizeChange = onHeaderSizeChange,
            stackState = stack.subscribeAsState(),
            modifier = modifier,
        )
        EventMessageEffect(
            messageHandler = uiDependencies.eventMessageHandler,
            snackbarHostState = uiDependencies.globalSnackbarHostState,
        )
    }

    @OptIn(ExperimentalDecomposeApi::class)
    private fun marketsListTokenSelected(token: TokenMarket, appCurrency: AppCurrency) {
        stackNavigation.pushNew(
            configuration = Child.TokenDetails(
                params = MarketsTokenDetailsComponent.Params(
                    token = token.toSerializableParam(),
                    appCurrency = appCurrency,
                    showPortfolio = true,
                ),
            ),
        )
    }

    private fun AppComponentContext.createRouter(child: Child): Router {
        return when (child) {
            is Child.TokenDetails -> {
                MarketTokenDetailsRouter(
                    contextRouter = this.router,
                    stackNavigation = stackNavigation,
                )
            }
            else -> this.router
        }
    }

    @AssistedFactory
    interface Factory : MarketsEntryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultMarketsEntryComponent
    }
}