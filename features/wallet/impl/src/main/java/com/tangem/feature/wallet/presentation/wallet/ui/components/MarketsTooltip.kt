package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.core.ui.test.MarketTooltipTestTags
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.impl.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Gap between the lifted tooltip and the obstacle below it. */
private val OBSTACLE_GAP = 4.dp

/**
 * Onboarding tooltip for the markets bottom sheet, anchored right above the sheet peek.
 *
 * @param obstacleBounds provider of root-coordinates bounds of content the tooltip must not cover —
 * e.g. the "Add & Manage" button. When the anchored tooltip would overlap the obstacle, the tooltip
 * is lifted to sit just above it.
 */
@Suppress("LongParameterList")
@Composable
internal fun MarketsTooltip(
    availableHeight: Dp,
    bottomSheetState: TangemSheetState,
    isVisible: Boolean,
    onCloseClick: () -> Unit,
    sheetTopInset: Dp,
    modifier: Modifier = Modifier,
    obstacleBounds: () -> Rect? = { null },
) {
    val density = LocalDensity.current
    var tooltipHeight by remember { mutableIntStateOf(0) }
    val tooltipOffset by remember(availableHeight, sheetTopInset, obstacleBounds) {
        derivedStateOf {
            // Sheet offset is in root coordinates: the scaffold content fills the screen edge to edge
            val sheetTop = runCatching { bottomSheetState.requireOffset() }.getOrElse { 0f }

            with(density) {
                val anchoredBottom = sheetTop + sheetTopInset.toPx()
                val anchoredTop = anchoredBottom - tooltipHeight
                val obstacle = obstacleBounds()?.takeIf { tooltipHeight > 0 }
                val isObstacleInTheWay = obstacle != null &&
                    obstacle.top < anchoredBottom && obstacle.bottom > anchoredTop
                val bottom = if (isObstacleInTheWay) {
                    obstacle.top - OBSTACLE_GAP.toPx()
                } else {
                    anchoredBottom
                }
                (bottom - availableHeight.toPx()).toDp()
            }
        }
    }

    var isVisibleWrapped by remember { mutableStateOf(value = false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(timeMillis = 300)
        }

        isVisibleWrapped = isVisible
    }

    val slideOffset = 40.dp.toPx()
    AnimatedVisibility(
        modifier = modifier
            .offset { IntOffset(x = 0, y = tooltipOffset.roundToPx()) }
            .testTag(MarketTooltipTestTags.CONTAINER),
        visible = isVisibleWrapped,
        enter = slideIn(
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                visibilityThreshold = IntOffset.VisibilityThreshold,
            ),
            initialOffset = { _ -> IntOffset(y = -slideOffset.roundToInt(), x = 0) },
        ) + fadeIn(),
        exit = fadeOut(),
    ) {
        MarketsTooltipContent(
            onCloseClick = onCloseClick,
            modifier = Modifier.onSizeChanged { tooltipHeight = it.height },
        )
    }
}

@Composable
internal fun MarketsTooltipContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = TangemTheme.colors3.bg.tertiary
    val tipDpSize = DpSize(width = 20.dp, height = 8.dp)
    val tooltipShape = remember(tipDpSize) { TooltipShape(cornerRadius = 16.dp, tipSize = tipDpSize) }

    Row(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = tooltipShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.7f),
            )
            .background(backgroundColor, tooltipShape)
            .clickable(interactionSource = null, indication = null, onClick = {})
            .padding(all = 12.dp)
            .padding(bottom = tipDpSize.height),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(size = 18.dp),
            painter = painterResource(id = R.drawable.ic_plus_18),
            tint = Color.Unspecified,
            contentDescription = null,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp),
        ) {
            Text(
                text = stringResourceSafe(id = R.string.markets_tooltip_v2_title),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(id = R.string.markets_tooltip_message),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        TangemButton(
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X7,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_cross_20),
            onClick = onCloseClick,
            modifier = Modifier.testTag(MarketTooltipTestTags.CLOSE_BUTTON),
        )
    }
}

private class TooltipShape(
    private val cornerRadius: Dp,
    private val tipSize: DpSize,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val tipWidth = with(density) { tipSize.width.toPx() }
        val tipHeight = with(density) { tipSize.height.toPx() }
        val bodyHeight = size.height - tipHeight

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(left = 0f, top = 0f, right = size.width, bottom = bodyHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                ),
            )
            moveTo(size.width / 2 - tipWidth / 2, bodyHeight)
            lineTo(size.width / 2, size.height)
            lineTo(size.width / 2 + tipWidth / 2, bodyHeight)
            close()
        }
        return Outline.Generic(path)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MarketsTooltip_Preview() {
    TangemThemePreviewRedesign {
        MarketsTooltipContent(onCloseClick = {})
    }
}
// endregion