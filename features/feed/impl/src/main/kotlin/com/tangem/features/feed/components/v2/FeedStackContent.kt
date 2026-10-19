package com.tangem.features.feed.components.v2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent

/**
 * Renders the active screen of the feed stack and animates between screens.
 *
 * Compose's [AnimatedContent] instead of Decompose's stack animation: typing into and clearing the
 * search bar pushes and pops the search screen faster than a transition lasts. Decompose's per-pair
 * animator (`@FaultyDecomposeApi`) then gets stuck on a stale screen behind its input-blocking overlay,
 * and its per-screen one can't fade the search in over the home while other screens slide.
 * [AnimatedContent] retargets mid-animation and always settles on the latest screen.
 */
@Composable
internal fun FeedStackContent(
    stack: ChildStack<FeedRoute, FeedScreenComponent>,
    modifier: Modifier = Modifier,
    content: @Composable (FeedScreenComponent) -> Unit,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    saveableStateHolder.RetainStates(stack)

    val transition = updateTransition(
        targetState = FeedStackTarget(active = stack.active, depth = stack.items.size),
        label = "feedStack",
    )

    Box(modifier = modifier) {
        transition.AnimatedContent(
            contentKey = { it.active.configuration },
            transitionSpec = { feedScreenTransition() },
        ) { target ->
            saveableStateHolder.SaveableStateProvider(key = target.active.configuration.stateKey()) {
                content(target.active.instance)
            }
        }

        // as Decompose does: a tap on a screen that is on its way out must not navigate again
        if (transition.isRunning) {
            Box(modifier = Modifier.matchParentSize().consumeAllPointerInput())
        }
    }
}

private data class FeedStackTarget(
    val active: Child.Created<FeedRoute, FeedScreenComponent>,
    val depth: Int,
)

private fun AnimatedContentTransitionScope<FeedStackTarget>.feedScreenTransition(): ContentTransform {
    val routes = listOf(initialState.active.configuration, targetState.active.configuration)
    val isSearchToggle = routes.any { it is FeedRoute.Search } && routes.any { it is FeedHomeRoute }
    val isForward = targetState.depth >= initialState.depth

    val transform = when {
        isSearchToggle -> fadeIn(tween()) togetherWith fadeOut(tween())
        isForward -> slideInHorizontally(tween()) { it } togetherWith slideOutHorizontally(tween()) { -it }
        else -> slideInHorizontally(tween()) { -it } togetherWith slideOutHorizontally(tween()) { it }
    }

    return transform using null
}

/** Drops the saved state of screens that left the stack, so a screen pushed again starts clean. */
@Composable
private fun SaveableStateHolder.RetainStates(stack: ChildStack<FeedRoute, *>) {
    val keys = stack.items.mapTo(HashSet()) { it.configuration.stateKey() }
    val retainedKeys = remember(this) { HashSet(keys) }

    DisposableEffect(this, keys) {
        retainedKeys.filterNot(keys::contains).forEach(::removeState)
        retainedKeys.clear()
        retainedKeys.addAll(keys)

        onDispose {}
    }
}

private fun FeedRoute.stateKey(): String = "${javaClass.name}_${hashCode()}"

private fun Modifier.consumeAllPointerInput(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}