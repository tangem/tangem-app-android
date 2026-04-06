package com.tangem.features.feed.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.feed.components.FeedEntryChildFactory
import com.tangem.features.feed.ui.utils.contentFeedEntryStackAnimation
import com.tangem.features.feed.ui.utils.topBarFeedEntryAnimatedContentTransitionSpec
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalDecomposeApi::class)
@Composable
internal fun EntryContent(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
    isOpenedInBottomSheet: Boolean,
) {
    if (LocalRedesignEnabled.current) {
        EntryContentV2(
            bottomSheetState = bottomSheetState,
            stackState = stackState,
            onHeaderSizeChange = onHeaderSizeChange,
            isOpenedInBottomSheet = isOpenedInBottomSheet,
        )
    } else {
        EntryContentV1(
            bottomSheetState = bottomSheetState,
            stackState = stackState,
            onHeaderSizeChange = onHeaderSizeChange,
            isOpenedInBottomSheet = isOpenedInBottomSheet,
        )
    }
}

@Composable
private fun EntryContentV1(
    bottomSheetState: State<BottomSheetState>,
    stackState: State<ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>>,
    onHeaderSizeChange: (Dp) -> Unit,
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
                Children(
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
                    stack = stackState.value,
                    animation = stackAnimation,
                ) { child ->
                    child.instance.Title(bottomSheetState)
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
    isOpenedInBottomSheet: Boolean,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    val animationContent = remember { contentFeedEntryStackAnimation() }
    val animationAppBar = remember(stackState) { topBarFeedEntryAnimatedContentTransitionSpec(stackState) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val hazeState = rememberHazeState()

    Surface(contentColor = background) {
        CompositionLocalProvider(LocalHazeState provides hazeState) {
            Box(modifier = Modifier.fillMaxSize()) {
                Children(
                    modifier = Modifier.fillMaxSize(),
                    stack = stackState.value,
                    animation = animationContent,
                ) { child ->
                    child.instance.Content(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSourceTangem(zIndex = 0f, state = hazeState),
                        contentPadding = PaddingValues(top = topBarHeight),
                        bottomSheetState = bottomSheetState,
                    )
                }
                AnimatedContent(
                    modifier = Modifier
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
                                    topBarHeight = height
                                    onHeaderSizeChange(height)
                                }
                            }
                        },
                    targetState = stackState.value.active,
                    transitionSpec = animationAppBar,
                    contentKey = { it.key },
                    label = "FeedEntryAppBar",
                ) { state ->
                    state.instance.Title(bottomSheetState)
                }
            }
        }
    }
}