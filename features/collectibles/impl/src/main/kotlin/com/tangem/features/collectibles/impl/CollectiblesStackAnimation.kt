package com.tangem.features.collectibles.impl

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Modal routes slide up over a screen that stays in place, other pairs slide horizontally. Not the two-child
 * `stackAnimation` overload: its `movableContentOf` crashes when IME insets change mid-move.
 */
internal class CollectiblesStackAnimation(initialRoute: CollectiblesRoute) {

    private var previousRoute: CollectiblesRoute = initialRoute
    private var activeRoute: CollectiblesRoute = initialRoute

    private val CollectiblesRoute.isModalFlow: Boolean
        get() = this is CollectiblesRoute.MachineDetails ||
            this is CollectiblesRoute.ConfirmOrder ||
            this is CollectiblesRoute.PackOpenFailed

    val animation: StackAnimation<CollectiblesRoute, ComposableContentComponent> = stackAnimation { child ->
        stackAnimator { factor, direction, content ->
            val axis = remember(direction) { selectSlideAxis(child.configuration) }
            content(Modifier.slideBy(axis = axis, factor = factor))
        }
    }

    /** Must run in the composition that hands `Children` the stack with [route] active, before `Children` is called. */
    fun onActiveRouteChanged(route: CollectiblesRoute) {
        if (route == activeRoute) return
        previousRoute = activeRoute
        activeRoute = route
    }

    private fun selectSlideAxis(route: CollectiblesRoute): Orientation? {
        val otherRoute = if (route == activeRoute) previousRoute else activeRoute
        return when {
            route.isModalFlow == otherRoute.isModalFlow -> Orientation.Horizontal
            route.isModalFlow -> Orientation.Vertical
            else -> null
        }
    }

    private fun Modifier.slideBy(axis: Orientation?, factor: Float): Modifier = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            when (axis) {
                Orientation.Horizontal -> placeable.placeRelative(x = (placeable.width * factor).toInt(), y = 0)
                Orientation.Vertical -> placeable.placeRelative(x = 0, y = (placeable.height * factor).toInt())
                null -> placeable.placeRelative(x = 0, y = 0)
            }
        }
    }
}

@Composable
internal fun rememberCollectiblesStackAnimation(
    activeRoute: CollectiblesRoute,
): StackAnimation<CollectiblesRoute, ComposableContentComponent> {
    val stackAnimation = remember { CollectiblesStackAnimation(initialRoute = activeRoute) }
    stackAnimation.onActiveRouteChanged(activeRoute)
    return stackAnimation.animation
}