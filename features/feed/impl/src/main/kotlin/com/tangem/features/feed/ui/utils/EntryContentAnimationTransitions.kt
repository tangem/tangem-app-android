package com.tangem.features.feed.ui.utils

import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.FaultyDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.utils.toPx
import com.tangem.features.feed.components.FeedEntryChildFactory

private const val DELAY_FOR_TRANSITION = 400

@OptIn(FaultyDecomposeApi::class)
internal fun topBarFeedEntryStackAnimation(): StackAnimation<
    FeedEntryChildFactory.Child,
    ComposableModularBottomSheetContentComponent,
    > =
    stackAnimation { to, from, _ ->
        val isSearchToTokenList =
            (to.configuration as? FeedEntryChildFactory.Child.TokenList)?.params?.shouldAlwaysShowSearchBar == true
        val isFromSearchTokenList =
            (from.configuration as? FeedEntryChildFactory.Child.TokenList)?.params?.shouldAlwaysShowSearchBar == true
        if (isSearchToTokenList || isFromSearchTokenList) {
            fade()
        } else {
            slide()
        }
    }

@OptIn(FaultyDecomposeApi::class)
internal fun contentFeedEntryStackAnimation(): StackAnimation<
    FeedEntryChildFactory.Child,
    ComposableModularBottomSheetContentComponent,
    > =
    stackAnimation { to, from, _ ->
        val isSearchToTokenList =
            (to.configuration as? FeedEntryChildFactory.Child.TokenList)?.params?.shouldAlwaysShowSearchBar == true
        val isFromSearchTokenList =
            (from.configuration as? FeedEntryChildFactory.Child.TokenList)?.params?.shouldAlwaysShowSearchBar == true
        if (isSearchToTokenList || isFromSearchTokenList) {
            fadeAndSlideInt()
        } else {
            slide()
        }
    }

private fun fadeAndSlideInt(): StackAnimator =
    stackAnimator(animationSpec = tween(DELAY_FOR_TRANSITION)) { factor, _, content ->
        val alpha = 1f - kotlin.math.abs(factor)
        val slidePx = 32.dp.toPx()
        val yOffset = when {
            factor > 0 -> (slidePx * factor).toInt()
            factor < 0 -> (slidePx * factor * -1).toInt()
            else -> 0
        }
        content(
            Modifier
                .alpha(alpha)
                .offsetY(yOffset),
        )
    }

private fun Modifier.offsetY(pixels: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x = 0, y = pixels)
    }
}