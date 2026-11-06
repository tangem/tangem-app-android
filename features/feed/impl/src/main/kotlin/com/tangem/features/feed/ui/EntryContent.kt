package com.tangem.features.feed.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.components.FeedEntryChildFactory
import com.tangem.features.feed.ui.utils.FadeConstants.BASE_FADE_LEVEL
import com.tangem.features.feed.ui.utils.contentFeedEntryStackAnimation
import com.tangem.features.feed.ui.utils.topBarFeedEntryAnimatedContentTransitionSpec
import dev.chrisbanes.haze.rememberHazeState

/**

 * When the value is `null` (default), the fade covers `topBarHeight`. A screen with sticky chrome
 * (e.g. category chips, sort options) can set this to `0.dp` to disable the centralized fade and
 * apply its own fade on its inner haze source covering the full sticky header (topbar + chrome).
 */
internal val LocalContentTopFadeHeightOverride = compositionLocalOf<MutableState<Dp?>?> { null }

@Composable
internal fun EntryContent(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
) {
    val background = LocalMainBottomSheetColor.current.value
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val hazeState = rememberHazeState()
    val fadeHeightOverride = remember { mutableStateOf<Dp?>(null) }
    val effectiveTopBarHeight = topBarHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveFadeHeight = fadeHeightOverride.value ?: effectiveTopBarHeight

    Surface(color = background, contentColor = background) {
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalContentTopFadeHeightOverride provides fadeHeightOverride,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ContentBlock(
                    bottomSheetState = bottomSheetState,
                    effectiveFadeHeight = effectiveFadeHeight,
                    stackState = stackState,
                    topBarHeight = effectiveTopBarHeight,
                )
                TitleBlock(
                    bottomSheetState = bottomSheetState,
                    stackState = stackState,
                    onTopBarHeightChange = { topBarHeight = it },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TitleBlock(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onTopBarHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val animationAppBar = remember(stackState) { topBarFeedEntryAnimatedContentTransitionSpec(stackState) }

    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .statusBarsPadding()
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.height > 0) {
                    with(density) { onTopBarHeightChange(coordinates.size.height.toDp()) }
                }
            },
    ) {
        AnimatedContent(
            targetState = stackState.value.active,
            transitionSpec = animationAppBar,
            contentKey = { it.key },
            label = "FeedEntryAppBar",
        ) { state ->
            state.instance.Title(bottomSheetState)
        }
    }
}

@Composable
private fun BoxScope.ContentBlock(
    bottomSheetState: State<BottomSheetState>,
    effectiveFadeHeight: Dp,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    topBarHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val animationContent = remember { contentFeedEntryStackAnimation() }

    Children(
        modifier = modifier.fillMaxSize(),
        stack = stackState.value,
        animation = animationContent,
    ) { child ->
        child.instance.Content(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem(zIndex = 0f, state = LocalHazeState.current)
                .topFade(
                    height = effectiveFadeHeight,
                    color = TangemTheme.colors3.bg.primary.copy(BASE_FADE_LEVEL),
                    solidStop = FEED_TOP_FADE_SOLID_STOP,
                ),
            contentPadding = PaddingValues(top = topBarHeight),
            bottomSheetState = bottomSheetState,
        )
    }
}