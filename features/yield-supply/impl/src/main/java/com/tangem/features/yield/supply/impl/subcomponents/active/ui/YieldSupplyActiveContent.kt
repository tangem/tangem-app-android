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
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.subcomponents.active.entity.YieldSupplyActiveContentUM

@Composable
internal fun YieldSupplyActiveContent(
    state: YieldSupplyActiveContentUM,
    isBalanceHidden: Boolean,
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
            Text(
                text = stringResourceSafe(R.string.yield_module_earn_sheet_total_earnings_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            ResizableText(
                text = state.totalEarnings.orMaskWithStars(isBalanceHidden).resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )

            CurrentApy(state.apy)
            chartComponent.Content(Modifier.padding(bottom = 12.dp))
        }
        YieldSupplyActiveMyFunds(state = state, isBalanceHidden = isBalanceHidden)

        AnimatedVisibility(state.notificationUM != null) {
            val wrappedNotification = remember(this) { requireNotNull(state.notificationUM) }
            Notification(
                config = wrappedNotification.config,
                iconTint = null,
                containerColor = TangemTheme.colors.background.action,
            )
        }
    }
}

@Composable
private fun CurrentApy(apy: TextReference?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = modifier.weight(1.0f),
            text = stringResourceSafe(R.string.yield_module_earn_sheet_current_apy_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.tertiary,
        )
        AnimatedContent(
            targetState = apy?.resolveReference(),
            label = "CurrentApy",
        ) { apyText ->
            if (apyText == null) {
                TextShimmer(
                    modifier = modifier.width(56.dp),
                    text = "",
                    style = TangemTheme.typography.body1,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_arrow_up_8),
                        tint = TangemTheme.colors.text.accent,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        modifier = modifier,
                        text = apyText,
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun YieldSupplyActiveMyFunds(state: YieldSupplyActiveContentUM, isBalanceHidden: Boolean) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.action)
            .fillMaxWidth()
            .padding(12.dp),
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
        DescriptionText(state = state)
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
    }
}

@Composable
private fun DescriptionText(state: YieldSupplyActiveContentUM) {
    val subtitle = annotatedReference {
        append(state.subtitle.resolveReference())
        appendSpace()
        withLink(
            LinkAnnotation.Clickable(
                tag = "LINK_TAG",
                linkInteractionListener = {},
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
                notificationUM = NotificationUM.Error.InvalidAmount,
            ),
        )
}
// endregion