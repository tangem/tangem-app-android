package com.tangem.core.ui.components.rows

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
fun SelectorRowItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    preEllipsize: TextReference? = null,
    postEllipsize: TextReference? = null,
    isSelected: Boolean = false,
    showDivider: Boolean = true,
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() },
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
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
            if (preEllipsize != null && postEllipsize != null) {
                SelectorValueContent(
                    amount = preEllipsize,
                    symbol = postEllipsize,
                    textStyle = textStyle,
                )
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

@Composable
private fun RowScope.SelectorValueContent(amount: TextReference, symbol: TextReference, textStyle: TextStyle) {
    Text(
        text = amount.resolveReference(),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.End,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
            .weight(1f)
            .padding(
                start = TangemTheme.dimens.spacing4,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
    Text(
        text = symbol.resolveReference(),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing1,
                end = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
            ),
    )
}

@Preview
@Composable
private fun SelectorRowItemPreview_Light() {
    TangemTheme {
        SelectorRowItem(
            titleRes = R.string.common_fee_selector_option_slow,
            iconRes = R.drawable.ic_tortoise_24,
            preEllipsize = TextReference.Str("1000"),
            postEllipsize = TextReference.Str("$"),
            isSelected = true,
            onSelect = { },
        )
    }
}

@Preview
@Composable
private fun SelectorRowItemPreview_Dark() {
    TangemTheme(isDark = true) {
        SelectorRowItem(
            titleRes = R.string.common_fee_selector_option_slow,
            iconRes = R.drawable.ic_tortoise_24,
            preEllipsize = TextReference.Str("1000"),
            postEllipsize = TextReference.Str("$"),
            isSelected = true,
            onSelect = { },
        )
    }
}