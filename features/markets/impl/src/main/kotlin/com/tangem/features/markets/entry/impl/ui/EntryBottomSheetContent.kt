package com.tangem.features.markets.entry.impl.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.entry.impl.MarketsEntryChildFactory
import com.tangem.features.markets.tokenlist.MarketsTokenListComponent

@Composable
internal fun EntryBottomSheetContent(
    bottomSheetState: State<BottomSheetState>,
    onHeaderSizeChange: (Dp) -> Unit,
    stackState: State<ChildStack<MarketsEntryChildFactory.Child, Any>>,
    modifier: Modifier = Modifier,
) {
    val primary = TangemTheme.colors.background.primary
    val backgroundColor = remember { Animatable(primary) }

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
            is MarketsEntryChildFactory.Child.TokenList -> {
                (it.instance as MarketsTokenListComponent).BottomSheetContent(
                    bottomSheetState = bottomSheetState,
                    onHeaderSizeChange = onHeaderSizeChange,
                    modifier = modifier,
                )
            }
        }
    }

    val activeChild = stackState.value.active.configuration

    BackgroundColorEffects(
        activeChild = activeChild,
        backgroundColor = backgroundColor,
        bottomSheetState = bottomSheetState,
    )
}

@Composable
private fun BackgroundColorEffects(
    activeChild: MarketsEntryChildFactory.Child,
    backgroundColor: Animatable<Color, AnimationVector4D>,
    bottomSheetState: State<BottomSheetState>,
) {
    val primary = TangemTheme.colors.background.primary
    val tertiary = TangemTheme.colors.background.tertiary

    // Order of LaunchedEffects is important here

    LaunchedEffect(activeChild) {
        when (activeChild) {
            is MarketsEntryChildFactory.Child.TokenDetails -> {
                backgroundColor.animateTo(
                    tertiary,
                    animationSpec = tween(durationMillis = 500),
                )
            }
            is MarketsEntryChildFactory.Child.TokenList -> {
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
                        tertiary,
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

    LaunchedEffect(primary, tertiary) {
        if (backgroundColor.isRunning) return@LaunchedEffect

        when (activeChild) {
            is MarketsEntryChildFactory.Child.TokenDetails -> {
                backgroundColor.snapTo(tertiary)
            }
            is MarketsEntryChildFactory.Child.TokenList -> {
                backgroundColor.snapTo(primary)
            }
        }
    }
}