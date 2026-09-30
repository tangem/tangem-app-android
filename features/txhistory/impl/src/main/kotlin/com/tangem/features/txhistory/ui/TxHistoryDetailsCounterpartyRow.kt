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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.domain.models.account.CryptoPortfolioIcon.Color as IconColor
import com.tangem.domain.models.account.CryptoPortfolioIcon.Icon
import com.tangem.core.ui.components.account.PaymentAccountIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowAddressText
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.components.transactions.state.asImageVector
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.CounterpartyAvatar
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.CounterpartyTitle
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM.CounterpartyUM

/**
 * Counterparty ("Recipient" / "From") card of the single-asset detail, built on the DS3 [TangemRow] inside a tinted
 * `bg.opaque.primary` cell. The layout is identical across counterparty kinds — only the leading
 * [avatar][CounterpartyUM.avatar] varies (see [CounterpartyAvatar]). The whole row is clickable and copies the
 * counterparty address via [CounterpartyUM.onCopyClick].
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
            subtitleSlot = {
                when (val title = counterparty.title) {
                    is CounterpartyTitle.Name -> TangemRowText(text = title.value, role = TangemRowTextRole.Title)
                    is CounterpartyTitle.Address -> TangemRowAddressText(
                        address = title.value,
                        role = TangemRowTextRole.Title,
                        modifier = Modifier.weight(weight = 1f, fill = false),
                    )
                }
                counterparty.titleIcon?.let { icon ->
                    CurrencyIcon(
                        state = icon,
                        shouldDisplayNetwork = false,
                        withFixedSize = false,
                        modifier = Modifier.size(17.dp),
                    )
                }
            },
            onClick = counterparty.onCopyClick,
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
        is CounterpartyAvatar.Glyph -> Box(
            modifier = avatarModifier
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.opaque.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = avatar.icon.asImageVector(),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        // The raw modifier, like the contact branch below: AccountIcon sizes itself from ContactDefault (40dp), the
        // same circle the hand-rolled avatars here are drawn at.
        is CounterpartyAvatar.Account -> AccountIcon(
            name = avatar.name,
            icon = avatar.icon,
            size = AccountIconSize.ContactDefault,
            modifier = modifier,
        )
        is CounterpartyAvatar.Wallet -> TangemDeviceIcon(
            state = avatar.deviceIconUM,
            modifier = avatarModifier,
        )
        is CounterpartyAvatar.PaymentAccount -> PaymentAccountIcon(
            size = AccountIconSize.Default,
            modifier = avatarModifier,
        )
        // The raw modifier, not the pre-sized avatarModifier: AccountIcon sizes itself from ContactDefault (40dp),
        // and an outer size() would pin that instead of following the design-system size.
        is CounterpartyAvatar.Contact -> AccountIcon(
            name = stringReference(avatar.name),
            icon = avatar.icon,
            size = AccountIconSize.ContactDefault,
            modifier = modifier,
        )
    }
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
                    title = CounterpartyTitle.Address("33Bd321fS2p2oMzEcHKNK1cEZga21412B"),
                    avatar = CounterpartyAvatar.Address(rawAddress = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359"),
                    onCopyClick = {},
                ),
            )
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = CounterpartyTitle.Name(stringReference("Danil Kolbasenko")),
                    avatar = CounterpartyAvatar.Account(
                        name = stringReference("Danil Kolbasenko"),
                        icon = AccountIconUM.CryptoPortfolio(value = Icon.Letter, color = IconColor.CandyGrapeFizz),
                    ),
                    onCopyClick = {},
                ),
            )
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = CounterpartyTitle.Name(stringReference("Tangem wallet")),
                    avatar = CounterpartyAvatar.Wallet(
                        deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                    ),
                    onCopyClick = {},
                ),
            )
            TxHistoryDetailsCounterpartyRow(
                counterparty = CounterpartyUM(
                    label = stringReference("Recipient"),
                    title = CounterpartyTitle.Name(stringReference("Binance")),
                    avatar = CounterpartyAvatar.Contact(
                        name = "Binance",
                        icon = AccountIconUM.CryptoPortfolio(
                            value = CryptoPortfolioIcon.Icon.Letter,
                            color = CryptoPortfolioIcon.Color.Pattypan,
                        ),
                    ),
                    onCopyClick = {},
                ),
            )
        }
    }
}

// endregion