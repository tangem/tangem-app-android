package com.tangem.features.swap.v2.impl.amount.ui

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.AmountBlockV2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.ui.preview.SwapAmountContentPreview
import com.tangem.features.swap.v2.impl.chooseprovider.ui.SwapChooseProviderContent
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM

@Suppress("DestructuringDeclarationWithTooManyEntries", "LongParameterList")
@Composable
internal fun SwapAmountBlockContent(
    amountUM: SwapAmountUM,
    isClickEnabled: Boolean,
    onProviderSelectClick: () -> Unit,
    onInfoClick: () -> Unit,
    onClick: () -> Unit,
    onFinishAnimation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (amountUM !is SwapAmountUM.Content) return
    ConstraintLayout(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .clickable(
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
                enabled = isClickEnabled,
                onClick = onClick,
            ),
    ) {
        val (from, to, separator, provider) = createRefs()
        AmountBlockV2(
            amountState = amountUM.primaryAmount.amountField,
            isClickDisabled = true,
            isEditingDisabled = false,
            modifier = Modifier.constrainAs(from) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            extraContent = { SwapPriceImpact(amountFieldUM = amountUM.primaryAmount, onInfoClick = onInfoClick) },
        )
        AmountBlockV2(
            amountState = (amountUM.secondaryAmount.amountField as? AmountState.Data)?.copy(
                title = resourceReference(R.string.send_with_swap_recipient_amount_title),
                availableBalance = TextReference.EMPTY,
                availableBalanceShort = TextReference.EMPTY,
            ) ?: amountUM.secondaryAmount.amountField,
            isClickDisabled = true,
            isEditingDisabled = false,
            modifier = Modifier.constrainAs(to) {
                top.linkTo(from.bottom, 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            extraContent = {
                SwapPriceImpact(amountFieldUM = amountUM.secondaryAmount, onInfoClick = onInfoClick)
            },
        )
        SwapAmountDivider(
            modifier = Modifier.constrainAs(separator) {
                top.linkTo(from.bottom)
                bottom.linkTo(to.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
        )
        val quoteContent = amountUM.selectedQuote as? SwapQuoteUM.Content
        val isBestRate = quoteContent?.diffPercent is SwapQuoteUM.Content.DifferencePercent.Best
        SwapChooseProviderContent(
            isBestRate = isBestRate,
            showBestRateAnimation = amountUM.showBestRateAnimation,
            expressProvider = amountUM.selectedQuote.provider,
            onClick = onProviderSelectClick,
            onFinishAnimation = onFinishAnimation,
            modifier = Modifier.constrainAs(provider) {
                top.linkTo(to.bottom)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            showFCAWarning = amountUM.showFCAWarning,
        )
    }
}

@Composable
private fun SwapPriceImpact(amountFieldUM: SwapAmountFieldUM, onInfoClick: () -> Unit) {
    val priceImpact = (amountFieldUM as? SwapAmountFieldUM.Content)?.priceImpact
    if (priceImpact != null) {
        Text(
            text = priceImpact.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.attention,
        )
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_information_24),
            ),
            tint = TangemTheme.colors.icon.attention,
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onInfoClick,
                ),
        )
    }
}

@Composable
private fun SwapAmountDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        SwapDivider()
        Text(
            text = stringResourceSafe(R.string.swap_via_provider),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .background(TangemTheme.colors.button.secondary, RoundedCornerShape(32.dp))
                .padding(horizontal = 11.dp, vertical = 5.dp)
                .align(Alignment.Center),
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun BoxScope.SwapDivider() {
    val color = TangemTheme.colors.background.tertiary
    Canvas(
        Modifier
            .fillMaxWidth()
            .align(Alignment.Center),
    ) {
        val gapWidth = 2.dp.toPx()
        val radius = 2.dp.toPx()

        val pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(gapWidth, gapWidth * 3),
            phase = 0f,
        )
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = pathEffect,
            cap = StrokeCap.Round,
            strokeWidth = radius,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapAmountBlockContent_Preview() {
    TangemThemePreview {
        SwapAmountBlockContent(
            amountUM = SwapAmountContentPreview.defaultState,
            isClickEnabled = true,
            onProviderSelectClick = {},
            onInfoClick = {},
            onClick = {},
            onFinishAnimation = {},
        )
    }
}
// endregion