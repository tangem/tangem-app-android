package com.tangem.features.feed.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.components.FeedEntryChildFactory

private fun FeedEntryChildFactory.Child?.usesFadeStackTransition(): Boolean = when (this) {
    is FeedEntryChildFactory.Child.Search -> true
    is FeedEntryChildFactory.Child.TokenList -> params.shouldAlwaysShowSearchBar
    else -> false
}

internal typealias FeedEntryActiveChild =
    Child.Created<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

internal typealias FeedEntryChildStack =
    ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

internal fun topBarFeedEntryAnimatedContentTransitionSpec(
    stackState: State<FeedEntryChildStack>,
): AnimatedContentTransitionScope<FeedEntryActiveChild>.() -> ContentTransform =
    { feedEntryAnimatedContentTransform(stackState.value) }

internal fun contentFeedEntryAnimatedContentTransitionSpec(
    stackState: State<FeedEntryChildStack>,
): AnimatedContentTransitionScope<FeedEntryActiveChild>.() -> ContentTransform =
    { feedEntryAnimatedContentTransform(stackState.value) }

private fun AnimatedContentTransitionScope<FeedEntryActiveChild>.feedEntryAnimatedContentTransform(
    stack: FeedEntryChildStack,
): ContentTransform {
    val shouldUseFade = initialState.configuration.usesFadeStackTransition() ||
        targetState.configuration.usesFadeStackTransition()
    return if (shouldUseFade) {
        fadeIn(animationSpec = tween(FEED_ENTRY_FADE_DURATION_MS)) togetherWith
            fadeOut(animationSpec = tween(FEED_ENTRY_FADE_DURATION_MS))
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

private const val FEED_ENTRY_FADE_DURATION_MS = 300
private const val FEED_ENTRY_SLIDE_DURATION_MS = 300