package com.tangem.tap.features.details.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.commands.Card
import com.tangem.tap.common.entities.TapCurrency.Companion.DEFAULT_FIAT_CURRENCY
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import org.rekotlin.StateType

data class DetailsState(
        val card: Card? = null,
        val wallet: Wallet? = null,
        val cardInfo: CardInfo? = null,
        val appCurrencyState: AppCurrencyState = AppCurrencyState(),
        val eraseWalletState: EraseWalletState? = null,
        val confirmScreenState: ConfirmScreenState? = null,
) : StateType

data class CardInfo(
        val cardId: String,
        val issuer: String,
        val signedHashes: Int
)

enum class EraseWalletState { Allowed, NotAllowedByCard, NotEmpty }
enum class ConfirmScreenState { EraseWallet, LongTap, AccessCode, PassCode }
data class AppCurrencyState(
        val fiatCurrencyName: FiatCurrencyName = DEFAULT_FIAT_CURRENCY,
        val showAppCurrencyDialog: Boolean = false,
        val fiatCurrencies: List<FiatCurrency>? = null,
)

