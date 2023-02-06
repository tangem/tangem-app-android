package com.tangem.tap.features.details.ui.cardsettings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.tangem.tap.features.details.redux.SecurityOption
import com.tangem.tap.features.details.ui.securitymode.toTitleRes
import com.tangem.tap.features.details.ui.utils.toResetCardDescriptionText
import com.tangem.wallet.R
import com.tangem.tap.features.details.redux.CardInfo as ReduxCardInfo

data class CardSettingsScreenState(
    val cardDetails: List<CardInfo>? = null,
    val onScanCardClick: () -> Unit,
    val onElementClick: (CardInfo) -> Unit,
)

sealed class CardInfo(
    val titleRes: TextReference,
    val subtitle: TextReference,
    val clickable: Boolean = false,
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
        subtitle = TextReference.Res(R.string.details_row_subtitle_signed_hashes_format, hashes),
    )

    class SecurityMode(securityOption: SecurityOption, clickable: Boolean) : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_security_mode),
        subtitle = TextReference.Res(securityOption.toTitleRes()),
        clickable = clickable,
    )

    object ChangeAccessCode : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_change_access_code),
        subtitle = TextReference.Res(R.string.card_settings_change_access_code_footer),
        clickable = true,
    )

    class ResetToFactorySettings(cardInfo: ReduxCardInfo) : CardInfo(
        titleRes = TextReference.Res(R.string.card_settings_reset_card_to_factory),
        subtitle = cardInfo.toResetCardDescriptionText(),
        clickable = true,
    )
}

sealed interface TextReference {
    class Res(@StringRes val id: Int, val formatArgs: List<Any> = emptyList()) : TextReference {
        constructor(@StringRes id: Int, vararg formatArgs: Any) : this(id, formatArgs.toList())
    }

    class Str(val value: String) : TextReference
}

@Composable
@ReadOnlyComposable
fun TextReference.resolveReference(): String {
    return when (this) {
        is TextReference.Res -> stringResource(this.id, *this.formatArgs.toTypedArray())
        is TextReference.Str -> this.value
    }
}
