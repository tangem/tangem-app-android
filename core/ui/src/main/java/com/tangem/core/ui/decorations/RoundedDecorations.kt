package com.tangem.core.ui.decorations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ProvideRoundedItemGroups(content: @Composable () -> Unit) {
    val groups = remember { RoundedItemGroups() }
    CompositionLocalProvider(LocalRoundedItemGroups provides groups, content = content)
}

@Composable
fun Modifier.roundedShapeItemDecoration(
    currentIndex: Int,
    lastIndex: Int,
    addDefaultPadding: Boolean = true,
    radius: Dp = TangemTheme.dimens.radius16,
    backgroundColor: Color? = null,
    groupKey: Any? = null,
): Modifier = composed {
    val modifier = if (addDefaultPadding) this.padding(horizontal = TangemTheme.dimens.spacing16) else this

    val applyTopPadding: @Composable Modifier.() -> Modifier = {
        if (addDefaultPadding && currentIndex == 0) {
            padding(top = TangemTheme.dimens.spacing12)
        } else {
            this
        }
    }

    val group = groupKey?.let { LocalRoundedItemGroups.current?.get(it) }
    if (group != null) {
        return@composed modifier
            .applyTopPadding()
            .groupSlice(
                group = group,
                currentIndex = currentIndex,
                lastIndex = lastIndex,
                radius = radius,
                backgroundColor = backgroundColor,
            )
    }

    val applyShape: Modifier.(shape: RoundedCornerShape?) -> Modifier = { shape ->
        if (backgroundColor != null) {
            if (shape != null) {
                clip(shape).background(color = backgroundColor, shape = shape)
            } else {
                background(color = backgroundColor)
            }
        } else {
            if (shape != null) {
                clip(shape = shape)
            } else {
                this
            }
        }
    }

    val isSingleItem = currentIndex == 0 && lastIndex == 0
    when {
        isSingleItem -> {
            modifier
                .applyTopPadding()
                .applyShape(RoundedCornerShape(radius))
        }
        currentIndex == 0 -> {
            modifier
                .applyTopPadding()
                .applyShape(RoundedCornerShape(topStart = radius, topEnd = radius))
        }
        currentIndex == lastIndex -> {
            modifier.applyShape(RoundedCornerShape(bottomStart = radius, bottomEnd = radius))
        }
        else -> modifier.applyShape(null)
    }
}

@Composable
private fun Modifier.groupSlice(
    group: RoundedItemGroup,
    currentIndex: Int,
    lastIndex: Int,
    radius: Dp,
    backgroundColor: Color?,
): Modifier {
    val measured = remember { MeasuredHeight() }
    DisposableEffect(group, currentIndex) {
        onDispose { group.heights.remove(currentIndex) }
    }
    // A lazy item that keeps its key but moves to another index keeps its node and its size, so
    // onSizeChanged stays silent while the old index's entry has just been disposed. Re-registering on
    // every composition puts it back under the new index; disposals run before side effects, so two
    // items swapping places cannot erase each other's entries.
    SideEffect {
        if (measured.height >= 0) group.heights[currentIndex] = measured.height
    }

    return this
        .onSizeChanged { size ->
            measured.height = size.height
            group.heights[currentIndex] = size.height
        }
        .graphicsLayer {
            shape = GroupSliceShape(
                above = group.extentAbove(currentIndex),
                below = group.extentBelow(currentIndex, lastIndex),
                radius = radius.toPx(),
            )
            clip = true
        }
        .then(if (backgroundColor != null) Modifier.drawBehind { drawRect(backgroundColor) } else Modifier)
}

private class GroupSliceShape(
    private val above: Float,
    private val below: Float,
    private val radius: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Rounded(
            RoundRect(
                left = 0f,
                top = -above,
                right = size.width,
                bottom = size.height + below,
                cornerRadius = CornerRadius(radius),
            ),
        )
    }
}

private class MeasuredHeight {
    var height: Int = -1
}

private class RoundedItemGroups {
    private val groups = mutableMapOf<Any, RoundedItemGroup>()

    fun get(key: Any): RoundedItemGroup = groups.getOrPut(key) { RoundedItemGroup() }
}

private class RoundedItemGroup {
    val heights = mutableStateMapOf<Int, Int>()

    fun extentAbove(index: Int): Float = sumHeights(0 until index)

    fun extentBelow(index: Int, lastIndex: Int): Float = sumHeights(index + 1..lastIndex)

    private fun sumHeights(indices: IntRange): Float = indices.sumOf { heights[it] ?: 0 }.toFloat()
}

private val LocalRoundedItemGroups = staticCompositionLocalOf<RoundedItemGroups?> { null }