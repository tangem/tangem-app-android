/*
 * Copied from Decompose 3.5.0 (extensions-compose-experimental), commit d36078df71276275adc3af0ac993b575b545f932
 * https://github.com/arkivanov/Decompose
 *

 * Licensed under the Apache License, Version 2.0: https://www.apache.org/licenses/LICENSE-2.0
 */

package com.tangem.core.decompose.ui.stack.animation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.tangem.core.decompose.ui.stack.WithStackAnimationScope
import com.tangem.core.decompose.ui.stack.awaitAll
import com.tangem.core.decompose.ui.stack.dropLast
import com.tangem.core.decompose.ui.stack.size
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.isExit
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackAnimatable
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@ExperimentalDecomposeApi
internal class DefaultStackAnimation<C : Any, T : Any>(
    private val isInputDisabledDuringAnimation: Boolean,
    private val predictiveBackParams: (ChildStack<C, T>) -> PredictiveBackParams?,
    private val selector: (
        child: Child.Created<C, T>,
        otherChild: Child.Created<C, T>,
        direction: Direction,
        isPredictiveBack: Boolean,
    ) -> StackAnimator?,
) : StackAnimation<C, T> {

    @Suppress("CyclomaticComplexMethod")
    @Composable
    override operator fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable StackAnimationScope.(child: Child.Created<C, T>) -> Unit,
    ) {
        var currentStack by remember { mutableStateOf(stack) }
        var items by remember { mutableStateOf(getAnimationItems(newStack = currentStack)) }
        var nextItems: Map<Any, AnimationItem<C, T>>? by remember { mutableStateOf(null) }
        val stackKeys = remember(stack) { stack.items.map { it.key } }
        val currentStackKeys = remember(currentStack) { currentStack.items.map { it.key } }

        if (stack != currentStack) {
            val oldStack = currentStack
            currentStack = stack

            val shouldUpdateItems =
                when {
                    stack.active.key == oldStack.active.key ->
                        items.keys.singleOrNull() != stack.active.key ||
                            items.values.singleOrNull()?.child?.instance != stack.active.instance

                    items.size == 1 -> items.keys.single() != stack.active.key
                    else -> items.keys.toList() != stackKeys
                }

            if (shouldUpdateItems) {
                val newItems = getAnimationItems(newStack = currentStack, oldStack = oldStack)
                if (items.size == 1 || items.values.last().transitionState.isSeekable()) {
                    items = newItems
                } else {
                    nextItems = newItems
                }
            }
        }

        Box(modifier = modifier) {
            items.forEach { (key, item) ->
                key(key) {
                    Child(
                        item = item,
                        onFinish = {
                            if (item.direction.isExit) {
                                items -= key
                            } else {
                                items += key to item.copy(animator = null)
                            }
                        },
                        content = content,
                    )

                    if (item.direction.isExit) {
                        DisposableEffect(Unit) {
                            onDispose {
                                nextItems?.also { items = it }
                                nextItems = null
                            }
                        }
                    }
                }
            }

            // A workaround until https://issuetracker.google.com/issues/214231672.
            // Normally only the exiting child should be disabled.
            if (isInputDisabledDuringAnimation && (items.size > 1 || nextItems != null)) {
                Overlay(modifier = Modifier.matchParentSize())
            }
        }

        if (currentStack.backStack.isNotEmpty()) {
            val predictiveBackParams = remember(currentStackKeys) { predictiveBackParams(currentStack) }
            if (predictiveBackParams != null) {
                key(currentStackKeys) {
                    PredictiveBackController(
                        stack = currentStack,
                        predictiveBackParams = predictiveBackParams,
                        isRegularTransitionRunning = { nextItems != null || items.isRegularTransition() },
                        onItemsChange = { items = it },
                    )
                }
            }
        }
    }

    @Composable
    private fun Child(
        item: AnimationItem<C, T>,
        onFinish: () -> Unit,
        content: @Composable StackAnimationScope.(child: Child.Created<C, T>) -> Unit,
    ) {
        val transition = rememberTransition(item.transitionState)
        item.transitionComposed?.let { transitionComposed ->
            SideEffect { transitionComposed.complete(Unit) }
        }
        val isTransitionIdle = item.transitionState.isIdle()

        if (isTransitionIdle) {
            LaunchedEffect(Unit) {
                onFinish()
            }
        }

        WithStackAnimationScope(item.direction.takeUnless { isTransitionIdle }, transition) {
            Box(modifier = item.animator?.run { animate(item.direction) } ?: Modifier) {
                content(item.child)
            }
        }
    }

    private fun getAnimationItems(
        newStack: ChildStack<C, T>,
        oldStack: ChildStack<C, T>? = null,
    ): Map<Any, AnimationItem<C, T>> = when {
        oldStack == null || newStack.active.key == oldStack.active.key ->
            keyedItemsOf(
                AnimationItem(
                    child = newStack.active,
                    direction = Direction.ENTER_FRONT,
                    transitionState = MutableTransitionState(EnterExitState.Visible),
                ),
            )

        newStack.size < oldStack.size && oldStack.backStack.any { it.key == newStack.active.key } ->
            keyedItemsOf(
                AnimationItem(
                    child = newStack.active,
                    direction = Direction.ENTER_BACK,
                    transitionState = EnterExitState.PreEnter transitionTo EnterExitState.Visible,
                    otherChild = oldStack.active,
                ),
                AnimationItem(
                    child = oldStack.active,
                    direction = Direction.EXIT_FRONT,
                    transitionState = EnterExitState.Visible transitionTo EnterExitState.PostExit,
                    otherChild = newStack.active,
                ),
            )

        else ->
            keyedItemsOf(
                AnimationItem(
                    child = oldStack.active,
                    direction = Direction.EXIT_BACK,
                    transitionState = EnterExitState.Visible transitionTo EnterExitState.PostExit,
                    otherChild = newStack.active,
                ),
                AnimationItem(
                    child = newStack.active,
                    direction = Direction.ENTER_FRONT,
                    transitionState = EnterExitState.PreEnter transitionTo EnterExitState.Visible,
                    otherChild = oldStack.active,
                ),
            )
    }

    @ExperimentalDecomposeApi
    @Composable
    private fun PredictiveBackController(
        stack: ChildStack<C, T>,
        predictiveBackParams: PredictiveBackParams,
        isRegularTransitionRunning: () -> Boolean,
        onItemsChange: (Map<Any, AnimationItem<C, T>>) -> Unit,
    ) {
        val scope = rememberCoroutineScope()

        val callback =
            remember {
                PredictiveBackCallback(
                    stack = stack,
                    scope = scope,
                    predictiveBackParams = predictiveBackParams,
                    isRegularTransitionRunning = isRegularTransitionRunning,
                    setItems = onItemsChange,
                )
            }

        DisposableEffect(predictiveBackParams.backHandler, callback) {
            predictiveBackParams.backHandler.register(callback)

            onDispose {
                scope.cancel() // Ensure the scope is cancelled before unregistering the callback
                predictiveBackParams.backHandler.unregister(callback)
            }
        }
    }

    private fun AnimationItem(
        child: Child.Created<C, T>,
        direction: Direction,
        transitionState: TransitionState<EnterExitState>,
        otherChild: Child.Created<C, T>,
        isPredictiveBack: Boolean = false,
        predictiveBackAnimator: StackAnimator? = null,
        transitionComposed: CompletableDeferred<Unit>? = null,
    ): AnimationItem<C, T> = AnimationItem(
        child = child,
        direction = direction,
        transitionState = transitionState,
        animator = predictiveBackAnimator ?: selector(child, otherChild, direction, isPredictiveBack),
        transitionComposed = transitionComposed,
    )

    private inner class PredictiveBackCallback(
        private val stack: ChildStack<C, T>,
        private val scope: CoroutineScope,
        private val predictiveBackParams: PredictiveBackParams,
        private val isRegularTransitionRunning: () -> Boolean,
        private val setItems: (Map<Any, AnimationItem<C, T>>) -> Unit,
    ) : BackCallback() {
        private var animationHandler: AnimationHandler? = null
        private var initialBackEvent: BackEvent? = null

        /**
         * Set from a committed back until its pop has gone through. The callback stays registered meanwhile —
         * disabling it would hand the next back to whoever is below, possibly the activity — so the gestures that
         * arrive in between are dropped: they would replace the committing handler and pop a second time.
         */
        private var isCommitting = false

        override fun onBackStarted(backEvent: BackEvent) {
            if (isCommitting) return
            // seeking would replace a running push or pop with fresh states, and the screen would jump; the back
            // still pops once the gesture completes, through the regular transition
            initialBackEvent = backEvent.takeUnless { isRegularTransitionRunning() }
        }

        override fun onBackProgressed(backEvent: BackEvent) {
            if (isCommitting) return
            startIfNeeded()

            scope.launch {
                animationHandler?.progress(backEvent)
            }
        }

        private fun startIfNeeded() {
            val backEvent = initialBackEvent ?: return
            initialBackEvent = null

            val animationHandler = AnimationHandler(animatable = predictiveBackParams.animatable(backEvent))
            this.animationHandler = animationHandler
            val exitChild = stack.active
            val enterChild = stack.backStack.last()

            setItems(
                keyedItemsOf(
                    AnimationItem(
                        child = enterChild,
                        direction = Direction.ENTER_BACK,
                        transitionState = animationHandler.enterTransitionState,
                        otherChild = exitChild,
                        isPredictiveBack = true,
                        predictiveBackAnimator = animationHandler.animatable?.let { anim ->
                            SimpleStackAnimator { anim.enterModifier }
                        },
                        transitionComposed = animationHandler.enterTransitionComposed,
                    ),
                    AnimationItem(
                        child = exitChild,
                        direction = Direction.EXIT_FRONT,
                        transitionState = animationHandler.exitTransitionState,
                        otherChild = enterChild,
                        isPredictiveBack = true,
                        predictiveBackAnimator = animationHandler.animatable?.let { anim ->
                            SimpleStackAnimator { anim.exitModifier }
                        },
                        transitionComposed = animationHandler.exitTransitionComposed,
                    ),
                ),
            )

            scope.launch {
                animationHandler.progress(backEvent)
            }
        }

        override fun onBackCancelled() {
            if (isCommitting) return
            initialBackEvent = null
            val handler = animationHandler ?: return

            scope.launch {
                handler.cancel()
                // a new gesture can start while this one settles back, and a back can commit it; either way the
                // handler and items are no longer ours to reset
                if (animationHandler === handler && !isCommitting) {
                    animationHandler = null
                    setItems(getAnimationItems(newStack = stack))
                }
            }
        }

        override fun onBack() {
            if (isCommitting) return
            isCommitting = true
            initialBackEvent = null

            scope.launch {
                animationHandler?.also { handler ->
                    handler.finish()
                    animationHandler = null
                    setItems(getAnimationItems(newStack = stack.dropLast()))
                }

                predictiveBackParams.onBack()

                // a pop re-keys this controller, which disposes it and cancels this scope; only a pop the router
                // declined lives to the next frame, and leaves the stack — and this callback — in place
                withFrameNanos {}
                isCommitting = false
            }
        }
    }

    private class AnimationHandler(
        val animatable: PredictiveBackAnimatable?,
    ) {
        val exitTransitionState: SeekableTransitionState<EnterExitState> =
            SeekableTransitionState(EnterExitState.Visible)
        val enterTransitionState: SeekableTransitionState<EnterExitState> =
            SeekableTransitionState(EnterExitState.PreEnter)

        /*
         * SeekableTransitionState.seekTo/animateTo silently return until rememberTransition has composed the state.
         * With three-button navigation onBackStarted, onBackProgressed and onBack can arrive within one frame,
         * so the completion animation was skipped and the child popped instantly (upstream PR #1057).
         */
        val exitTransitionComposed = CompletableDeferred<Unit>()
        val enterTransitionComposed = CompletableDeferred<Unit>()

        private suspend fun awaitTransitionsComposed() {
            exitTransitionComposed.await()
            enterTransitionComposed.await()
        }

        suspend fun progress(backEvent: BackEvent) {
            animatable?.run {
                animate(backEvent)
                return@progress // Don't animate transition states on back progress if there is PredictiveBackAnimatable
            }

            awaitTransitionsComposed()
            awaitAll(
                { exitTransitionState.seekTo(fraction = backEvent.progress, targetState = EnterExitState.PostExit) },
                { enterTransitionState.seekTo(fraction = backEvent.progress, targetState = EnterExitState.Visible) },
            )
        }

        suspend fun cancel() {
            awaitAll(
                { exitTransitionState.snapTo(EnterExitState.Visible) },
                { enterTransitionState.snapTo(EnterExitState.PreEnter) },
                { animatable?.cancel() },
            )
        }

        suspend fun finish() {
            awaitTransitionsComposed()
            awaitAll(
                { exitTransitionState.animateTo(EnterExitState.PostExit) },
                { enterTransitionState.animateTo(EnterExitState.Visible) },
                { animatable?.finish() },
            )
        }
    }
}

@Composable
private fun Overlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
        },
    )
}

@ExperimentalDecomposeApi
private data class AnimationItem<out C : Any, out T : Any>(
    val child: Child.Created<C, T>,
    val direction: Direction,
    val transitionState: TransitionState<EnterExitState>,
    val animator: StackAnimator? = null,
    val transitionComposed: CompletableDeferred<Unit>? = null,
)

@ExperimentalDecomposeApi
private fun <C : Any, T : Any> keyedItemsOf(vararg items: AnimationItem<C, T>): Map<Any, AnimationItem<C, T>> =
    items.associateBy { it.child.key }

/*
 * Can't be anonymous. See:
 * https://github.com/JetBrains/compose-jb/issues/2688
 * https://github.com/JetBrains/compose-jb/issues/2612
 */
@ExperimentalDecomposeApi
private class SimpleStackAnimator(
    private val modifier: () -> Modifier,
) : StackAnimator {
    @Composable
    override fun StackAnimationScope.animate(direction: Direction): Modifier = modifier()
}

private infix fun <S> S.transitionTo(targetState: S): MutableTransitionState<S> = MutableTransitionState(this).apply {
    this.targetState = targetState
}

private fun TransitionState<*>.isIdle(): Boolean = when (this) {
    is MutableTransitionState -> isIdle
    is SeekableTransitionState -> false
    else -> false
}

/** A push or pop animating on its own, as opposed to a predictive back seeking under the finger. */
@ExperimentalDecomposeApi
private fun Map<Any, AnimationItem<*, *>>.isRegularTransition(): Boolean =
    size > 1 && !values.last().transitionState.isSeekable()

private fun TransitionState<*>.isSeekable(): Boolean = this is SeekableTransitionState