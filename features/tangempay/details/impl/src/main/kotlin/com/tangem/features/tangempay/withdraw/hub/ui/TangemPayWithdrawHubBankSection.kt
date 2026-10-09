package com.tangem.features.tangempay.withdraw.hub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_dots_horizontal_20
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM

@Composable
internal fun BankSection(state: BankSectionUM, modifier: Modifier = Modifier) {
    val isAvailable = (state as? BankSectionUM.Content)?.isAvailable != false
    Column(modifier = modifier.fillMaxWidth()) {
        HubSectionHeader(
            title = resourceReference(R.string.tangempay_withdraw_hub_bank_section_title),
            subtitle = if (isAvailable) {
                resourceReference(R.string.tangempay_withdraw_hub_bank_section_subtitle)
            } else {
                resourceReference(R.string.tangempay_withdraw_hub_bank_section_subtitle_unavailable)
            },
            subtitleColor = if (isAvailable) {
                TangemTheme.colors3.text.secondary
            } else {
                TangemTheme.colors3.text.status.warning
            },
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isAvailable) 1f else UNAVAILABLE_ALPHA)
                .padding(top = 4.dp, bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state) {
                BankSectionUM.Loading -> loadingItems()
                is BankSectionUM.Content -> {
                    item(key = "bank_actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BankActionCard(state = state.toPersonalAccount, isEnabled = state.isAvailable)
                            BankActionCard(state = state.toBusinessAccount, isEnabled = state.isAvailable)
                        }
                    }
                    recentsItems(recents = state.recents, isEnabled = state.isAvailable)
                }
            }
        }
    }
}

private fun LazyListScope.loadingItems() {
    item(key = "bank_actions_shimmer") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(times = 2) {
                TangemShimmer(
                    modifier = Modifier.size(width = CardWidth, height = ActionCardHeight),
                    radius = ActionCardCornerRadius,
                )
            }
        }
    }
    recentsShimmerItems(keyPrefix = "recents_shimmer")
}

private fun LazyListScope.recentsItems(recents: RecentsUM, isEnabled: Boolean) {
    when (recents) {
        RecentsUM.Loading -> recentsShimmerItems(keyPrefix = "recents_shimmer")
        RecentsUM.Empty -> item(key = "recents_empty") { RecentsEmptyCard() }
        is RecentsUM.Error -> item(key = "recents_error") {
            UnableToLoadCard(onRetryClick = recents.onRetryClick.takeIf { isEnabled })
        }
        is RecentsUM.Content -> {
            items(items = recents.recipients, key = { it.id }) { recipient ->
                RecentRecipientCard(
                    state = recipient,
                    onClick = { recents.onRecipientClick(recipient.id) }.takeIf { isEnabled },
                )
            }
            recents.onSeeAllClick?.let { onSeeAllClick ->
                item(key = "recents_see_all") { SeeAllCard(onClick = onSeeAllClick.takeIf { isEnabled }) }
            }
        }
    }
}

@Composable
private fun BankActionCard(state: TangemPayWithdrawHubUM.ActionCardUM, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .width(CardWidth)
            .clip(RoundedCornerShape(ActionCardCornerRadius))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .clickableSingle(enabled = isEnabled, role = Role.Button, onClick = state.onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(background = TangemTheme.colors3.bg.brand) {
            TangemIcon(modifier = Modifier.size(16.dp), tangemIconUM = state.icon)
        }
        Text(
            modifier = Modifier.weight(1f),
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentRecipientCard(state: TangemPayWithdrawHubUM.RecentRecipientUM, onClick: (() -> Unit)?) {
    RecentsCard(onClick = onClick) {
        RecipientAvatar(initial = state.initial)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.name,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.accountNumber,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeeAllCard(onClick: (() -> Unit)?) {
    RecentsCard(onClick = onClick) {
        SecondaryIconCircle(imageVector = Icons.ic_dots_horizontal_20)
        Text(
            text = resourceReference(R.string.common_see_all).resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnableToLoadCard(onRetryClick: (() -> Unit)?) {
    RecentsCard(isDashed = true, onClick = onRetryClick) {
        IconCircle(background = TangemTheme.colors3.bg.inverse) {
            TangemIcon(
                modifier = Modifier.size(20.dp),
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_refresh_20,
                    tintReference = { TangemTheme.colors3.icon.inverse },
                ),
            )
        }
        Text(
            text = resourceReference(R.string.tangempay_withdraw_hub_accounts_load_error).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecipientAvatar(initial: String) {
    IconCircle(background = TangemTheme.colors3.bg.opaque.secondary) {
        Text(
            text = initial,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

private const val UNAVAILABLE_ALPHA = 0.4f