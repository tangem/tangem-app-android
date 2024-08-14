package com.tangem.features.markets

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.*
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.component.MarketsEntryComponent
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.api.toSerializable
import com.tangem.features.markets.tokenlist.api.MarketsTokenListComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsEntryComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    private val marketsEntryChildFactory: MarketsEntryChildFactory,
) : MarketsEntryComponent, AppComponentContext by context {

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
                onDetailsBack = ::onDetailsBack,
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
        val primary = TangemTheme.colors.background.primary
        val secondary = TangemTheme.colors.background.secondary
        val backgroundColor = remember { Animatable(primary) }
        val stackState = stack.subscribeAsState()

        LocalMainBottomSheetColor.current.value = backgroundColor.value

        Children(
            stack = stackState.value,
            animation = stackAnimation(slide()),
        ) {
            when (it.configuration) {
                is MarketsEntryChildFactory.Child.TokenDetails -> {
                    (it.instance as MarketsTokenDetailsComponent).BottomSheetContent(
                        bottomSheetState = bottomSheetState,
                        onHeaderSizeChange = onHeaderSizeChange,
                        modifier = modifier,
                    )
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

        // order of LaunchedEffects is important here

        val activeChild = stackState.value.active.configuration

        LaunchedEffect(activeChild) {
            when (activeChild) {
                is MarketsEntryChildFactory.Child.TokenDetails -> {
                    backgroundColor.animateTo(
                        secondary,
                        animationSpec = tween(durationMillis = 500),
                    )
                }
                MarketsEntryChildFactory.Child.TokenList -> {
                    backgroundColor.animateTo(
                        primary,
                        animationSpec = tween(durationMillis = 500),
                    )
                }
            }
        }

        LaunchedEffect(bottomSheetState.value) {
            if (activeChild is MarketsEntryChildFactory.Child.TokenDetails) {
                when (bottomSheetState.value) {
                    BottomSheetState.EXPANDED -> {
                        backgroundColor.animateTo(
                            secondary,
                            animationSpec = tween(durationMillis = 100),
                        )
                    }
                    BottomSheetState.COLLAPSED -> {
                        backgroundColor.animateTo(
                            primary,
                            animationSpec = tween(durationMillis = 100),
                        )
                    }
                }
            }
        }

        LaunchedEffect(primary, secondary) {
            if (backgroundColor.isRunning) return@LaunchedEffect

            when (activeChild) {
                is MarketsEntryChildFactory.Child.TokenDetails -> {
                    backgroundColor.snapTo(secondary)
                }
                MarketsEntryChildFactory.Child.TokenList -> {
                    backgroundColor.snapTo(primary)
                }
            }
        }
    }

    @OptIn(ExperimentalDecomposeApi::class)
    private fun marketsListTokenSelected(token: TokenMarket, appCurrency: AppCurrency) {
        stackNavigation.pushNew(
            configuration = MarketsEntryChildFactory.Child.TokenDetails(
                params = MarketsTokenDetailsComponent.Params(
                    token = token.toSerializable(),
                    appCurrency = appCurrency,
                ),
            ),
        )
    }

    private fun onDetailsBack() {
        stackNavigation.popWhile { it != MarketsEntryChildFactory.Child.TokenList }
    }

    @AssistedFactory
    interface Factory : MarketsEntryComponent.Factory {
        override fun create(context: AppComponentContext): DefaultMarketsEntryComponent
    }
}
