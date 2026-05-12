package com.tangem.features.feed.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.topFade
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
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

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun EntryContent(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
    onExpandSheet: () -> Unit,
    isOpenedInBottomSheet: Boolean,
) {
    if (LocalRedesignEnabled.current) {
        EntryContentV2(
            bottomSheetState = bottomSheetState,
            stackState = stackState,
            onHeaderSizeChange = onHeaderSizeChange,
            onExpandSheet = onExpandSheet,
            isOpenedInBottomSheet = isOpenedInBottomSheet,
        )
    } else {
        EntryContentV1(
            bottomSheetState = bottomSheetState,
            stackState = stackState,
            onHeaderSizeChange = onHeaderSizeChange,
            onExpandSheet = onExpandSheet,
            isOpenedInBottomSheet = isOpenedInBottomSheet,
        )
    }
}

@Composable
private fun EntryContentV1(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
    onExpandSheet: () -> Unit,
    isOpenedInBottomSheet: Boolean,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    val stackAnimation = remember { contentFeedEntryStackAnimation() }

    Surface(contentColor = background) {
        Scaffold(
            containerColor = background,
            contentWindowInsets = WindowInsetsZero,
            topBar = {
                Box(
                    modifier = Modifier
                        .then(
                            if (!isOpenedInBottomSheet) {
                                Modifier.statusBarsPadding()
                            } else {
                                Modifier
                            },
                        )
                        .onGloballyPositioned { coordinates ->
                            if (coordinates.size.height > 0) {
                                with(density) {
                                    onHeaderSizeChange(coordinates.size.height.toDp())
                                }
                            }
                        },
                ) {
                    Children(
                        stack = stackState.value,
                        animation = stackAnimation,
                    ) { child ->
                        child.instance.Title(bottomSheetState)
                    }
                    CollapsedTitleClickOverlay(
                        bottomSheetState = bottomSheetState,
                        onExpandSheet = onExpandSheet,
                    )
                }
            },
            content = { contentPadding ->
                Children(
                    stack = stackState.value,
                    animation = stackAnimation,
                ) { child ->
                    child.instance.Content(
                        modifier = Modifier.padding(contentPadding),
                        bottomSheetState = bottomSheetState,
                        contentPadding = PaddingValues(),
                    )
                }
            },
        )
    }
}

@Composable
private fun EntryContentV2(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
    onExpandSheet: () -> Unit,
    isOpenedInBottomSheet: Boolean,
) {
    val background = LocalMainBottomSheetColor.current.value

    var topBarHeight by remember { mutableStateOf(0.dp) }
    val hazeState = rememberHazeState()
    val fadeHeightOverride = remember { mutableStateOf<Dp?>(null) }
    val effectiveFadeHeight = fadeHeightOverride.value ?: topBarHeight

    Surface(contentColor = background) {
        CompositionLocalProvider(
            LocalHazeState provides hazeState,
            LocalContentTopFadeHeightOverride provides fadeHeightOverride,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ContentBlock(
                    bottomSheetState = bottomSheetState,
                    effectiveFadeHeight = effectiveFadeHeight,
                    isOpenedInBottomSheet = isOpenedInBottomSheet,
                    stackState = stackState,
                    topBarHeight = topBarHeight,
                )
                TitleBlock(
                    bottomSheetState = bottomSheetState,
                    stackState = stackState,
                    onTopBarHeightChang = { dp ->
                        onHeaderSizeChange(dp)
                        topBarHeight = dp
                    },
                    isOpenedInBottomSheet = isOpenedInBottomSheet,
                    onExpandSheet = onExpandSheet,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.TitleBlock(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onTopBarHeightChang: (Dp) -> Unit,
    isOpenedInBottomSheet: Boolean,
    onExpandSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val animationAppBar = remember(stackState) { topBarFeedEntryAnimatedContentTransitionSpec(stackState) }

    Box(
        modifier = modifier
            .align(Alignment.TopStart)
            .then(
                if (!isOpenedInBottomSheet) {
                    Modifier.statusBarsPadding()
                } else {
                    Modifier
                },
            )
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.height > 0) {
                    with(density) {
                        val height = coordinates.size.height.toDp()
                        onTopBarHeightChang(height)
                    }
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
        CollapsedTitleClickOverlay(
            bottomSheetState = bottomSheetState,
            onExpandSheet = onExpandSheet,
        )
    }
}

@Composable
private fun BoxScope.ContentBlock(
    bottomSheetState: State<BottomSheetState>,
    effectiveFadeHeight: Dp,
    isOpenedInBottomSheet: Boolean,
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
                .conditionalCompose(
                    condition = !isOpenedInBottomSheet,
                    modifier = {
                        padding(top = topBarHeight)
                    },
                )
                .hazeSourceTangem(zIndex = 0f, state = LocalHazeState.current)
                .conditionalCompose(
                    condition = isOpenedInBottomSheet,
                    modifier = {
                        topFade(
                            height = effectiveFadeHeight,
                            color = TangemTheme.colors2.surface.level2.copy(BASE_FADE_LEVEL),
                            solidStop = .6f,
                        )
                    },
                ),
            contentPadding = PaddingValues(
                top = if (isOpenedInBottomSheet) topBarHeight else TangemTheme.dimens2.x2_5,
            ),
            bottomSheetState = bottomSheetState,
        )
    }
}

@Composable
private fun BoxScope.CollapsedTitleClickOverlay(bottomSheetState: State<BottomSheetState>, onExpandSheet: () -> Unit) {
    if (bottomSheetState.value == BottomSheetState.COLLAPSED) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onExpandSheet,
                )
                .clearAndSetSemantics {},
        )
    }
}