package com.tangem.common.ui.expressStatus

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.constraintlayout.compose.*
import com.tangem.common.ui.R
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Suppress("DestructuringDeclarationWithTooManyEntries", "LongMethod", "LongParameterList")
@Composable
internal fun ExpressStatusItem(
    title: TextReference,
    fromTokenIconState: CurrencyIconState,
    toTokenIconState: CurrencyIconState,
    fromAmount: TextReference,
    fromSymbol: String,
    toSymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    toAmount: TextReference = TextReference.EMPTY,
    @DrawableRes infoIconRes: Int? = null,
    infoIconTint: Color? = null,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.primary)
            .clickable { onClick() }
            .padding(TangemTheme.dimens.spacing12),
    ) {
        val (titleRef, iconRef, infoIconRef, swapIconRef, fromRef, toRef, fromIconRef, toIconRef) = createRefs()
        val padding6 = TangemTheme.dimens.spacing6

        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            },
        )
        CurrencyIcon(
            state = fromTokenIconState,
            shouldDisplayNetwork = false,
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .constrainAs(fromIconRef) {
                    start.linkTo(parent.start)
                    top.linkTo(titleRef.bottom, padding6)
                    bottom.linkTo(parent.bottom)
                },
        )
        EllipsisText(
            text = fromAmount.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            ellipsis = TextEllipsis.OffsetEnd(fromSymbol.length),
            modifier = Modifier.constrainAs(fromRef) {
                start.linkTo(fromIconRef.end, padding6)
                top.linkTo(titleRef.bottom, padding6)
                end.linkTo(swapIconRef.start)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints.atMostWrapContent
            },
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_forward_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .size(TangemTheme.dimens.size12)
                .constrainAs(swapIconRef) {
                    start.linkTo(fromRef.end, padding6)
                    top.linkTo(titleRef.bottom, padding6)
                    end.linkTo(toIconRef.start)
                    bottom.linkTo(parent.bottom)
                },
        )
        CurrencyIcon(
            state = toTokenIconState,
            shouldDisplayNetwork = false,
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .constrainAs(toIconRef) {
                    start.linkTo(swapIconRef.end, padding6)
                    top.linkTo(titleRef.bottom, padding6)
                    end.linkTo(toRef.start)
                    bottom.linkTo(parent.bottom)
                },
        )
        EllipsisText(
            text = toAmount.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            ellipsis = TextEllipsis.OffsetEnd(toSymbol.length),
            modifier = Modifier.constrainAs(toRef) {
                start.linkTo(toIconRef.end, padding6)
                top.linkTo(titleRef.bottom, padding6)
                end.linkTo(infoIconRef.start, padding6, padding6)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints.atLeastWrapContent
            },
        )
        Icon(
            painter = painterResource(id = infoIconRes ?: R.drawable.ic_alert_triangle_20),
            contentDescription = null,
            tint = infoIconTint ?: TangemTheme.colors.icon.informative,
            modifier = Modifier
                .size(TangemTheme.dimens.size20)
                .constrainAs(infoIconRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(iconRef.start)
                    visibility = if (infoIconRes == null) {
                        Visibility.Gone
                    } else {
                        Visibility.Visible
                    }
                },
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .size(TangemTheme.dimens.size24)
                .constrainAs(iconRef) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
        )
    }
}

//region Preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpressStatusItemPreview(
    @PreviewParameter(ExpressStatusItemPreviewParameterProvider::class) amount: String,
) {
    TangemThemePreview {
        ExpressStatusItem(
            title = stringReference("ChangeNow"),
            fromTokenIconState = CurrencyIconState.Loading,
            toTokenIconState = CurrencyIconState.Loading,
            fromAmount = stringReference(amount),
            fromSymbol = "USDT",
            toAmount = stringReference(amount),
            toSymbol = "USDT",
            onClick = {},
            infoIconRes = null,
            infoIconTint = null,
        )
    }
}

private class ExpressStatusItemPreviewParameterProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() = sequenceOf(
            "1111111111111111111111111111 USDT",
            "11111 USDT",
        )
}
//endregion