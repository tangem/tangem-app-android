package com.tangem.features.walletconnect.transaction.ui.blockaid

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.divider.DividerWithPadding
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangeUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.ui.common.WcSmallTitleItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

private const val MAX_CHANGES_SIZE = 4

@Composable
internal fun WcEstimatedWalletChangesItem(item: WcEstimatedWalletChangesUM, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color = TangemTheme.colors.background.action)
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        WcSmallTitleItem(
            textRex = R.string.wc_estimated_wallet_changes,
            modifier = Modifier
                .padding(bottom = 16.dp),
        )

        val itemsToDisplay = if (isExpanded) item.items else item.items.take(MAX_CHANGES_SIZE)

        itemsToDisplay.forEachIndexed { idx, row ->
            key(row.hashCode()) {
                WcEstimatedWalletChangeRow(
                    item = row,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                if (idx == item.items.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    DividerWithPadding(start = 48.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                }
            }
        }

        if (item.items.size > MAX_CHANGES_SIZE) {
            val interactionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { isExpanded = !isExpanded },
                    )
                    .padding(bottom = 12.dp, start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResourceSafe(if (isExpanded) R.string.common_show_less else R.string.common_show_more),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                )
                Icon(
                    painter = rememberVectorPainter(
                        image = ImageVector.vectorResource(id = R.drawable.ic_chevron_24),
                    ),
                    tint = TangemTheme.colors.icon.informative,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(start = 2.dp),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EstimatedWalletChangesPreviewTwoItems(
    @PreviewParameter(EstimatedWalletChangesPreviewProviderTwoItems::class) item: WcEstimatedWalletChangesUM,
) {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            WcEstimatedWalletChangesItem(item = item)
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EstimatedWalletChangesPreviewMoreThanFour(
    @PreviewParameter(EstimatedWalletChangesPreviewProviderManyItems::class) item: WcEstimatedWalletChangesUM,
) {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary),
        ) {
            WcEstimatedWalletChangesItem(item = item)
        }
    }
}

private class EstimatedWalletChangesPreviewProviderTwoItems : PreviewParameterProvider<WcEstimatedWalletChangesUM> {
    override val values = sequenceOf(
        WcEstimatedWalletChangesUM(
            items = persistentListOf(
                WcEstimatedWalletChangeUM(
                    iconRes = R.drawable.ic_send_new_24,
                    title = resourceReference(R.string.common_send),
                    description = "- 42 USDT",
                    tokenIconUrl = "https://tangem.com",
                ),
                WcEstimatedWalletChangeUM(
                    iconRes = R.drawable.ic_receive_new_24,
                    title = resourceReference(R.string.common_receive),
                    description = "+ 1,131.46 MATIC",
                    tokenIconUrl = "https://tangem.com",
                ),
                WcEstimatedWalletChangeUM(
                    iconRes = R.drawable.img_approvale_new_24,
                    title = resourceReference(R.string.common_approve),
                    description = "10 Collection",
                    tokenIconUrl = "https://cdn.blockaid.io/nft/0x09851531816f78cF4841f1DeF22fbaB78aDD02c5/29805/" +
                        "polygon?r=ed117da0-6065-4ff8-ba81-cb7395e1ec3d",
                ),
            ),
        ),
    )
}

private class EstimatedWalletChangesPreviewProviderManyItems :
    PreviewParameterProvider<WcEstimatedWalletChangesUM> {
    override val values = sequenceOf(
        WcEstimatedWalletChangesUM(
            items = (1..6).map {
                WcEstimatedWalletChangeUM(
                    iconRes = R.drawable.ic_send_new_24,
                    title = resourceReference(R.string.common_send),
                    description = "Nethers #1111",
                    tokenIconUrl = "https://tangem.com",
                )
            }.toPersistentList(),
        ),
    )
}