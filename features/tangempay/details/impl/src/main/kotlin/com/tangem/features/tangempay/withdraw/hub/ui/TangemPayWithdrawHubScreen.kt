package com.tangem.features.tangempay.withdraw.hub.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.AvatarAccent
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentRecipientUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUMFactory
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayWithdrawHubScreen(state: TangemPayWithdrawHubUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        WithdrawHubTopBar(onBackClick = state.onBackClick)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SwapSection(state = state.swapSection)
            BankSection(state = state.bankSection)
        }
    }
}

@Composable
private fun WithdrawHubTopBar(onBackClick: () -> Unit) {
    TangemTopBar(
        modifier = Modifier.statusBarsPadding(),
        title = resourceReference(R.string.tangempay_card_details_withdraw),
        startContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(Icons.ic_chevron_left_20),
                onClick = onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
    )
}

@Composable
private fun SwapSection(state: TangemPayWithdrawHubUM.SwapSectionUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HubSectionHeader(
            title = resourceReference(R.string.tangempay_withdraw_hub_swap_section_title),
            subtitle = resourceReference(R.string.tangempay_withdraw_hub_swap_section_subtitle),
        )
        HubActionCard(
            state = state.withinPortfolio,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        )
    }
}

@Composable
internal fun HubSectionHeader(title: TextReference, subtitle: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun HubActionCard(state: TangemPayWithdrawHubUM.ActionCardUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(156.dp)
            .heightIn(min = 136.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .clickable(onClick = state.onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(color = TangemTheme.colors3.bg.brand, shape = CircleShape),
        ) {
            TangemIcon(
                modifier = Modifier.size(20.dp),
                tangemIconUM = state.icon,
            )
        }
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private class RecentsPreviewProvider : PreviewParameterProvider<RecentsUM> {
    override val values: Sequence<RecentsUM> = sequenceOf(
        RecentsUM.Content(
            recipients = persistentListOf(
                RecentRecipientUM(
                    id = "1",
                    initial = "A",
                    accent = AvatarAccent.VIOLET,
                    name = "Andrei M.",
                    accountNumber = "0000000000",
                ),
                RecentRecipientUM(
                    id = "2",
                    initial = "J",
                    accent = AvatarAccent.YELLOW,
                    name = "Johnny S.",
                    accountNumber = "0000000000",
                ),
            ),
            onRecipientClick = {},
            onSeeAllClick = {},
        ),
        RecentsUM.Loading,
        RecentsUM.Empty(onSeeAllClick = null),
        RecentsUM.Empty(onSeeAllClick = {}),
        RecentsUM.Error(onRetryClick = {}),
    )
}

private fun previewState(recents: RecentsUM): TangemPayWithdrawHubUM {
    val state = TangemPayWithdrawHubUMFactory(
        callbacks = TangemPayWithdrawHubUMFactory.Callbacks(
            onBackClick = {},
            onWithinPortfolioClick = {},
            onToPersonalAccountClick = {},
            onToBusinessAccountClick = {},
            onRecentRecipientClick = {},
            onSeeAllClick = {},
            onRetryRecentsClick = {},
        ),
    ).create()
    return state.copy(bankSection = state.bankSection.copy(recents = recents))
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayWithdrawHubScreenPreview(@PreviewParameter(RecentsPreviewProvider::class) recents: RecentsUM) {
    TangemThemePreviewRedesign {
        TangemPayWithdrawHubScreen(state = previewState(recents))
    }
}