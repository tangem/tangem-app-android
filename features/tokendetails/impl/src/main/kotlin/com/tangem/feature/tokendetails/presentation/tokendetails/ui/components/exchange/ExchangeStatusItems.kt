package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.exchange

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.tokenicon.TokenIcon
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.SwapTransactionsState
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.PersistentList

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.swapTransactionsItems(
    swapTxs: PersistentList<SwapTransactionsState>,
    modifier: Modifier = Modifier,
) {
    if (swapTxs.isNotEmpty()) {
        items(
            count = swapTxs.size,
            key = { swapTxs[it].txId },
            contentType = { swapTxs[it]::class.java },
        ) {
            val item = swapTxs[it]

            val (iconRes, tint) = when (item.activeStatus) {
                ExchangeStatus.Verifying -> R.drawable.ic_alert_triangle_20 to TangemTheme.colors.icon.attention
                ExchangeStatus.Failed -> R.drawable.ic_alert_circle_24 to TangemTheme.colors.icon.warning
                else -> null to null
            }

            ExchangeStatusItem(
                providerName = item.provider.name,
                fromTokenIconState = item.fromCurrencyIcon,
                toTokenIconState = item.toCurrencyIcon,
                fromAmount = item.fromCryptoAmount,
                fromSymbol = item.fromCryptoSymbol,
                toAmount = item.toCryptoAmount,
                toSymbol = item.toCryptoSymbol,
                onClick = item.onClick,
                infoIconRes = iconRes,
                infoIconTint = tint,
                modifier = modifier.animateItemPlacement(),
            )
        }
    }
}

@Suppress("DestructuringDeclarationWithTooManyEntries", "LongMethod", "LongParameterList")
@Composable
private fun ExchangeStatusItem(
    providerName: String,
    fromTokenIconState: TokenIconState,
    toTokenIconState: TokenIconState,
    fromAmount: String,
    fromSymbol: String,
    toAmount: String,
    toSymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes infoIconRes: Int? = null,
    infoIconTint: Color? = null,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable { onClick() }
            .padding(TangemTheme.dimens.spacing12),
    ) {
        val (titleRef, iconRef, infoIconRef, swapIconRef, fromRef, toRef, fromIconRef, toIconRef) = createRefs()
        val padding6 = TangemTheme.dimens.spacing6

        Text(
            text = stringResource(id = R.string.express_exchange_by, providerName),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            },
        )
        TokenIcon(
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
            text = fromAmount,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            ellipsis = TextEllipsis.OffsetEnd(fromSymbol.length),
            modifier = Modifier.constrainAs(fromRef) {
                start.linkTo(fromIconRef.end, padding6)
                top.linkTo(titleRef.bottom, padding6)
                end.linkTo(swapIconRef.start)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
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
        TokenIcon(
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
            text = toAmount,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            ellipsis = TextEllipsis.OffsetEnd(toSymbol.length),
            modifier = Modifier.constrainAs(toRef) {
                start.linkTo(toIconRef.end, padding6)
                top.linkTo(titleRef.bottom, padding6)
                end.linkTo(infoIconRef.start, padding6)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
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
