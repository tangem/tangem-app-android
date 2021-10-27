package com.tangem.tap.features.details.redux

import android.net.Uri
import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.Card
import com.tangem.tap.common.entities.Button
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.features.onboarding.twins.redux.TwinCardsState
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import com.tangem.tap.store
import org.rekotlin.StateType
import java.util.*
import kotlin.properties.ReadOnlyProperty

data class DetailsState(
    val card: Card? = null,
    val wallets: List<Wallet> = emptyList(),
    val cardInfo: CardInfo? = null,
    val appCurrencyState: AppCurrencyState = AppCurrencyState(),
    val eraseWalletState: EraseWalletState? = null,
    val confirmScreenState: ConfirmScreenState? = null,
    val securityScreenState: SecurityScreenState? = null,
    val cardTermsOfUseUrl: Uri? = null,
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

enum class EraseWalletState { Allowed, NotAllowedByCard, NotEmpty }
enum class ConfirmScreenState { EraseWallet, LongTap, AccessCode, PassCode }
data class SecurityScreenState(
    val currentOption: SecurityOption = SecurityOption.LongTap,
    val selectedOption: SecurityOption = currentOption,
    val allowedOptions: EnumSet<SecurityOption> = EnumSet.allOf(SecurityOption::class.java),
    val buttonProceed: Button = Button(true),
)

enum class SecurityOption { LongTap, PassCode, AccessCode }
data class AppCurrencyState(
    val fiatCurrencyName: FiatCurrencyName = DEFAULT_FIAT_CURRENCY,
    val showAppCurrencyDialog: Boolean = false,
    val fiatCurrencies: List<FiatCurrency>? = null,
)