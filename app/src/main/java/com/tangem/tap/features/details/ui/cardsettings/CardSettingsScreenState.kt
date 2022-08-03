package com.tangem.tap.features.details.ui.cardsettings

import com.tangem.wallet.R

data class CardSettingsScreenState(
    val cardDetails: List<CardInfo>? = null,
    val onScanCardClick: () -> Unit,
    val onElementClick: (CardInfo) -> Unit,
)

sealed class CardInfo(
    val titleRes: Int, val subtitle: String, val clickable: Boolean = false,
) {
    class CardId(subtitle: String) : CardInfo(R.string.details_row_title_cid, subtitle)

    class Issuer(subtitle: String) : CardInfo(R.string.details_row_title_issuer, subtitle)

    class SignedHashes(subtitle: String) : CardInfo(R.string.details_row_title_signed_hashes, subtitle)

    class SecurityMode(
        subtitle: String,
        clickable: Boolean,
    ) : CardInfo(R.string.card_settings_security_mode, subtitle, clickable)

    class ChangeAccessCode(subtitle: String) : CardInfo(
        R.string.card_settings_change_access_code,
        subtitle,
        true,
    )

    class ResetToFactorySettings(subtitle: String) : CardInfo(
        R.string.details_row_title_reset_factory_settings,
        subtitle,
        true,
    )
}