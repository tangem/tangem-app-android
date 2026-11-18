@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.toast

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_16
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun ToastSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TangemSurface(
        modifier = modifier,
        isMaterial = true,
        materialStyle = TangemSurface.MaterialStyle.Menu,
        shape = RoundedCornerShape(size = 24.dp),
        content = content,
    )
}

@Suppress("LongParameterList")
@Composable
internal fun ToastContent(
    label: TextReference,
    appearance: TangemToast.Appearance,
    icon: ImageVector?,
    actionLabel: TextReference?,
    onActionClick: (() -> Unit)?,
    onCloseClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val iconTokens = appearance.iconTokens(neutralIcon = icon)

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconTokens.icon != null) {
                Icon(
                    imageVector = iconTokens.icon,
                    contentDescription = null,
                    tint = iconTokens.tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = label.resolveReference(),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = onTextLayout,
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X7,
                text = actionLabel,
                onClick = onActionClick,
            )
        }
        if (onCloseClick != null) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X7,
                iconStart = TangemIconUM.Icon(Icons.ic_cross_16),
                contentDescription = stringResourceSafe(R.string.common_close),
                onClick = onCloseClick,
            )
        }
    }
}

@Composable
internal fun AnimatedToast(
    entry: ToastEntry,
    state: TangemToastStackState,
    startCenterY: () -> Float,
    modifier: Modifier = Modifier,
) {
    val animation = remember { ToastAnimation() }

    LaunchedEffect(entry.isVisible) {
        if (entry.isVisible) {
            animation.show()
        } else {
            animation.dismiss()
            state.onExitFinished(entry)
        }
    }

    val message = entry.message

    Layout(
        modifier = modifier,
        content = {
            ToastSurface(modifier = Modifier.graphicsLayer { alpha = animation.toastAlpha.value.coerceIn(0f, 1f) }) {}
            ToastContent(
                label = message.label,
                appearance = message.appearance,
                icon = message.icon,
                actionLabel = message.actionLabel,
                onActionClick = { state.performAction(entry) }.takeIf { message.action != null },
                onCloseClick = { state.close(entry) }.takeIf { message.isClosable },
                modifier = Modifier
                    .onSizeChanged { entry.height = it.height }
                    .graphicsLayer {
                        val progress = animation.contentAlpha.value.coerceIn(0f, 1f)
                        alpha = progress
                        clip = true
                        shape = PillShape(
                            pillSize = with(animation) { pillSize(size.width, size.height) },
                            maxCornerRadius = 24.dp.toPx(),
                        )
                        val blurRadius = (1f - progress) * 32.dp.toPx()
                        renderEffect = if (blurRadius > 0f) {
                            BlurEffect(radiusX = blurRadius, radiusY = blurRadius, edgeTreatment = TileMode.Decal)
                        } else {
                            null
                        }
                    },
                onTextLayout = { entry.isMultiLine = it.lineCount > 1 },
            )
        },
    ) { measurables, constraints ->
        val content = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val pill = with(animation) { pillSize(content.width.toFloat(), content.height.toFloat()) }
        val background = measurables[0].measure(
            Constraints.fixed(width = pill.width.roundToInt(), height = pill.height.roundToInt()),
        )
        val centerY = lerp(startCenterY(), content.height / 2f, animation.reveal.value)

        layout(width = content.width, height = content.height) {
            background.place(
                x = (content.width - background.width) / 2,
                y = (centerY - background.height / 2f).roundToInt(),
            )
            content.place(x = 0, y = (centerY - content.height / 2f).roundToInt())
        }
    }
}

private class ToastAnimation {

    val reveal = Animatable(0f)
    val shrink = Animatable(0f)
    val toastAlpha = Animatable(0f)
    val contentAlpha = Animatable(0f)

    suspend fun show() = coroutineScope<Unit> {
        launch {
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = ToastShowEasing),
            )
        }
        launch {
            toastAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 50,
                    easing = ToastShowEasing,
                ),
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 250,
                    delayMillis = 100,
                    easing = ToastShowEasing,
                ),
            )
        }
    }

    suspend fun dismiss() = coroutineScope<Unit> {
        launch {
            shrink.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = ToastShrinkEasing),
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = ToastShowEasing),
            )
        }
        launch {
            toastAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 50,
                    easing = ToastShowEasing,
                ),
            )
        }
    }

    fun Density.pillSize(fullWidth: Float, fullHeight: Float): Size {
        val shownWidth = lerp(start = 120.dp.toPx(), stop = fullWidth, fraction = reveal.value)
        val shownHeight = lerp(start = 32.dp.toPx(), stop = fullHeight, fraction = reveal.value)
        val endSize = 24.dp.toPx()
        return Size(
            width = lerp(shownWidth, endSize, shrink.value).coerceAtLeast(0f),
            height = lerp(shownHeight, endSize, shrink.value).coerceAtLeast(0f),
        )
    }
}

private class PillShape(private val pillSize: Size, private val maxCornerRadius: Float) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val left = (size.width - pillSize.width) / 2f
        val top = (size.height - pillSize.height) / 2f
        val radius = min(maxCornerRadius, pillSize.height / 2f)
        return Outline.Rounded(
            RoundRect(
                left = left,
                top = top,
                right = left + pillSize.width,
                bottom = top + pillSize.height,
                cornerRadius = CornerRadius(radius),
            ),
        )
    }
}