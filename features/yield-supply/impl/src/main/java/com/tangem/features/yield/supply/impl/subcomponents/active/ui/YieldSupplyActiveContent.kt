package com.tangem.features.yield.supply.impl.subcomponents.active.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun YieldSupplyActiveContent(
    state: YieldSupplyActiveContentUM,
    isBalanceHidden: Boolean,
    onReadMoreClick: () -> Unit,
    chartComponent: ComposableContentComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.padding(
            vertical = 8.dp,
            horizontal = 16.dp,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(TangemTheme.colors.background.action)
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            CurrentApy(state.apy)
            chartComponent.Content(Modifier)
        }

        AnimatedVisibility(state.notifications.isNotEmpty()) {
            val notifications = remember(state.notifications) { state.notifications }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                notifications.forEach { notificationUM ->
                    Notification(
                        config = notificationUM.config,
                        containerColor = TangemTheme.colors.background.action,
                        iconTint = if (notificationUM is NotificationUM.Info) {
                            TangemTheme.colors.icon.accent
                        } else {
                            null
                        },
                    )
                }
            }
        }

        YieldSupplyActiveMyFunds(
            state = state,
            isBalanceHidden = isBalanceHidden,
            onReadMoreClick = onReadMoreClick,
        )

        AnimatedVisibility(state.feeDescription != null) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = state.feeDescription?.resolveReference().orEmpty(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        AnimatedVisibility(state.minFeeDescription != null) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = state.minFeeDescription?.resolveReference().orEmpty(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun CurrentApy(apy: TextReference?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier,
            text = stringResourceSafe(R.string.yield_module_earn_sheet_current_apy_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        AnimatedContent(
            modifier = Modifier.height(32.dp),
            targetState = apy?.resolveReference(),
            label = "CurrentApy",
        ) { apyText ->
            if (apyText == null) {
                TextShimmer(
                    modifier = modifier.width(94.dp),
                    text = "",
                    style = TangemTheme.typography.h2,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_up_8),
                        tint = TangemTheme.colors.text.accent,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(12.dp),
                    )
                    Text(
                        modifier = modifier,
                        text = apyText,
                        style = TangemTheme.typography.h2,
                        color = TangemTheme.colors.text.accent,
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun YieldSupplyActiveMyFunds(
    state: YieldSupplyActiveContentUM,
    onReadMoreClick: () -> Unit,
    isBalanceHidden: Boolean,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(
                top = 12.dp,
                start = 12.dp,
                end = 12.dp,
            ),
    ) {
        Text(
            text = stringResourceSafe(R.string.yield_module_earn_sheet_my_funds_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        SpacerH4()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_aave_36),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = state.providerTitle.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )
        }
        SpacerH8()
        DescriptionText(state = state, onReadMoreClick = onReadMoreClick)
        SpacerH8()
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        InfoRow(
            title = resourceReference(R.string.yield_module_earn_sheet_transfers_title),
            info = resourceReference(R.string.yield_module_transfer_mode_automatic),
            isBalanceHidden = false,
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        InfoRow(
            title = resourceReference(R.string.yield_module_earn_sheet_available_title),
            info = state.availableBalance,
            isBalanceHidden = isBalanceHidden,
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        InfoRow(
            title = resourceReference(R.string.yield_module_fee_policy_sheet_min_amount_title),
            info = state.minAmount,
            isBalanceHidden = false,
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        HighComissionInfoRow(
            title = resourceReference(R.string.common_estimated_fee),
            info = state.currentFee,
            isHighComission = state.isHighFee,
        )
    }
}

@Composable
private fun DescriptionText(state: YieldSupplyActiveContentUM, onReadMoreClick: () -> Unit) {
    val subtitle = annotatedReference {
        append(state.subtitle.resolveReference())
        appendSpace()
        withLink(
            LinkAnnotation.Clickable(
                tag = "LINK_TAG",
                linkInteractionListener = { onReadMoreClick() },
            ),
            {
                appendColored(
                    text = state.subtitleLink.resolveReference(),
                    color = TangemTheme.colors.icon.accent,
                )
            },
        )
    }
    Text(
        text = subtitle.resolveAnnotatedReference(),
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun InfoRow(title: TextReference, isBalanceHidden: Boolean, info: TextReference?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()

        AnimatedContent(info) { currentInfo ->
            if (currentInfo != null) {
                Text(
                    text = currentInfo.orMaskWithStars(isBalanceHidden).resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.End,
                )
            } else {
                TextShimmer(
                    text = title.resolveReference(),
                    style = TangemTheme.typography.body1,
                )
            }
        }
    }
}

@Composable
private fun HighComissionInfoRow(title: TextReference, info: TextReference?, isHighComission: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()

        AnimatedContent(info) { currentInfo ->
            if (currentInfo != null) {
                if (isHighComission) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_token_info_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = TangemTheme.colors.text.warning,
                        )
                        Text(
                            text = currentInfo.resolveReference(),
                            style = TangemTheme.typography.body1,
                            color = TangemTheme.colors.text.warning,
                            textAlign = TextAlign.End,
                        )
                    }
                } else {
                    Text(
                        text = currentInfo.resolveReference(),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.tertiary,
                        textAlign = TextAlign.End,
                    )
                }
            } else {
                TextShimmer(
                    text = title.resolveReference(),
                    style = TangemTheme.typography.body1,
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyActiveBottomSheet_Preview(
    @PreviewParameter(YieldSupplyActiveBottomSheetPreviewProvider::class) params: YieldSupplyActiveContentUM,
) {
    TangemThemePreview {
        YieldSupplyActiveContent(
            state = params,
            isBalanceHidden = true,
            chartComponent = ComposableContentComponent.EMPTY,
            onReadMoreClick = {},
        )
    }
}

private class YieldSupplyActiveBottomSheetPreviewProvider : PreviewParameterProvider<YieldSupplyActiveContentUM> {
    override val values: Sequence<YieldSupplyActiveContentUM>
        get() = sequenceOf(
            YieldSupplyActiveContentUM(
                totalEarnings = stringReference("0.006994219 USDT"),
                availableBalance = stringReference("3,210.006994 aUSDT"),
                providerTitle = stringReference("Aave"),
                subtitle = resourceReference(
                    R.string.yield_module_earn_sheet_provider_description,
                    wrappedList("USDT", "USDT"),
                ),
                subtitleLink = resourceReference(R.string.common_read_more),
                notifications = persistentListOf(
                    NotificationUM.Error.InvalidAmount,
                    NotificationUM.Info(
                        title = resourceReference(
                            R.string.yield_module_amount_not_transfered_to_aave_title,
                            wrappedList(
                                "0,03",
                            ),
                        ),
                        subtitle = TextReference.EMPTY,
                        iconTint = NotificationConfig.IconTint.Accent,
                    ),
                ),
                minAmount = stringReference("50 USDT"),
                currentFee = stringReference("30 USDT"),
                feeDescription = stringReference(
                    "The network fee is currently too high to execute lending." +
                        "Funds will be supplied once it drops to \$12 or below. ",
                ),
                apy = stringReference("5,14%"),
                isHighFee = true,
                minFeeDescription = stringReference(
                    "The network fee is currently too high to execute lending." +
                        "Funds will be supplied once it drops to \$12 or below. ",
                ),
            ),
        )
}
// endregion