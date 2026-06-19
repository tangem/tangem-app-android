package com.tangem.features.feed.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.stack.animation.*
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.features.feed.components.FeedEntryChildFactory
import kotlin.math.abs

private const val FEED_ENTRY_SLIDE_DURATION_MS = 300
private const val FEED_ENTRY_FADE_DURATION_MS = 300

internal typealias FeedEntryActiveChild =
    Child.Created<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

internal typealias FeedEntryChildStack =
    ChildStack<FeedEntryChildFactory.Child, ComposableModularBottomSheetContentComponent>

// Single-child `stackAnimation` overload (-> SimpleStackAnimation), like the root content. Avoids the
// 3-arg overload, which is @FaultyDecomposeApi and backed by a movableContentOf impl known to misbehave on
// rapid transition interruptions (the "stuck content" bug). The selector sees only one child, so the fade vs
// slide choice is per-screen: Search fades, everything else slides.
internal fun contentFeedEntryStackAnimation(): StackAnimation<
    FeedEntryChildFactory.Child,
    ComposableModularBottomSheetContentComponent,
    > =
    stackAnimation { child ->
        if (child.configuration.usesFadeStackTransition()) {
            feedContentFade()
        } else {
            slide()
        }
    }

private fun feedContentFade(): StackAnimator = stackAnimator(
    animationSpec = tween(FEED_ENTRY_FADE_DURATION_MS),
) { factor, _, content ->
    content(
        Modifier.graphicsLayer {
            alpha = 1f - abs(factor)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    )
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