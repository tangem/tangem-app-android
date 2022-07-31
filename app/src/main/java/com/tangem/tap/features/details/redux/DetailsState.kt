package com.tangem.tap.features.details.redux

import android.net.Uri
import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.Card
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.StateDialog
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.store
import org.rekotlin.StateType
import java.util.*
import kotlin.properties.ReadOnlyProperty

data class DetailsState(
    val scanResponse: ScanResponse? = null,
    val wallets: List<Wallet> = emptyList(),
    val cardSettingsState: CardSettingsState? = null,
    val cardTermsOfUseUrl: Uri? = null,
    val privacyPolicyUrl: String? = null,
    val createBackupAllowed: Boolean = false,
    val appCurrency: FiatCurrency = FiatCurrency.Default,
    val saveCards: Boolean = true,
    val savePasswords: Boolean = true
) : StateType {

    // if you do not delegate - the application crashes on startup,
    // because twinCardsState has not been created yet
    val twinCardsState: TwinCardsState by ReadOnlyProperty<Any, TwinCardsState> { thisRef, property ->
        store.state.twinCardsState
    }

    val isTangemTwins: Boolean
        get() = store.state.globalState.scanResponse?.isTangemTwins() == true
}

data class CardInfo(
    val cardId: String,
    val issuer: String,
    val signedHashes: Int,
)

data class CardSettingsState(
    val cardInfo: CardInfo,
    val card: Card,
    val manageSecurityState: ManageSecurityState?,
    val resetCardAllowed: Boolean,
    val resetConfirmed: Boolean = false
)

data class ManageSecurityState(
    val currentOption: SecurityOption = SecurityOption.LongTap,
    val selectedOption: SecurityOption = currentOption,
    val allowedOptions: EnumSet<SecurityOption> = EnumSet.allOf(SecurityOption::class.java),
    val buttonProceed: Button = Button(true),
)

enum class SecurityOption { LongTap, PassCode, AccessCode }


sealed interface DetailsDialog : StateDialog {
    data class ConfirmDisablingSaving(val setting: PrivacySetting) : DetailsDialog {
        val onOk: () -> Unit =
            { store.dispatch(DetailsAction.AppSettings.ConfirmSwitchingSetting(false, setting)) }
    }
}

enum class PrivacySetting {
    SAVE_CARDS, SAVE_ACCESS_CODE
}
