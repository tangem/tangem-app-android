package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Input Row Approx](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2207-810&mode=design&t=fM1ZU6zQF6g3CaTv-4)
 *
 * @param leftIcon left token state
 * @param leftTitle left token title
 * @param leftSubtitle left token subtitle
 * @param rightIcon right token state
 * @param rightTitle right token title
 * @param rightSubtitle right token subtitle
 * @param modifier composable modifier
 * @param showDivider show divider
 */
@Suppress("LongParameterList")
@Composable
fun InputRowApprox(
    leftIcon: CurrencyIconState,
    leftTitle: TextReference,
    leftSubtitle: TextReference,
    rightIcon: CurrencyIconState,
    rightTitle: TextReference,
    rightSubtitle: TextReference,
    modifier: Modifier = Modifier,
    leftTitleEllipsisOffset: Int = 0,
    rightTitleEllipsisOffset: Int = 0,
    showDivider: Boolean = false,
) {
    DividerContainer(
        showDivider = showDivider,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        ) {
            InputRowApproxItem(
                iconState = leftIcon,
                title = leftTitle,
                subtitle = leftSubtitle,
                titleEllipsisOffset = leftTitleEllipsisOffset,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_forward_12),
                contentDescription = null,
                tint = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(
                        horizontal = TangemTheme.dimens.spacing4,
                        vertical = TangemTheme.dimens.spacing10,
                    ),
            )
            InputRowApproxItem(
                iconState = rightIcon,
                title = rightTitle,
                subtitle = rightSubtitle,
                titleEllipsisOffset = rightTitleEllipsisOffset,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InputRowApproxItem(
    iconState: CurrencyIconState,
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
    titleEllipsisOffset: Int = 0,
) {
    Row(
        modifier = modifier,
    ) {
        CurrencyIcon(
            state = iconState,
            modifier = Modifier
                .size(TangemTheme.dimens.size36),
        )
        Column(
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing12,
                ),
        ) {
            EllipsisText(
                text = title.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                ellipsis = TextEllipsis.OffsetEnd(titleEllipsisOffset),
            )
            EllipsisText(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing2),
            )
        }
    }
}

//region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowApproxPreview() {
    TangemThemePreview {
        Column {
            InputRowApprox(
                leftIcon = CurrencyIconState.Loading,
                leftTitle = TextReference.Str("Left title USD"),
                leftSubtitle = TextReference.Str("Left subtitle USD"),
                leftTitleEllipsisOffset = 3,
                rightIcon = CurrencyIconState.Loading,
                rightTitle = TextReference.Str("Right title Right title Right title Right title Right title USD"),
                rightSubtitle = TextReference.Str("Right subtitle Right subtitle Right subtitle USD"),
                rightTitleEllipsisOffset = 3,
                modifier = Modifier
                    .background(TangemTheme.colors.background.action),
            )
            InputRowApprox(
                leftIcon = CurrencyIconState.Loading,
                leftTitle = TextReference.Str("Left title Left title Left title Left title Left title USD"),
                leftSubtitle = TextReference.Str("Left subtitle Left subtitle Left subtitle USD"),
                leftTitleEllipsisOffset = 3,
                rightIcon = CurrencyIconState.Loading,
                rightTitle = TextReference.Str("Right title USD"),
                rightSubtitle = TextReference.Str("Right subtitle USD"),
                rightTitleEllipsisOffset = 3,
                modifier = Modifier
                    .background(TangemTheme.colors.background.action),
            )
        }
    }
}
//endregion