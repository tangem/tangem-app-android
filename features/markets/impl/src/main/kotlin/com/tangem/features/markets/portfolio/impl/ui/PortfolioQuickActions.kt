package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.icons.badge.drawBadge
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PortfolioQuickActions(
    actions: ImmutableList<QuickActionUM>,
    isVisible: Boolean,
    onActionClick: (QuickActionUM) -> Unit,
    onActionLongClick: (QuickActionUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Column(modifier = modifier) {
            actions.fastForEach { action ->
                LineSeparator()
                QuickActionItem(
                    state = action,
                    onClick = { onActionClick(action) },
                    onLongClick = { onActionLongClick(action) }.takeIf { action.longClickAvailable },
                )
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.LineSeparator(modifier: Modifier = Modifier) {
    val lineColor = TangemTheme.colors.stroke.primary
    val strokeWidth = TangemTheme.dimens.size1
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val startPadding = TangemTheme.dimens.spacing30

    val height = TangemTheme.dimens.size16

    Canvas(
        modifier = modifier
            .animateEnterExit(
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessLow,
                    ),
                    expandFrom = Alignment.Top,
                ) + fadeIn(),
                exit = shrinkVertically(
                    spring(
                        stiffness = Spring.StiffnessLow,
                    ),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(),
            )
            .fillMaxWidth()
            .height(height),
    ) {
        val x = if (isLtr) startPadding.toPx() else size.width - startPadding.toPx()

        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth.toPx(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedVisibilityScope.QuickActionItem(
    state: QuickActionUM,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hapticManager = LocalHapticManager.current
    val onLongClickInternal: (() -> Unit)? = if (onLongClick != null) {
        {
            hapticManager.perform(TangemHapticEffect.View.LongPress)
            onLongClick()
        }
    } else {
        null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongClickInternal,
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.SegmentTick)
                    onClick()
                },
            )
            .padding(horizontal = TangemTheme.dimens.spacing14, vertical = TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing18),
    ) {
        QuickActionIcon(state)
        Column(
            modifier = Modifier
                .animateEnterExit(
                    enter = fadeIn(),
                    exit = fadeOut(),
                ),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = state.description.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.QuickActionIcon(state: QuickActionUM) {
    val containerColor = TangemTheme.colors.background.action
    Box(
        Modifier
            .animateEnterExit(
                enter = scaleIn(),
                exit = scaleOut(),
            )
            .background(
                color = TangemTheme.colors.button.secondary,
                shape = CircleShape,
            )
            .size(TangemTheme.dimens.size32)
            .drawWithContent {
                drawContent()
                if (state is QuickActionUM.Exchange && state.showBadge) {
                    drawBadge(containerColor = containerColor, offset = 4.dp)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier
                .requiredSize(TangemTheme.dimens.size16),
            imageVector = ImageVector.vectorResource(id = state.icon),
            contentDescription = null,
            tint = TangemTheme.colors.button.primary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        var isVisible by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(680.dp),
        ) {
            Button(
                onClick = { isVisible = !isVisible },
                modifier = Modifier.padding(TangemTheme.dimens.spacing12),
            ) {
                Text(text = "Toggle")
            }
            SpacerH4()
            Box(
                modifier = Modifier.background(color = TangemTheme.colors.background.action),
            ) {
                PortfolioQuickActions(
                    actions = persistentListOf(
                        QuickActionUM.Buy,
                        QuickActionUM.Exchange(showBadge = true),
                        QuickActionUM.Receive,
                    ),
                    isVisible = isVisible,
                    onActionClick = {},
                    onActionLongClick = {},
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewRtl() {
    TangemThemePreview(rtl = true) {
        Box(modifier = Modifier.background(color = TangemTheme.colors.background.action)) {
            PortfolioQuickActions(
                actions = persistentListOf(
                    QuickActionUM.Buy,
                    QuickActionUM.Exchange(showBadge = true),
                    QuickActionUM.Receive,
                ),
                isVisible = true,
                onActionClick = {},
                onActionLongClick = {},
            )
        }
    }
}