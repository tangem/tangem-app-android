package com.tangem.core.ui.components.inputrow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

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
    leftIcon: TokenIconState,
    leftTitle: TextReference,
    leftSubtitle: TextReference,
    rightIcon: TokenIconState,
    rightTitle: TextReference,
    rightSubtitle: TextReference,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    DividerContainer(
        showDivider = showDivider,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing12)
                .fillMaxWidth(),
        ) {
            InputRowApproxItem(
                iconState = leftIcon,
                title = leftTitle,
                subtitle = leftSubtitle,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_approx_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .padding(
                        horizontal = TangemTheme.dimens.spacing8,
                        vertical = TangemTheme.dimens.spacing10,
                    ),
            )
            InputRowApproxItem(
                iconState = rightIcon,
                title = rightTitle,
                subtitle = rightSubtitle,
            )
        }
    }
}

@Composable
private fun InputRowApproxItem(
    iconState: TokenIconState,
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        TokenIcon(
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
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
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
@Composable
private fun InputRowApproxPreview_Light() {
    TangemTheme {
        InputRowApprox(
            leftIcon = TokenIconState.Loading,
            leftTitle = TextReference.Str("Left title"),
            leftSubtitle = TextReference.Str("Left subtitle"),
            rightIcon = TokenIconState.Loading,
            rightTitle = TextReference.Str("Right title"),
            rightSubtitle = TextReference.Str("Right subtitle"),
            modifier = Modifier
                .background(TangemTheme.colors.background.action),
        )
    }
}

@Preview
@Composable
private fun InputRowApproxPreview_Dark() {
    TangemTheme(isDark = true) {
        InputRowApprox(
            leftIcon = TokenIconState.Loading,
            leftTitle = TextReference.Str("Left title"),
            leftSubtitle = TextReference.Str("Left subtitle"),
            rightIcon = TokenIconState.Loading,
            rightTitle = TextReference.Str("Right title"),
            rightSubtitle = TextReference.Str("Right subtitle"),
            modifier = Modifier
                .background(TangemTheme.colors.background.action),
        )
    }
}
//endregion
