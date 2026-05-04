package com.tangem.features.feed.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.FaultyDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.components.FeedEntryChildFactory

private const val FEED_ENTRY_SLIDE_DURATION_MS = 300

internal typealias FeedEntryActiveChild =
    Child.Created<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

internal typealias FeedEntryChildStack =
    ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

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
            fade()
        } else {
            slide()
        }
    }

internal fun topBarFeedEntryAnimatedContentTransitionSpec(
    stackState: State<FeedEntryChildStack>,
): AnimatedContentTransitionScope<FeedEntryActiveChild>.() -> ContentTransform =
    { feedEntryAnimatedContentTransform(stackState.value) }

private fun AnimatedContentTransitionScope<FeedEntryActiveChild>.feedEntryAnimatedContentTransform(
    stack: FeedEntryChildStack,
): ContentTransform {
    val shouldUseFade = initialState.configuration.usesFadeStackTransition() ||
        targetState.configuration.usesFadeStackTransition()
    return if (shouldUseFade) {
        // No transition: fade/slide both fight with haze during the animation frame window.
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        feedEntrySlideTransform(stack)
    }
}

private fun AnimatedContentTransitionScope<FeedEntryActiveChild>.feedEntrySlideTransform(
    stack: FeedEntryChildStack,
): ContentTransform {
    val isPushing = stack.backStack.lastOrNull()?.configuration == initialState.configuration
    return if (isPushing) {
        slideInHorizontally(
            animationSpec = tween(FEED_ENTRY_SLIDE_DURATION_MS),
            initialOffsetX = { it },
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(FEED_ENTRY_SLIDE_DURATION_MS),
            targetOffsetX = { -it },
        )
    } else {
        slideInHorizontally(
            animationSpec = tween(FEED_ENTRY_SLIDE_DURATION_MS),
            initialOffsetX = { -it },
        ) togetherWith slideOutHorizontally(
            animationSpec = tween(FEED_ENTRY_SLIDE_DURATION_MS),
            targetOffsetX = { it },
        )
    }
}

private fun FeedEntryChildFactory.Child?.usesFadeStackTransition(): Boolean = when (this) {
    is FeedEntryChildFactory.Child.Search -> true
    is FeedEntryChildFactory.Child.TokenList -> params.shouldAlwaysShowSearchBar
    else -> false
}