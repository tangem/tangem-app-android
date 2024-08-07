package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ExchangeStatusState
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ExchangeStatusBlock(
    statuses: ImmutableList<ExchangeStatusState>,
    showLink: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = stringResource(id = R.string.express_exchange_status_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
            AnimatedVisibility(visible = showLink) {
                Row(
                    modifier = Modifier.clickable { onClick() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_top_right_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier
                            .size(TangemTheme.dimens.spacing16)
                            .padding(end = TangemTheme.dimens.spacing2),
                    )
                    Text(
                        text = stringResource(id = R.string.common_go_to_provider),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }

        AnimatedContent(targetState = statuses.lastIndex, label = "Exchange Status List Change") {
            Column {
                statuses.forEachIndexed { index, item ->
                    ExchangeStatusStep(
                        stepStatus = item,
                        isLast = index == it,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeStatusStep(
    stepStatus: ExchangeStatusState,
    modifier: Modifier = Modifier,
    isLast: Boolean = false,
) {
    Row(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = stepStatus,
                label = "Exchange Step Change Success",
                modifier = Modifier
                    .size(TangemTheme.dimens.size20),
            ) {
                when {
                    it.status == ExchangeStatus.Cancelled -> ExchangeStep(
                        iconRes = R.drawable.ic_close_24,
                        color = TangemTheme.colors.icon.warning,
                        isDone = false,
                    )
                    it.status == ExchangeStatus.Failed -> ExchangeStep(
                        iconRes = R.drawable.ic_close_24,
                        color = TangemTheme.colors.icon.warning,
                        isDone = it.isDone,
                    )
                    it.status == ExchangeStatus.Refunded -> ExchangeStep(
                        iconRes = R.drawable.ic_close_24,
                        color = TangemTheme.colors.icon.warning,
                        isDone = it.isDone,
                    )
                    it.status == ExchangeStatus.Verifying -> ExchangeStep(
                        iconRes = R.drawable.ic_exclamation_24,
                        color = TangemTheme.colors.icon.attention,
                        isDone = it.isDone,
                    )
                    it.isDone -> ExchangeStep(
                        iconRes = R.drawable.ic_check_24,
                        color = TangemTheme.colors.icon.primary1,
                        isDone = true,
                    )
                    it.isActive -> ExchangeStepInProgress()
                    else -> ExchangeStepDefault()
                }
            }
            if (!isLast) {
                ExchangeStepSeparator()
            }
        }
        ExchangeStatusStepText(stepStatus)
    }
}

@Composable
private fun ExchangeStatusStepText(stepStatus: ExchangeStatusState) {
    val textColor = when {
        stepStatus.status == ExchangeStatus.Cancelled -> TangemTheme.colors.icon.warning
        stepStatus.status == ExchangeStatus.Refunded -> TangemTheme.colors.icon.warning
        stepStatus.status == ExchangeStatus.Failed && !stepStatus.isDone -> TangemTheme.colors.icon.warning
        stepStatus.status == ExchangeStatus.Verifying && !stepStatus.isDone -> TangemTheme.colors.icon.attention
        stepStatus.isDone -> TangemTheme.colors.text.primary1
        !stepStatus.isActive -> TangemTheme.colors.text.disabled
        else -> TangemTheme.colors.text.primary1
    }

    Text(
        text = stepStatus.text.resolveReference(),
        style = TangemTheme.typography.body2,
        color = textColor,
        modifier = Modifier
            .padding(start = TangemTheme.dimens.spacing12),
    )
}

@Composable
private fun ExchangeStepDefault() {
    Box(
        modifier = Modifier
            .border(
                width = TangemTheme.dimens.size1_5,
                color = TangemTheme.colors.field.focused,
                shape = CircleShape,
            )
            .padding(TangemTheme.dimens.spacing2),
    )
}

@Composable
private fun ExchangeStep(color: Color, @DrawableRes iconRes: Int, isDone: Boolean) {
    val (iconColor, borderColor) = if (isDone) {
        TangemTheme.colors.icon.primary1 to TangemTheme.colors.field.focused
    } else {
        color to color
    }
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier
            .border(
                width = TangemTheme.dimens.size1_5,
                color = borderColor,
                shape = CircleShape,
            )
            .padding(TangemTheme.dimens.spacing2),
    )
}

@Composable
private fun ExchangeStepInProgress() {
    CircularProgressIndicator(
        color = TangemTheme.colors.icon.primary1,
        strokeWidth = TangemTheme.dimens.size2,
        modifier = Modifier
            .padding(TangemTheme.dimens.spacing2)
            .size(TangemTheme.dimens.size14),
    )
}

@Composable
private fun ExchangeStepSeparator() {
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
