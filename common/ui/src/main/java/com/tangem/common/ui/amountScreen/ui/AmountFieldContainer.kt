package com.tangem.common.ui.amountScreen.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.R
import com.tangem.common.ui.account.AccountTitle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.SendScreenTestTags

private const val AMOUNT_FIELD_KEY = "amountFieldKey"

internal fun LazyListScope.amountFieldV2(
    amountState: AmountState,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onValuePastedTriggerDismiss: () -> Unit,
    onCurrencyChange: (Boolean) -> Unit,
    onMaxAmountClick: () -> Unit,
) {
    item(key = AMOUNT_FIELD_KEY) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier
                    .padding(top = 48.dp, bottom = 28.dp),
            ) {
                if (amountState !is AmountState.Data) {
                    TextShimmer(
                        style = TangemTheme.typography.caption2,
                        modifier = Modifier.width(60.dp),
                    )
                } else {
                    AccountTitle(amountState.accountTitleUM)
                }
                AmountFieldV2(
                    amountUM = amountState,
                    onValueChange = onValueChange,
                    onValuePastedTriggerDismiss = onValuePastedTriggerDismiss,
                    onCurrencyChange = onCurrencyChange,
                    modifier = Modifier,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = TangemTheme.colors.stroke.primary,
            )
            AmountInfo(
                amountUM = amountState,
                onMaxAmountClick = onMaxAmountClick,
            )
        }
    }
}

@Composable
private fun AmountInfo(amountUM: AmountState, onMaxAmountClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokenIconState = (amountUM as? AmountState.Data)?.tokenIconState ?: CurrencyIconState.Loading
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
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
        AmountInfoMain(
            amountUM = amountUM,
            modifier = Modifier.weight(1f),
        )
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
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag(SendScreenTestTags.MAX_BUTTON),
        )
    }
}

@Composable
private fun AmountInfoMain(amountUM: AmountState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = amountUM,
        modifier = modifier,
        contentKey = { amountUM.javaClass }, // Show animation only when the state type changes [Empty -> Data]
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { currentAmount ->
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when (currentAmount) {
                is AmountState.Data -> {
                    Text(
                        text = currentAmount.tokenName.resolveReference(),
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.primary1,
                        maxLines = 1,
                        modifier = Modifier.testTag(SendScreenTestTags.TOKEN_NAME),
                    )
                    Row {
                        EllipsisText(
                            text = currentAmount.availableBalanceCrypto.resolveReference(),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                            ellipsis = TextEllipsis.OffsetEnd(
                                currentAmount.amountTextField.cryptoAmount.currencySymbol.length,
                            ),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag(SendScreenTestTags.PRIMARY_AMOUNT),
                        )
                        EllipsisText(
                            text = currentAmount.availableBalanceFiat.resolveReference(),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                            ellipsis = TextEllipsis.OffsetEnd(
                                currentAmount.amountTextField.fiatAmount.currencySymbol.length,
                            ),
                            modifier = Modifier.testTag(SendScreenTestTags.SECONDARY_AMOUNT),
                        )
                    }
                }
                AmountState.Empty -> {
                    TextShimmer(
                        style = TangemTheme.typography.subtitle2,
                        modifier = Modifier.width(56.dp),
                    )
                    TextShimmer(
                        style = TangemTheme.typography.caption2,
                        modifier = Modifier.width(72.dp),
                    )
                }
            }
        }
    }
}