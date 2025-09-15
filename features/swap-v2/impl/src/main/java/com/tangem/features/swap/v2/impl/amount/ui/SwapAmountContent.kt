package com.tangem.features.swap.v2.impl.amount.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.ui.AmountFieldV2
import com.tangem.core.ui.components.SpacerH2
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.SwapAmountClickIntents
import com.tangem.features.swap.v2.impl.amount.ui.preview.SwapAmountClickIntentsStub
import com.tangem.features.swap.v2.impl.amount.ui.preview.SwapAmountContentPreview
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM

@Composable
internal fun SwapAmountContent(
    amountUM: SwapAmountUM,
    clickIntents: SwapAmountClickIntents,
    modifier: Modifier = Modifier,
) {
    val swapAmountContent = amountUM as? SwapAmountUM.Content
    val isFixedRate = swapAmountContent?.swapRateType == ExpressRateType.Fixed
    ConstraintLayout(
        modifier = modifier,
    ) {
        val (amountFromRef, amountToRef, middleButtonRef) = createRefs()
        SwapAmountBlock(
            amountFieldUM = amountUM.primaryAmount,
            selectedAmountType = amountUM.selectedAmountType,
            selectedQuote = swapAmountContent?.selectedQuote,
            isFixedRate = isFixedRate,
            clickIntents = clickIntents,
            modifier = Modifier.constrainAs(amountFromRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
        )
        SwapAmountBlock(
            amountFieldUM = amountUM.secondaryAmount,
            selectedAmountType = amountUM.selectedAmountType,
            selectedQuote = swapAmountContent?.selectedQuote,
            isFixedRate = isFixedRate,
            clickIntents = clickIntents,
            modifier = Modifier.constrainAs(amountToRef) {
                top.linkTo(amountFromRef.bottom, 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
        )
        SwapAmountBlockSeparator(
            onClick = clickIntents::onSeparatorClick,
            modifier = Modifier
                .constrainAs(middleButtonRef) {
                    top.linkTo(amountFromRef.bottom)
                    bottom.linkTo(amountToRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
        )
    }
}

@Composable
private fun SwapAmountBlockSeparator(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // todo add swap type like [Swap, SendViaSwap, SendIncognito]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(max = 28.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(TangemTheme.colors.button.secondary)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 12.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_convert),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_close_24),
            ),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun SwapAmountBlock(
    amountFieldUM: SwapAmountFieldUM,
    selectedQuote: SwapQuoteUM?,
    selectedAmountType: SwapAmountType,
    isFixedRate: Boolean,
    clickIntents: SwapAmountClickIntents,
    modifier: Modifier = Modifier,
) {
    val isSelectedAmountType = selectedAmountType == amountFieldUM.amountType
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors.background.action),
    ) {
        AnimatedVisibility(isSelectedAmountType) {
            Box {
                SwapAmountEditBlock(
                    amountFieldUM = amountFieldUM,
                    modifier = Modifier,
                    onValueChange = clickIntents::onAmountValueChange,
                    onValuePastedTriggerDismiss = clickIntents::onAmountPasteTriggerDismiss,
                    onCurrencyChange = clickIntents::onCurrencyChangeClick,
                )
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = TangemTheme.colors.stroke.primary,
                )
            }
        }
        SwapAmountInfo(
            amountFieldUM = amountFieldUM,
            selectedQuote = selectedQuote,
            isSelectedAmountType = isSelectedAmountType,
            isFixedRate = isFixedRate,
            onExpandEditField = clickIntents::onExpandEditField,
            onSelectTokenClick = clickIntents::onSelectTokenClick,
            onMaxAmountClick = clickIntents::onMaxValueClick,
        )
    }
}

@Composable
private fun SwapAmountEditBlock(
    amountFieldUM: SwapAmountFieldUM,
    onValueChange: (String) -> Unit,
    onValuePastedTriggerDismiss: () -> Unit,
    onCurrencyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.padding(top = 48.dp, bottom = 28.dp),
    ) {
        if (amountFieldUM.amountField !is AmountState.Data) {
            TextShimmer(
                style = TangemTheme.typography.caption2,
                modifier = Modifier.width(60.dp),
            )
        } else {
            Text(
                text = (amountFieldUM.amountField as AmountState.Data).title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        AmountFieldV2(
            amountUM = amountFieldUM.amountField,
            onValueChange = onValueChange,
            onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
            onCurrencyChange = onCurrencyChange,
            modifier = Modifier,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SwapAmountInfo(
    amountFieldUM: SwapAmountFieldUM,
    selectedQuote: SwapQuoteUM?,
    isSelectedAmountType: Boolean,
    isFixedRate: Boolean,
    onExpandEditField: (SwapAmountType) -> Unit,
    onMaxAmountClick: () -> Unit,
    onSelectTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokenIconState = (amountFieldUM.amountField as? AmountState.Data)?.tokenIconState ?: CurrencyIconState.Loading
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = (amountFieldUM as? SwapAmountFieldUM.Content)?.isClickEnabled == true,
                onClick = {
                    if (isFixedRate) {
                        onExpandEditField(amountFieldUM.amountType)
                    } else {
                        onSelectTokenClick()
                    }
                },
            ),
    ) {
        CurrencyIcon(
            state = tokenIconState,
            shouldDisplayNetwork = true,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
        )
        SwapAmountInfoMain(
            amountFieldUM = amountFieldUM,
            selectedQuote = selectedQuote,
            isSelectedAmountType = isSelectedAmountType,
            modifier = Modifier.weight(1f),
        )
        AnimatedContent(
            targetState = isSelectedAmountType,
        ) { isSelected ->
            if (isSelected) {
                AmountMaxButton(onMaxAmountClick)
            } else {
                SwapAmountInfoQuote(
                    quoteUM = selectedQuote,
                    isFixedRate = isFixedRate,
                    onSelectTokenClick = onSelectTokenClick,
                )
            }
        }
    }
}

@Composable
private fun SwapAmountInfoMain(
    amountFieldUM: SwapAmountFieldUM,
    selectedQuote: SwapQuoteUM?,
    isSelectedAmountType: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        AnimatedContent(amountFieldUM is SwapAmountFieldUM.Content) { isContent ->
            if (isContent && amountFieldUM is SwapAmountFieldUM.Content) {
                Text(
                    text = amountFieldUM.title.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                )
            } else {
                TextShimmer(
                    style = TangemTheme.typography.subtitle2,
                    modifier = Modifier.width(56.dp),
                )
            }
        }
        AnimatedContent(isSelectedAmountType) { isSelected ->
            if (isSelected && amountFieldUM is SwapAmountFieldUM.Content) {
                SpacerH2()
                Row {
                    EllipsisText(
                        text = amountFieldUM.subtitleLeft.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        ellipsis = amountFieldUM.subtitleEllipsisLeft,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    EllipsisText(
                        text = amountFieldUM.subtitleRight.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        ellipsis = amountFieldUM.subtitleEllipsisRight,
                    )
                }
            } else {
                AnimatedContent(selectedQuote) { quote ->
                    when (quote) {
                        is SwapQuoteUM.Content -> {
                            SpacerH2()
                            EllipsisText(
                                text = stringResourceSafe(
                                    R.string.send_with_swap_recipient_get_amount,
                                    quote.quoteAmountValue.resolveReference(),
                                ),
                                style = TangemTheme.typography.caption2,
                                color = TangemTheme.colors.text.tertiary,
                            )
                        }
                        SwapQuoteUM.Loading -> {
                            SpacerH2()
                            TextShimmer(
                                style = TangemTheme.typography.caption2,
                                modifier = Modifier.width(72.dp),
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountMaxButton(onMaxAmountClick: () -> Unit) {
    Text(
        text = stringResourceSafe(R.string.send_max_amount),
        style = TangemTheme.typography.caption1,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.button.secondary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onMaxAmountClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun SwapAmountInfoQuote(quoteUM: SwapQuoteUM?, isFixedRate: Boolean, onSelectTokenClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.conditionalCompose(
            condition = isFixedRate,
            modifier = {
                clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onSelectTokenClick,
                )
            },
        ),
    ) {
        AnimatedContent(
            quoteUM,
        ) { quote ->
            when (quote) {
                is SwapQuoteUM.Loading -> CircularProgressIndicator(
                    color = TangemTheme.colors.icon.inactive,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(20.dp),
                )
                else -> Box(modifier = Modifier.padding(start = 16.dp))
            }
        }

        Icon(
            painter = rememberVectorPainter(
                ImageVector.vectorResource(R.drawable.ic_chevron_24),
            ),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
            modifier = Modifier
                .padding(
                    end = 16.dp,
                    top = 24.dp,
                    bottom = 24.dp,
                )
                .size(24.dp),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapAmountContent_Preview(
    @PreviewParameter(SwapAmountContentPreviewProvider::class) params: SwapAmountUM,
) {
    TangemThemePreview {
        SwapAmountContent(
            amountUM = params,
            modifier = Modifier,
            clickIntents = SwapAmountClickIntentsStub,
        )
    }
}

private class SwapAmountContentPreviewProvider : PreviewParameterProvider<SwapAmountUM> {
    override val values: Sequence<SwapAmountUM>
        get() = sequenceOf(
            SwapAmountContentPreview.emptyState,
            SwapAmountContentPreview.defaultState,
        )
}
// endregion