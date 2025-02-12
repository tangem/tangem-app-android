package com.tangem.common.ui.expressStatus

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.common.ui.R
import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

/**
 * Block with express statuses
 *
 * @param state ui holder
 * @param modifier modifier
 * @see [Figma](https://www.figma.com/design/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=18459-26521&t=4jox7bfqUiXnm2h1-4)
 */
@Composable
fun ExpressStatusBlock(state: ExpressStatusUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(
                vertical = TangemTheme.dimens.spacing14,
                horizontal = TangemTheme.dimens.spacing12,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing16),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            AnimatedVisibility(visible = state.link is ExpressLinkUM.Content) {
                val link = remember(this) { state.link as ExpressLinkUM.Content }
                Row(
                    modifier = Modifier.clickable { link.onClick() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = link.icon),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .size(TangemTheme.dimens.spacing16)
                            .padding(end = TangemTheme.dimens.spacing2),
                    )
                    Text(
                        text = link.text.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }

        Column {
            state.statuses.forEachIndexed { index, item ->
                ExpressStatusStep(item, index == state.statuses.lastIndex)
            }
        }
    }
}

@Composable
private fun ExpressStatusStep(status: ExpressStatusItemUM, isLast: Boolean) {
    AnimatedContent(
        targetState = status,
        label = "Exchange Step Change Success",
        transitionSpec = {
            fadeIn(tween(durationMillis = 220)) togetherWith
                fadeOut(tween(durationMillis = 220))
        },
    ) { content ->
        Row {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (content.state) {
                    ExpressStatusItemState.Active -> StepInProgress()
                    ExpressStatusItemState.Default -> StepDefault()
                    ExpressStatusItemState.Done -> Step(
                        iconRes = R.drawable.ic_check_24,
                        iconColor = TangemTheme.colors.icon.primary1,
                        borderColor = TangemTheme.colors.field.focused,
                    )
                    ExpressStatusItemState.Error -> Step(
                        iconRes = R.drawable.ic_close_24,
                        iconColor = TangemTheme.colors.icon.warning,
                    )
                    ExpressStatusItemState.Warning -> Step(
                        iconRes = R.drawable.ic_close_24,
                        iconColor = TangemTheme.colors.icon.attention,
                    )
                }
                if (!isLast) {
                    StepSeparator()
                }
            }
            val textColor = when (status.state) {
                ExpressStatusItemState.Active -> TangemTheme.colors.text.primary1
                ExpressStatusItemState.Default -> TangemTheme.colors.text.disabled
                ExpressStatusItemState.Done -> TangemTheme.colors.text.primary1
                ExpressStatusItemState.Error -> TangemTheme.colors.text.warning
                ExpressStatusItemState.Warning -> TangemTheme.colors.text.attention
            }
            Text(
                text = content.text.resolveReference(),
                style = TangemTheme.typography.body2,
                color = textColor,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun StepDefault() {
    Box(
        modifier = Modifier
            .size(TangemTheme.dimens.size20)
            .border(
                width = TangemTheme.dimens.size1_5,
                color = TangemTheme.colors.field.focused,
                shape = CircleShape,
            )
            .padding(TangemTheme.dimens.spacing2),
    )
}

@Composable
private fun Step(iconColor: Color, @DrawableRes iconRes: Int, borderColor: Color = iconColor) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier
            .size(TangemTheme.dimens.size20)
            .border(
                width = TangemTheme.dimens.size1_5,
                color = borderColor,
                shape = CircleShape,
            )
            .padding(TangemTheme.dimens.spacing2),
    )
}

@Composable
private fun StepInProgress() {
    CircularProgressIndicator(
        color = TangemTheme.colors.icon.primary1,
        strokeWidth = TangemTheme.dimens.size2,
        modifier = Modifier
            .padding(TangemTheme.dimens.spacing2)
            .size(TangemTheme.dimens.size14),
    )
}

@Composable
private fun StepSeparator() {
    Box(
        modifier = Modifier
            .padding(vertical = TangemTheme.dimens.spacing2)
            .size(
                width = TangemTheme.dimens.size1_5,
                height = TangemTheme.dimens.size10,
            )
            .background(
                color = TangemTheme.colors.field.focused,
                shape = CircleShape,
            ),
    )
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_ExchangeStatusBlock() {
    val state = ExpressStatusUM(
        title = resourceReference(R.string.express_exchange_status_title),
        link = ExpressLinkUM.Content(
            icon = R.drawable.ic_alert_24,
            text = resourceReference(R.string.common_go_to_provider),
            onClick = {},
        ),
        statuses = persistentListOf(
            ExpressStatusItemUM(text = stringReference("Done"), state = ExpressStatusItemState.Done),
            ExpressStatusItemUM(text = stringReference("Active"), state = ExpressStatusItemState.Active),
            ExpressStatusItemUM(text = stringReference("Warning"), state = ExpressStatusItemState.Warning),
            ExpressStatusItemUM(text = stringReference("Error"), state = ExpressStatusItemState.Error),
            ExpressStatusItemUM(text = stringReference("Default"), state = ExpressStatusItemState.Default),
        ),
    )

    TangemThemePreview {
        ExpressStatusBlock(state = state)
    }
}