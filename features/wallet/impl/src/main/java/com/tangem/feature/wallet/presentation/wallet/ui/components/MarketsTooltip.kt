package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.sheetscaffold.TangemSheetState
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.MarketTooltipTestTags
import com.tangem.core.ui.utils.lineTo
import com.tangem.core.ui.utils.moveTo
import com.tangem.core.ui.utils.toPx
import com.tangem.feature.wallet.impl.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun MarketsTooltip(
    availableHeight: Dp,
    bottomSheetState: TangemSheetState,
    isVisible: Boolean,
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
        MarketsTooltipContent()
    }
}

@Composable
internal fun MarketsHint2(isVisible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
        exit = fadeOut(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResourceSafe(R.string.markets_hint),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
            Icon(
                modifier = Modifier.size(size = 24.dp),
                painter = painterResource(id = R.drawable.ic_chevron_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun MarketsTooltipContent(modifier: Modifier = Modifier) {
    val backgroundColor = TangemTheme.colors.background.action
    val cornerRadius = CornerRadius(x = 14.dp.toPx())
    val tipDpSize = DpSize(width = 20.dp, height = 8.dp)

    Column(
        modifier = modifier
            .padding(bottom = tipDpSize.height)
            .drawBehind {
                val rect = size.toRect()
                val tipSize = tipDpSize.toSize()
                val tipRect = Rect(
                    offset = Offset(
                        x = rect.center.x - tipSize.center.x,
                        y = rect.bottom,
                    ),
                    size = tipSize,
                )
                drawRoundRect(color = backgroundColor, cornerRadius = cornerRadius)

                val tipPath = Path().apply {
                    moveTo(tipRect.topLeft)
                    lineTo(tipRect.bottomCenter)
                    lineTo(tipRect.topRight)
                }
                drawPath(color = backgroundColor, path = tipPath)
            }
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResourceSafe(id = R.string.markets_tooltip_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = stringResourceSafe(id = R.string.markets_tooltip_message),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}