package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ExchangeStatusState
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.PersistentList

@Composable
internal fun ExchangeStatusBlock(
    status: PersistentList<ExchangeStatusState>,
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
                text = stringResource(id = R.string.common_balance_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            SpacerWMax()
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
                    text = stringResource(id = R.string.express_go_to_provider),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }

        status.forEachIndexed { index, item ->
            ExchangeStatusStep(
                stepStatus = item,
                isLast = index == status.lastIndex,
            )
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
                    it.status == ExchangeStatus.Failed && !it.isDone -> ExchangeStepWaringOrError(
                        iconRes = R.drawable.ic_close_24,
                        color = TangemTheme.colors.icon.warning,
                    )
                    it.status == ExchangeStatus.Verifying && !it.isDone -> ExchangeStepWaringOrError(
                        iconRes = R.drawable.ic_exclamation_24,
                        color = TangemTheme.colors.icon.attention,
                    )
                    it.isDone -> ExchangeStepSuccess()
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
        stepStatus.status == ExchangeStatus.Failed && !stepStatus.isDone -> TangemTheme.colors.icon.warning
        stepStatus.status == ExchangeStatus.Verifying && !stepStatus.isDone -> TangemTheme.colors.icon.attention
        stepStatus.isDone -> TangemTheme.colors.text.primary1
        !stepStatus.isActive -> TangemTheme.colors.text.disabled
        else -> TangemTheme.colors.text.primary1
    }

    Text(
        text = stepStatus.text,
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
private fun ExchangeStepSuccess() {
    Icon(
        painter = painterResource(id = R.drawable.ic_check_24),
        contentDescription = null,
        tint = TangemTheme.colors.icon.primary1,
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
private fun ExchangeStepWaringOrError(color: Color, @DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .border(
                width = TangemTheme.dimens.size1_5,
                color = color,
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
