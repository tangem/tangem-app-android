@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.transactionrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.transactions.INLINE_IMAGE_PLACEHOLDER
import com.tangem.core.ui.components.transactions.InlineImagesText
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.expandable.ExpandableDot
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_warning_16_filled
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TransactionRowHeadAvatar(icon: ImageVector, status: TangemTransactionRow.Status) {
    TangemAvatar(
        state = TangemAvatar.UiState.Transaction(imageVector = icon, status = status.toAvatarStatus()),
        size = TangemAvatar.Size.X40,
    )
}

@Composable
internal fun TransactionRowTitle(
    title: TextReference,
    status: TangemTransactionRow.Status,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = status.titleColor(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun TransactionRowAmount(
    amount: TextReference,
    status: TangemTransactionRow.Status,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = amount.orMaskWithStars(isBalanceHidden).resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = status.amountColor(),
        textDecoration = if (status == TangemTransactionRow.Status.Failed && !isBalanceHidden) {
            TextDecoration.LineThrough
        } else {
            null
        },
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
internal fun TransactionRowEndBottom(badge: TextReference?, caption: TextReference?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (badge != null) {
            TangemBadge(
                text = badge,
                variant = TangemBadge.Variant.Tinted,
                status = TangemBadge.Status.Neutral,
                size = TangemBadge.Size.X4,
            )
        }
        if (caption != null) {
            Text(
                text = caption.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun TransactionRowStatusMessage(text: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_warning_16_filled,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.status.warning,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.status.warning,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun TransactionRowDetailModule(
    description: TextReference?,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
        if (description != null) {
            ExpandableDot()
            TransactionRowSubtitleText(
                text = description.resolveReference(),
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
        }
    }
}

@Composable
internal fun TransactionRowEntity(
    template: String,
    avatars: ImmutableList<TangemAvatar.UiState>,
    modifier: Modifier = Modifier,
) {
    val icons = remember(avatars) { avatars.map(::subtitleAvatarIcon) }
    InlineImagesText(
        template = template,
        icons = icons,
        color = TangemTheme.colors3.text.secondary,
        textStyle = TangemTheme.typography3.caption.medium,
        modifier = modifier,
    )
}

@Composable
internal fun TransactionRowAddressEntity(
    template: String,
    address: String,
    avatar: TangemAvatar.UiState,
    modifier: Modifier = Modifier,
) {
    val before = template.substringBefore(address).trim()
    val after = template.substringAfter(address).trim()
    val isAvatarBefore = before.contains(INLINE_IMAGE_PLACEHOLDER)
    val icons = remember(avatar) { persistentListOf(avatar) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (before.isNotEmpty()) {
            TransactionRowEntity(template = before, avatars = if (isAvatarBefore) icons else persistentListOf())
        }
        Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
            EllipsisText(
                text = address,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                ellipsis = TextEllipsis.Middle,
            )
        }
        if (after.isNotEmpty()) {
            TransactionRowEntity(template = after, avatars = if (isAvatarBefore) persistentListOf() else icons)
        }
    }
}

@Composable
internal fun TransactionRowSubtitleText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun subtitleAvatarIcon(avatar: TangemAvatar.UiState): @Composable () -> Unit = {
    TangemAvatar(state = avatar, size = TangemAvatar.Size.X16)
}

private fun TangemTransactionRow.Status.toAvatarStatus(): TangemAvatar.Status = when (this) {
    TangemTransactionRow.Status.Progress -> TangemAvatar.Status.Progress
    TangemTransactionRow.Status.Finished -> TangemAvatar.Status.Finished
    TangemTransactionRow.Status.Failed -> TangemAvatar.Status.Failed
    TangemTransactionRow.Status.Pending -> TangemAvatar.Status.Pending
}

@Composable
@ReadOnlyComposable
private fun TangemTransactionRow.Status.titleColor(): Color {
    val text = TangemTheme.colors3.text
    return when (this) {
        TangemTransactionRow.Status.Progress -> text.status.info
        TangemTransactionRow.Status.Finished -> text.primary
        TangemTransactionRow.Status.Failed -> text.status.error
        TangemTransactionRow.Status.Pending -> text.status.warning
    }
}

@Composable
@ReadOnlyComposable
private fun TangemTransactionRow.Status.amountColor(): Color {
    val text = TangemTheme.colors3.text
    return when (this) {
        TangemTransactionRow.Status.Finished, TangemTransactionRow.Status.Failed -> text.primary
        TangemTransactionRow.Status.Progress, TangemTransactionRow.Status.Pending -> text.secondary
    }
}