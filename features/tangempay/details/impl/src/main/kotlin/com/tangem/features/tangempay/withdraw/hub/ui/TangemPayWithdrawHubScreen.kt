package com.tangem.features.tangempay.withdraw.hub.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_chevron_left_20
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM

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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HubSectionHeader(
                title = resourceReference(R.string.tangempay_withdraw_hub_swap_section_title),
                subtitle = resourceReference(R.string.tangempay_withdraw_hub_swap_section_subtitle),
            )
            HubActionCard(state = state.swapSection.withinPortfolio)
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
private fun HubSectionHeader(title: TextReference, subtitle: TextReference) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayWithdrawHubScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayWithdrawHubScreen(
            state = TangemPayWithdrawHubUM(
                swapSection = TangemPayWithdrawHubUM.SwapSectionUM(
                    withinPortfolio = TangemPayWithdrawHubUM.ActionCardUM(
                        icon = TangemIconUM.Icon(
                            imageVector = Icons.ic_arrow_swap_horizontal_20,
                            tintReference = { TangemTheme.colors3.icon.staticDark },
                        ),
                        title = resourceReference(R.string.tangempay_withdraw_hub_within_portfolio),
                        onClick = {},
                    ),
                ),
                onBackClick = {},
            ),
        )
    }
}