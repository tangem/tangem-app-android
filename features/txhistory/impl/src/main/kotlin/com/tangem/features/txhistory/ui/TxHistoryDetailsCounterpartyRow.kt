package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.CounterpartyAvatar
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.CounterpartyUM
import com.tangem.features.txhistory.impl.R

/**
 * Counterparty ("Recipient" / "From") card of the single-asset detail, built on the DS3 [TangemRow] inside a tinted
 * `bg.opaque.primary` cell. The layout is identical across counterparty kinds — only the leading
 * [avatar][CounterpartyUM.avatar] varies (see [CounterpartyAvatar]) and the trailing copy button is shown only when
 * [CounterpartyUM.onCopyClick] is non-null.
 *
 * The section [label][CounterpartyUM.label] sits above the counterparty value. [TangemRow] renders its `titleSlot`
 * above the `subtitleSlot`, so the slots are filled inverted to their semantic role: the small caption label goes in
 * the (upper) title slot and the body-sized value goes in the (lower) subtitle slot.
 *
 * @param counterparty Counterparty data driving the avatar, labels and the copy action.
 * @param modifier Modifier applied to the cell container.
 */
@Composable
internal fun TxHistoryDetailsCounterpartyRow(counterparty: CounterpartyUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary),
    ) {
        TangemRow(
            verticalAlignment = TangemRowVerticalAlignment.Center,
            contentLead = TangemRowContentLead.Start,
            startSlot = { CounterpartyAvatar(counterparty.avatar) },
            titleSlot = { TangemRowText(text = counterparty.label, role = TangemRowTextRole.Subtitle) },
            subtitleSlot = { TangemRowText(text = counterparty.title, role = TangemRowTextRole.Title) },
            endSlot = counterparty.onCopyClick?.let { onCopyClick ->
                { CounterpartyCopyButton(onClick = onCopyClick) }
            },
        )
    }
}

@Composable
private fun CounterpartyAvatar(avatar: CounterpartyAvatar, modifier: Modifier = Modifier) {
    val avatarModifier = modifier.size(40.dp)
    when (avatar) {
        is CounterpartyAvatar.Address -> IdentIcon(
            address = avatar.rawAddress,
            modifier = avatarModifier.clip(CircleShape),
        )
        is CounterpartyAvatar.Account -> Box(
            modifier = avatarModifier
                .clip(CircleShape)
                .background(avatar.backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = avatar.iconResId),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.staticDark,
                modifier = Modifier.size(20.dp),
            )
        }
        is CounterpartyAvatar.Wallet -> TangemDeviceIcon(
            state = avatar.deviceIconUM,
            modifier = avatarModifier,
        )
    }
}

@Composable
private fun CounterpartyCopyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X9,
        iconStart = TangemIconUM.Icon(Icons.ic_copy_20),
        contentDescription = resourceReference(R.string.common_copy).resolveReference(),
        onClick = onClick,
    )
}

// region Preview

@Suppress("MagicNumber")
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsCounterpartyRowPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = stringReference("33Bd321fS...ga21412B"),
                    avatar = CounterpartyAvatar.Address(rawAddress = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359"),
                    onCopyClick = {},
                ),
            )
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = stringReference("Danil Kolbasenko"),
                    avatar = CounterpartyAvatar.Account(
                        iconResId = R.drawable.ic_arrow_down_24,
                        backgroundColor = Color(0xFF704AF1),
                    ),
                    onCopyClick = {},
                ),
            )
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = stringReference("Tangem wallet"),
                    avatar = CounterpartyAvatar.Wallet(
                        deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                    ),
                    onCopyClick = null,
                ),
            )
        }
    }
}

// endregion