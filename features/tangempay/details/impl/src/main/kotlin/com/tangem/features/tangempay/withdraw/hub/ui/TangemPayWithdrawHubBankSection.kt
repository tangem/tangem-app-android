package com.tangem.features.tangempay.withdraw.hub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_heart_20
import com.tangem.core.ui.utils.dashedBorder
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.AvatarAccent
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.BankSectionUM
import com.tangem.features.tangempay.withdraw.hub.state.TangemPayWithdrawHubUM.RecentsUM

private val CardWidth = 156.dp
private val RecentsCardHeight = 136.dp
private val ActionCardHeight = 64.dp
private val RecentsCardCornerRadius = 24.dp
private val ActionCardCornerRadius = 20.dp
private val IconCircleSize = 32.dp
private val DashLength = 6.dp
private val DashGap = 4.dp
private const val SHIMMER_CARDS_COUNT = 3

@Composable
internal fun BankSection(state: BankSectionUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        HubSectionHeader(
            title = resourceReference(R.string.tangempay_withdraw_hub_bank_section_title),
            subtitle = resourceReference(R.string.tangempay_withdraw_hub_bank_section_subtitle),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state) {
                BankSectionUM.Loading -> loadingItems()
                is BankSectionUM.Error -> item(key = "bank_error") {
                    UnableToLoadCard(onRetryClick = state.onRetryClick)
                }
                is BankSectionUM.Content -> {
                    item(key = "bank_actions") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BankActionCard(state = state.toPersonalAccount)
                            BankActionCard(state = state.toBusinessAccount)
                        }
                    }
                    recentsItems(recents = state.recents)
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
    items(count = SHIMMER_CARDS_COUNT, key = { index -> "recents_shimmer_$index" }) {
        TangemShimmer(
            modifier = Modifier.size(width = CardWidth, height = RecentsCardHeight),
            radius = RecentsCardCornerRadius,
        )
    }
}

private fun LazyListScope.recentsItems(recents: RecentsUM) {
    when (recents) {
        RecentsUM.Loading -> items(
            count = SHIMMER_CARDS_COUNT,
            key = { index -> "recents_shimmer_$index" },
        ) {
            TangemShimmer(
                modifier = Modifier.size(width = CardWidth, height = RecentsCardHeight),
                radius = RecentsCardCornerRadius,
            )
        }
        is RecentsUM.Empty -> {
            item(key = "recents_empty") { RecentsEmptyCard() }
            recents.onSeeAllClick?.let { onSeeAllClick ->
                item(key = "recents_see_all") { SeeAllCard(onClick = onSeeAllClick) }
            }
        }
        is RecentsUM.Error -> item(key = "recents_error") { UnableToLoadCard(onRetryClick = recents.onRetryClick) }
        is RecentsUM.Content -> {
            items(items = recents.recipients, key = { it.id }) { recipient ->
                RecentRecipientCard(
                    state = recipient,
                    onClick = { recents.onRecipientClick(recipient.id) },
                )
            }
            item(key = "recents_see_all") { SeeAllCard(onClick = recents.onSeeAllClick) }
        }
    }
}

@Composable
private fun BankActionCard(state: TangemPayWithdrawHubUM.ActionCardUM) {
    Row(
        modifier = Modifier
            .width(CardWidth)
            .clip(RoundedCornerShape(ActionCardCornerRadius))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .clickableSingle(role = Role.Button, onClick = state.onClick)
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
private fun RecentRecipientCard(state: TangemPayWithdrawHubUM.RecentRecipientUM, onClick: () -> Unit) {
    RecentsCard(onClick = onClick) {
        RecipientAvatar(initial = state.initial, accent = state.accent)
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
private fun SeeAllCard(onClick: () -> Unit) {
    RecentsCard(onClick = onClick) {
        SecondaryIconCircle(imageVector = Icons.ic_chevron_right_20)
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
private fun RecentsEmptyCard() {
    RecentsCard(isDashed = true) {
        SecondaryIconCircle(imageVector = Icons.ic_heart_20)
        Text(
            text = resourceReference(R.string.tangempay_withdraw_hub_recents_empty).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnableToLoadCard(onRetryClick: () -> Unit) {
    RecentsCard(isDashed = true, onClick = onRetryClick) {
        SecondaryIconCircle(imageVector = Icons.ic_arrow_refresh_20)
        Text(
            text = resourceReference(R.string.common_unable_to_load).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentsCard(
    isDashed: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RecentsCardCornerRadius)
    val surface = if (isDashed) {
        Modifier
            .dashedBorder(
                color = TangemTheme.colors3.border.secondary,
                shape = shape,
                dashLength = DashLength,
                gapLength = DashGap,
            )
            .clip(shape)
    } else {
        Modifier
            .clip(shape)
            .background(TangemTheme.colors3.bg.secondary)
    }
    val click = if (onClick != null) Modifier.clickableSingle(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier = Modifier
            .size(width = CardWidth, height = RecentsCardHeight)
            .then(surface)
            .then(click)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
private fun RecipientAvatar(initial: String, accent: AvatarAccent) {
    Box(
        modifier = Modifier
            .size(IconCircleSize)
            .background(color = accent.toColor(), shape = CircleShape)
            .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.staticDark.primary,
        )
    }
}

@Composable
private fun SecondaryIconCircle(imageVector: ImageVector) {
    IconCircle(background = TangemTheme.colors3.bg.opaque.primary) {
        TangemIcon(
            modifier = Modifier.size(20.dp),
            tangemIconUM = TangemIconUM.Icon(
                imageVector = imageVector,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}

@Composable
private fun IconCircle(background: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(IconCircleSize)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
@ReadOnlyComposable
private fun AvatarAccent.toColor(): Color {
    val accent = TangemTheme.colors3.bg.accent
    return when (this) {
        AvatarAccent.BLUE -> accent.blue
        AvatarAccent.VIOLET -> accent.violet
        AvatarAccent.RED -> accent.red
        AvatarAccent.ORANGE -> accent.orange
        AvatarAccent.YELLOW -> accent.yellow
        AvatarAccent.GREEN -> accent.green
    }
}