package com.tangem.features.markets.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.entry.MarketsEntryComponent
import com.tangem.features.markets.entry.impl.MarketsEntryChildFactory.Child
import com.tangem.features.markets.entry.impl.ui.EntryBottomSheetContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val marketsEntryChildFactory: MarketsEntryChildFactory,
) : MarketsEntryComponent, AppComponentContext by context {

    private val stackNavigation = StackNavigation<Child>()

    private val innerRouter = InnerRouter<Child>(
        stackNavigation = stackNavigation,
        popCallback = { onChildBack() },
    )

    private val stack: Value<ChildStack<Child, Any>> = childStack(
        key = "main",
        source = stackNavigation,
        serializer = Child.serializer(),
        initialConfiguration = Child.TokenList,
        handleBackButton = false,
        childFactory = { configuration, factoryContext ->
            marketsEntryChildFactory.createChild(
                child = configuration,
                appComponentContext = childByContext(
                    componentContext = factoryContext,
                    router = innerRouter,
                ),
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
    }

    private fun onChildBack() {
        if (stack.value.active.configuration !is Child.TokenList) {
            stackNavigation.popWhile { it != Child.TokenList }
        }
    }

    @AssistedFactory
    interface Factory : MarketsEntryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultMarketsEntryComponent
    }
}