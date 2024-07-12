package com.tangem.core.ui.components.rows

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.utils.StringsSigns

@Composable
fun SelectorRowItem(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(TangemTheme.dimens.spacing12),
    preDot: TextReference? = null,
    postDot: TextReference? = null,
    ellipsizeOffset: Int? = null,
    isSelected: Boolean = false,
    showDivider: Boolean = true,
    showSelectedAppearance: Boolean = true,
    onSelect: (() -> Unit)? = null,
) {
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            TangemTheme.colors.icon.accent
        } else {
            TangemTheme.colors.icon.informative
        },
        label = "Selector icon tint change",
    )
    val textStyle = if (isSelected && showSelectedAppearance) {
        TangemTheme.typography.subtitle2
    } else {
        TangemTheme.typography.body2
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onSelect != null) {
                    Modifier.clickable { onSelect() }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                tint = iconTint,
                contentDescription = null,
            )
            Text(
                text = stringResource(titleRes),
                style = textStyle,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            )
            if (preDot != null) {
                SelectorValueContent(
                    preDot = preDot,
                    postDot = postDot,
                    ellipsizeOffset = ellipsizeOffset,
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
private fun RowScope.SelectorValueContent(
    preDot: TextReference,
    postDot: TextReference?,
    textStyle: TextStyle,
    ellipsizeOffset: Int? = null,
) {
    val ellipsis = if (ellipsizeOffset == null) {
        TextEllipsis.End
    } else {
        TextEllipsis.OffsetEnd(ellipsizeOffset)
    }
    EllipsisText(
        text = preDot.resolveReference(),
        style = textStyle,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.End,
        ellipsis = ellipsis,
        modifier = Modifier
            .weight(1f)
            .padding(start = TangemTheme.dimens.spacing4),
    )
    if (postDot != null) {
        Text(
            text = StringsSigns.DOT,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing4),
        )
        Text(
            text = postDot.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SelectorRowItemPreview() {
    TangemThemePreview {
        SelectorRowItem(
            titleRes = R.string.common_fee_selector_option_slow,
            iconRes = R.drawable.ic_tortoise_24,
            preDot = TextReference.Str("1000 ETH"),
            postDot = TextReference.Str("1000 $"),
            ellipsizeOffset = 4,
            isSelected = true,
            onSelect = { },
        )
    }
}
