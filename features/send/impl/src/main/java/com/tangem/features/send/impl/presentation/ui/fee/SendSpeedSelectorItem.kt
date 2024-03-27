package com.tangem.features.send.impl.presentation.ui.fee

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState

@Composable
internal fun SendSpeedSelectorItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    feeSelectorState: FeeSelectorState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    amount: TextReference? = null,
    fiatAmount: TextReference? = null,
    symbolLength: Int? = null,
    isSelected: Boolean = false,
    showDivider: Boolean = true,
    showWarning: Boolean = false,
    isSingle: Boolean = false,
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            TangemTheme.colors.icon.accent
        } else {
            TangemTheme.colors.icon.informative
        },
        label = "Selector icon tint change",
    )

    val textStyle = if (isSelected) {
        TangemTheme.typography.subtitle2
    } else {
        TangemTheme.typography.body2
    }

    AnimatedVisibility(
        visible = !isSingle,
        label = "Fee Selector Visibility Animation",
        enter = expandVertically().plus(fadeIn()),
        exit = shrinkVertically().plus(fadeOut()),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onSelect() },
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SelectorTitleContent(
                    titleRes = titleRes,
                    iconRes = iconRes,
                    iconTint = iconTint,
                    textStyle = textStyle,
                )
                Box {
                    if (amount != null && symbolLength != null && fiatAmount != null) {
                        SelectorValueContent(
                            amount = amount,
                            fiatAmount = fiatAmount,
                            symbolLength = symbolLength,
                            textStyle = textStyle,
                        )
                    }
                    SendSpeedSelectorItemLoading(isLoading = feeSelectorState.isLoading)
                    SendSpeedSelectorItemError(isError = feeSelectorState.isError)
                    WarningIcon(showWarning = showWarning)
                }
            }
            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TangemTheme.dimens.size1)
                        .padding(horizontal = TangemTheme.dimens.spacing12)
                        .background(TangemTheme.colors.stroke.primary)
                        .align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SelectorTitleContent(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    iconTint: Color = TangemTheme.colors.icon.informative,
    textStyle: TextStyle = TangemTheme.typography.body2,
) {
    Icon(
        painter = painterResource(iconRes),
        tint = iconTint,
        contentDescription = null,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
    )
    Text(
        text = stringResource(titleRes),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
}

@Composable
private fun SelectorValueContent(
    amount: TextReference,
    fiatAmount: TextReference,
    symbolLength: Int,
    textStyle: TextStyle,
) {
    Row {
        EllipsisText(
            text = amount.resolveReference(),
            style = textStyle,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.End,
            ellipsis = TextEllipsis.OffsetEnd(symbolLength),
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = TangemTheme.dimens.spacing4,
                    top = TangemTheme.dimens.spacing14,
                    bottom = TangemTheme.dimens.spacing14,
                ),
        )
        Text(
            text = "(${fiatAmount.resolveReference()})",
            style = textStyle,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing4,
                    end = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing14,
                    bottom = TangemTheme.dimens.spacing14,
                ),
        )
    }
}

@Composable
private fun SendSpeedSelectorItemError(isError: Boolean) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = isError,
            label = "Error state indication animation",
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing14,
                        horizontal = TangemTheme.dimens.spacing12,
                    ),
            )
        }
    }
}

@Composable
private fun SendSpeedSelectorItemLoading(isLoading: Boolean) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = isLoading,
            label = "Loading state indication animation",
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RectangleShimmer(
                radius = TangemTheme.dimens.radius3,
                modifier = Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing18,
                        bottom = TangemTheme.dimens.spacing18,
                        end = TangemTheme.dimens.spacing12,
                    )
                    .size(
                        width = TangemTheme.dimens.size90,
                        height = TangemTheme.dimens.size12,
                    ),
            )
        }
    }
}

@Composable
private fun WarningIcon(showWarning: Boolean = false) {
    Row {
        SpacerWMax()
        AnimatedVisibility(
            visible = showWarning,
            label = "Custom fee warning indicator",
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_alert_triangle_20),
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        vertical = TangemTheme.dimens.spacing12,
                        horizontal = TangemTheme.dimens.spacing14,
                    ),
            )
        }
    }
}
