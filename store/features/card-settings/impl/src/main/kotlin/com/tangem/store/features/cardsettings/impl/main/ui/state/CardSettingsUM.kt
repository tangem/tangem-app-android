package com.tangem.store.features.cardsettings.impl.main.ui.state

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.store.features.cardsettings.impl.common.utils.toTitleRes
import com.tangem.store.features.cardsettings.impl.domain.SecurityOption
import kotlinx.collections.immutable.ImmutableList

internal data class CardSettingsUM(
    val cardDetails: ImmutableList<CardInfoUM>? = null,
    val onScanCardClick: () -> Unit,
    val onElementClick: (CardInfoUM) -> Unit,
    val onBackClick: () -> Unit,
)

internal sealed class CardInfoUM(
    val titleRes: TextReference,
    val subtitle: TextReference,
    val isClickable: Boolean = false,
) {
    class CardId(subtitle: String) : CardInfoUM(
        titleRes = TextReference.Res(R.string.details_row_title_cid),
        subtitle = TextReference.Str(subtitle),
    )

    class Issuer(subtitle: String) : CardInfoUM(
        titleRes = TextReference.Res(R.string.details_row_title_issuer),
        subtitle = TextReference.Str(subtitle),
    )

    class SignedHashes(hashes: String) : CardInfoUM(
        titleRes = TextReference.Res(R.string.details_row_title_signed_hashes),
        subtitle = TextReference.Res(R.string.details_row_subtitle_signed_hashes_format, wrappedList(hashes)),
    )

    class SecurityMode(securityOption: SecurityOption, clickable: Boolean) : CardInfoUM(
        titleRes = TextReference.Res(R.string.card_settings_security_mode),
        subtitle = TextReference.Res(securityOption.toTitleRes()),
        isClickable = clickable,
    )

    data object ChangeAccessCode : CardInfoUM(
        titleRes = TextReference.Res(R.string.card_settings_change_access_code),
        subtitle = TextReference.Res(R.string.card_settings_change_access_code_footer),
        isClickable = true,
    )

    class AccessCodeRecovery(isEnabled: Boolean) : CardInfoUM(
        titleRes = TextReference.Res(R.string.card_settings_access_code_recovery_title),
        subtitle = if (isEnabled) {
            TextReference.Res(R.string.common_enabled)
        } else {
            TextReference.Res(R.string.common_disabled)
        },
        isClickable = true,
    )

    class ResetToFactorySettings(description: TextReference) : CardInfoUM(
        titleRes = TextReference.Res(R.string.card_settings_reset_card_to_factory),
        subtitle = description,
        isClickable = true,
    )
}