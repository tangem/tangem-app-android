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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.MarketTooltipTestTags
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.impl.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun MarketsTooltip(
    availableHeight: Dp,
    bottomSheetState: TangemSheetState,
    isVisible: Boolean,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tooltipOffset by remember {
        derivedStateOf {
            val bottomSheetOffset = try {
                // Can throw exception during the first composition
                with(density) { bottomSheetState.requireOffset().toDp() }
            } catch (e: Exception) {
                0.dp
            }

            bottomSheetOffset - availableHeight
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
        MarketsTooltipContent(onCloseClick = onCloseClick)
    }
}

@Composable
internal fun MarketsTooltipContent(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = TangemTheme.colors.background.action
    val tipDpSize = DpSize(width = 20.dp, height = 8.dp)
    val tooltipShape = remember(tipDpSize) { TooltipShape(cornerRadius = 16.dp, tipSize = tipDpSize) }

    Row(
        modifier = modifier
            .shadow(
                elevation = TangemTheme.dimens.elevation12,
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
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = stringResourceSafe(id = R.string.markets_tooltip_message),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.secondary,
            )
        }
        Icon(
            modifier = Modifier
                .size(size = 16.dp)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onCloseClick,
                )
                .testTag(MarketTooltipTestTags.CLOSE_BUTTON),
            painter = painterResource(id = R.drawable.ic_close_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
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