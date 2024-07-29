package com.tangem.features.markets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.component.MarketsEntryComponent
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.api.toSerializable
import com.tangem.features.markets.tokenlist.api.MarketsTokenListComponent
import com.tangem.features.markets.tokenlist.impl.model.MarketsListModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultMarketsEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val marketsEntryChildFactory: MarketsEntryChildFactory,
) : MarketsEntryComponent, AppComponentContext by context {

    private val model: MarketsListModel = getOrCreateModel()

    private val stackNavigation = StackNavigation<MarketsEntryChildFactory.Child>()

    val stack: Value<ChildStack<MarketsEntryChildFactory.Child, Any>> = childStack(
        key = "main",
        source = stackNavigation,
        serializer = MarketsEntryChildFactory.Child.serializer(),
        initialConfiguration = MarketsEntryChildFactory.Child.TokenList,
        handleBackButton = true,
        childFactory = { configuration, componentContext ->
            marketsEntryChildFactory.createChild(
                child = configuration,
                appComponentContext = childByContext(componentContext),
                onTokenSelected = ::marketsListTokenSelected,
            )
        },
    )

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        Children(stack) {
            when (it.configuration) {
                is MarketsEntryChildFactory.Child.TokenDetails -> {
                    (it.instance as MarketsTokenDetailsComponent).Content(modifier)
                }
                MarketsEntryChildFactory.Child.TokenList -> {
                    (it.instance as MarketsTokenListComponent).BottomSheetContent(
                        bottomSheetState = bottomSheetState,
                        onHeaderSizeChange = onHeaderSizeChange,
                        modifier = modifier,
                    )
                }
            }
        }
    }

    private fun marketsListTokenSelected(token: TokenMarket) {
        stackNavigation.push(
            configuration = MarketsEntryChildFactory.Child.TokenDetails(
                params = MarketsTokenDetailsComponent.Params(token.toSerializable()),
            ),
        )
    }

    @AssistedFactory
    interface Factory : MarketsEntryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultMarketsEntryComponent
    }
}