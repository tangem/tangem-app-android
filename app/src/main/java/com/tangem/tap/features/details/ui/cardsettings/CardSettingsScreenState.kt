package com.tangem.tap.features.details.ui.cardsettings

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.securitymode.toTitleRes
import com.tangem.wallet.R

internal data class CardSettingsScreenState(
    val cardDetails: List<CardInfo>? = null,
    val onScanCardClick: () -> Unit,
    val onElementClick: (CardInfo) -> Unit,
    val onBackClick: () -> Unit,
)

internal sealed class CardInfo(
    val titleRes: TextReference,
    val subtitle: TextReference,
    val isClickable: Boolean = false,
) {
    class CardId(subtitle: String) : CardInfo(
        titleRes = TextReference.Res(R.string.details_row_title_cid),
        subtitle = TextReference.Str(subtitle),
    )

    class Issuer(subtitle: String) : CardInfo(
        titleRes = TextReference.Res(R.string.details_row_title_issuer),
        subtitle = TextReference.Str(subtitle),
    )

    class SignedHashes(hashes: String) : CardInfo(
        titleRes = TextReference.Res(R.string.details_row_title_signed_hashes),
        subtitle = TextReference.Res(R.string.details_row_subtitle_signed_hashes_format, wrappedList(hashes)),
    )

    class SecurityMode(securityOption: SecurityOption, clickable: Boolean) : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_security_mode),
        subtitle = TextReference.Res(securityOption.toTitleRes()),
        isClickable = clickable,
    )

    data object ChangeAccessCode : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_change_access_code),
        subtitle = TextReference.Res(R.string.card_settings_change_access_code_footer),
        isClickable = true,
    )

    class AccessCodeRecovery(isEnabled: Boolean) : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_access_code_recovery_title),
        subtitle = if (isEnabled) {
            TextReference.Res(R.string.common_enabled)
        } else {
            TextReference.Res(R.string.common_disabled)
        },
        isClickable = true,
    )

    class ResetToFactorySettings(description: TextReference) : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_reset_card_to_factory),
        subtitle = description,
        isClickable = true,
    )
}