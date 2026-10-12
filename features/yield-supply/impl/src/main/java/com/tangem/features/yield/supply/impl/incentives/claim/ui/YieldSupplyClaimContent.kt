package com.tangem.features.yield.supply.impl.incentives.claim.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountLabel
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.ui.YieldSupplyFeesCard
import com.tangem.features.yield.supply.impl.incentives.claim.entity.YieldSupplyClaimUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun YieldSupplyClaimContent(
    state: YieldSupplyClaimUM,
    notificationsComponent: ComposableContentComponent,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        // The title and subtitle live in the sheet's own header, which stays put while this block scrolls.
        SpacerH24()
        Text(
            text = state.total.resolveReference(),
            style = TangemTheme.typography.h1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerH(48.dp)

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.groups.forEach { group ->
                RewardGroup(group = group)
            }

            if (state.warning == YieldSupplyClaimUM.Warning.RewardsUnavailable) {
                Notification(
                    config = rewardsUnavailableConfig(onRetryClick),
                    containerColor = TangemTheme.colors.background.action,
                )
            }

            notificationsComponent.Content(Modifier)

            YieldSupplyFeesCard(
                serviceFee = state.serviceFee,
                networkFee = state.networkFee,
                networkFeeCurrency = state.networkFeeCurrency,
                isServiceFeeVisible = state.isServiceFeeVisible,
            )

            Text(
                text = stringResourceSafe(R.string.yield_module_claim_footer_note),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
            )
        }
    }
}

@Composable
private fun RewardGroup(group: YieldSupplyClaimUM.RewardGroupUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = group.title.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.primary1,
            )
            group.storage?.let { storage ->
                SpacerWMax()
                AccountLabel(
                    name = storage.name,
                    icon = storage.icon,
                    iconSize = AccountIconSize.ExtraSmall,
                    nameStyle = TangemTheme.typography.caption2,
                    nameColor = TangemTheme.colors.text.tertiary,
                )
            }
        }
        group.rewards.forEachIndexed { index, reward ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = TangemTheme.colors.stroke.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            RewardRow(reward = reward)
        }
    }
}

@Composable
private fun RewardRow(reward: YieldSupplyClaimUM.RewardUM) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        CurrencyIcon(
            state = reward.icon,
            shouldDisplayNetwork = true,
            modifier = Modifier.size(40.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = reward.symbol,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = reward.networkName,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        SpacerWMax()
        // The USD value leads and the token amount follows it: the claim is read as money first, and an
        // unroutable reward has no USD value to lead with.
        Column(horizontalAlignment = Alignment.End) {
            reward.fiatAmount?.let { fiat ->
                Text(
                    text = fiat.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
            Text(
                text = reward.amount.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun rewardsUnavailableConfig(onRetryClick: () -> Unit) = NotificationConfig(
    title = resourceReference(R.string.yield_module_claim_rewards_unavailable_notification_title),
    subtitle = resourceReference(R.string.yield_module_claim_rewards_unavailable_notification_description),
    iconResId = R.drawable.ic_alert_24,
    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
        text = resourceReference(R.string.common_reload),
        onClick = onRetryClick,
    ),
)

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyClaimContentPreview() {
    TangemThemePreview {
        YieldSupplyClaimContent(
            state = YieldSupplyClaimUM(
                total = stringReference("$30.00"),
                groups = persistentListOf(
                    YieldSupplyClaimUM.RewardGroupUM(
                        title = resourceReference(R.string.yield_module_claim_destination_owner_title),
                        rewards = persistentListOf(
                            YieldSupplyClaimUM.RewardUM(
                                icon = CurrencyIconState.Loading,
                                symbol = "WETH",
                                networkName = "Ethereum",
                                amount = stringReference("WETH 0.85"),
                                fiatAmount = stringReference("$30.00"),
                            ),
                        ),
                    ),
                ),
                serviceFee = stringReference("(15%)\u00A0$0.15"),
                networkFee = stringReference("$2.00"),
                networkFeeCurrency = stringReference("ETH"),
                isServiceFeeVisible = true,
                warning = null,
                isClaimEnabled = true,
                isClaimInProgress = false,
            ),
            notificationsComponent = ComposableContentComponent.EMPTY,
            onRetryClick = {},
        )
    }
}