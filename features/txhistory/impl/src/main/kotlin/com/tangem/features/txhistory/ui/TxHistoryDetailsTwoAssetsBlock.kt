package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.TangemCurrencyIcon
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.AssetOwnerUM
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.AssetUM

/**
 * Two-asset ("exchange") block of the details card, used by Swap and Onramp: one `bg.tertiary` rounded cell
 * with the [from] side over the [to] side, split by an inset dashed divider with a centered down-arrow masking the
 * line. Each side is a [TangemRow]: label (with an optional owner) over the signed amount, currency icon trailing.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=1265-87546)
 *
 * @param from Sent ("You sent" / "From …") side.
 * @param to Received ("You receive" / "To …") side.
 * @param modifier Modifier applied to the block container.
 */
@Composable
internal fun TxHistoryDetailsTwoAssetsBlock(from: AssetUM, to: AssetUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TwoAssetsSideRow(asset = from)
            DashedDivider()
            TwoAssetsSideRow(asset = to)
        }
        // Centered exchange arrow. Both rows are equal-height, so the block center sits on the divider; the
        // `bg.tertiary` chip behind the icon masks the dashed line, reproducing the Figma center gap.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.tertiary)
                .padding(4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TwoAssetsSideRow(asset: AssetUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { TwoAssetsSideLabel(label = asset.label, owner = asset.owner) },
        subtitleSlot = {
            Text(
                text = asset.amount.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = if (asset.isFaded) {
                    TangemTheme.colors3.text.tertiary
                } else {
                    TangemTheme.colors3.text.primary
                },
                textDecoration = if (asset.isFaded) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        },
        endSlot = {
            // The fiat leg of an onramp carries no icon (no CryptoCurrency, no country flag) — leave the slot empty.
            asset.currencyIcon?.let { icon ->
                TangemCurrencyIcon(
                    state = icon,
                    modifier = Modifier.size(40.dp),
                )
            }
        },
    )
}

/**
 * Caption label above a leg amount. Renders the [label] prefix ("You sent" / "You receive" / "You paid", or "From" /
 * "To" when an [owner] is present) and, for a resolved [owner], its inline 16dp decoration in the Figma order — the
 * account avatar and the external-address identicon lead their name, the wallet key-card icon trails its name.
 */
@Composable
private fun TwoAssetsSideLabel(label: TextReference, owner: AssetOwnerUM?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabelText(text = label)
        when (owner) {
            is AssetOwnerUM.Account,
            is AssetOwnerUM.Address,
            -> {
                AssetOwnerIcon(owner = owner)
                LabelText(text = owner.name, modifier = Modifier.weight(weight = 1f, fill = false))
            }
            is AssetOwnerUM.Wallet -> {
                LabelText(text = owner.name, modifier = Modifier.weight(weight = 1f, fill = false))
                AssetOwnerIcon(owner = owner)
            }
            null -> Unit
        }
    }
}

@Composable
private fun LabelText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** 16dp inline owner decoration: the account glyph over its color, the wallet device card, or an address identicon. */
@Composable
private fun AssetOwnerIcon(owner: AssetOwnerUM, modifier: Modifier = Modifier) {
    val iconModifier = modifier.size(16.dp)
    when (owner) {
        is AssetOwnerUM.Account -> Box(
            modifier = iconModifier
                .clip(RoundedCornerShape(4.dp))
                .background(owner.backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = owner.iconResId),
                contentDescription = null,
                // staticDark == white in both themes (the constant glyph tone for a colored avatar), matching the
                // white-on-color account avatar in Figma and the counterparty card / history-list account icon.
                tint = TangemTheme.colors3.icon.staticDark,
                modifier = Modifier.size(8.dp),
            )
        }
        is AssetOwnerUM.Wallet -> TangemDeviceIcon(
            state = owner.deviceIconUM,
            modifier = iconModifier,
        )
        is AssetOwnerUM.Address -> IdentIcon(
            address = owner.rawAddress,
            modifier = iconModifier.clip(CircleShape),
        )
    }
}

/** 1px inset dashed divider between the two sides, matching the Figma `divider` (dashed `line`). */
@Composable
private fun DashedDivider(modifier: Modifier = Modifier) {
    val color = TangemTheme.colors3.border.tertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height / 2f
                drawLine(
                    color = color,
                    start = Offset(x = 0f, y = y),
                    end = Offset(x = size.width, y = y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(2.dp.toPx(), 4.dp.toPx()),
                    ),
                )
            },
    )
}

// region Preview

@Suppress("MagicNumber", "LongMethod")
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsTwoAssetsBlockPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Plain swap (no resolved owner) — both sides settled.
            TxHistoryDetailsTwoAssetsBlock(
                from = previewAsset(label = "You sent", amount = "- 390 USDT", isFaded = false),
                to = previewAsset(label = "You receive", amount = "+ 1,800.00 POL", isFaded = false),
            )
            // Unsettled swap — the "You receive" side shows the estimated amount with a `~` until the funds arrive
            // (struck through is reserved for the failed state).
            TxHistoryDetailsTwoAssetsBlock(
                from = previewAsset(label = "You sent", amount = "- 390 USDT", isFaded = false),
                to = previewAsset(label = "You receive", amount = "~ 1,800.00 POL", isFaded = false),
            )
            // Account -> another account (own-to-own transfer between two of the user's accounts).
            TxHistoryDetailsTwoAssetsBlock(
                from = previewAsset(
                    label = "From",
                    amount = "- 390 USDT",
                    isFaded = false,
                    owner = AssetOwnerUM.Account(
                        name = stringReference("Main account"),
                        iconResId = R.drawable.ic_rounded_star_24,
                        backgroundColor = Color(0xFF007FFF),
                    ),
                ),
                to = previewAsset(
                    label = "To",
                    amount = "+ 1,800.00 POL",
                    isFaded = false,
                    owner = AssetOwnerUM.Account(
                        name = stringReference("Family"),
                        iconResId = R.drawable.ic_family_24,
                        backgroundColor = Color(0xFF744FF1),
                    ),
                ),
            )
            // Wallet -> another wallet (own-to-own transfer between two of the user's wallets).
            TxHistoryDetailsTwoAssetsBlock(
                from = previewAsset(
                    label = "From",
                    amount = "- 390 USDT",
                    isFaded = false,
                    owner = AssetOwnerUM.Wallet(
                        name = stringReference("Tangem 2.0"),
                        deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                    ),
                ),
                to = previewAsset(
                    label = "To",
                    amount = "+ 1,800.00 POL",
                    isFaded = false,
                    owner = AssetOwnerUM.Wallet(
                        name = stringReference("My Wallet"),
                        deviceIconUM = DeviceIconUM.Ring(mainColor = Color(0xFF9F86FF)),
                    ),
                ),
            )
            // Send-and-swap — the payout went to an external (non-user) address.
            TxHistoryDetailsTwoAssetsBlock(
                from = previewAsset(
                    label = "From",
                    amount = "- 390 USDT",
                    isFaded = false,
                    owner = AssetOwnerUM.Wallet(
                        name = stringReference("Tangem 2.0"),
                        deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                    ),
                ),
                to = previewAsset(
                    label = "To",
                    amount = "+ 1,800.00 POL",
                    isFaded = false,
                    owner = AssetOwnerUM.Address(
                        name = stringReference("33Bd3…a21412B"),
                        rawAddress = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359",
                    ),
                ),
            )
        }
    }
}

private fun previewAsset(label: String, amount: String, isFaded: Boolean, owner: AssetOwnerUM? = null) = AssetUM(
    label = stringReference(label),
    owner = owner,
    amount = stringReference(amount),
    currencyIcon = CurrencyIconState.CoinIcon(
        url = null,
        fallbackResId = R.drawable.img_eth_22,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    ),
    isFaded = isFaded,
)

// endregion