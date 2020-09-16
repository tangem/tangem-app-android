package com.tangem.tap.features.details.redux

import com.tangem.blockchain.common.Wallet
import com.tangem.commands.Card
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.common.redux.global.FiatCurrencyName
import com.tangem.tap.network.coinmarketcap.FiatCurrency
import com.tangem.wallet.R
import org.rekotlin.Action

sealed class DetailsAction : Action {

    data class PrepareScreen(
            val card: Card,
            val wallet: Wallet?,
            val fiatCurrencyName: FiatCurrencyName,
            val fiatCurrencies: List<FiatCurrencyName>? = null,
    ): DetailsAction()


    sealed class EraseWallet : DetailsAction() {
        object Check : EraseWallet()
        object Proceed : EraseWallet() {
            object NotAllowedByCard: EraseWallet(), NotificationAction {
                override val messageResource = R.string.details_notification_erase_wallet_not_allowed
            }
            object NotEmpty: EraseWallet(), NotificationAction {
                override val messageResource = R.string.details_notification_erase_wallet_not_possible
            }
        }
        object Confirm : EraseWallet()
        object Cancel : EraseWallet()
        object Failure : EraseWallet()
        object Success : EraseWallet()
    }

    sealed class AppCurrencyAction : DetailsAction() {
        data class SetCurrencies(val currencies: List<FiatCurrency>) : AppCurrencyAction()
        object ChooseAppCurrency : AppCurrencyAction()
        object Cancel: AppCurrencyAction()
        data class SelectAppCurrency(val fiatCurrencyName: FiatCurrencyName): AppCurrencyAction()
    }

}