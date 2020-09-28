package com.tangem.tap.features.wallet.ui

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.card_balance.*
import kotlinx.android.synthetic.main.layout_balance.*
import kotlinx.android.synthetic.main.layout_balance_error.*
import kotlinx.android.synthetic.main.layout_token.view.*

enum class BalanceStatus {
    VerifiedOnline,
    TransactionInProgress,
    Unreachable,
    Loading,
    NoAccount,
    EmptyCard,
    UnknownBlockchain
}

data class BalanceWidgetData(
        val status: BalanceStatus? = null,
        val currency: String? = null,
        val currencySymbol: String? = null,
        val amount: String? = null,
        val fiatAmount: String? = null,
        val token: TokenData? = null,
        val amountToCreateAccount: String? = null,
        val errorMessage: String? = null
)

data class TokenData(
        val amount: String,
        val tokenSymbol: String,
        val fiatAmount: String? = null
)


class BalanceWidget(
        val fragment: Fragment,
        val data: BalanceWidgetData,
) {

    fun setup() {

        when (data.status) {
            BalanceStatus.Loading -> {
                fragment.l_balance.show()
                fragment.l_balance_error.hide()
                fragment.tv_fiat_amount.hide()
                fragment.l_token.hide()

                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = ""

                showStatus(R.id.tv_status_loading)
            }
            BalanceStatus.VerifiedOnline, BalanceStatus.TransactionInProgress -> {
                fragment.l_balance.show()
                fragment.l_balance_error.hide()
                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = data.amount
                fragment.tv_fiat_amount.show()
                fragment.tv_fiat_amount.text = data.fiatAmount
                val statusView = if (data.status == BalanceStatus.VerifiedOnline) {
                    R.id.tv_status_verified
                } else {
                    fragment.tv_status_error.text =
                            fragment.getText(R.string.wallet_transaction_in_progress)
                    R.id.group_error
                }
                showStatus(statusView)
                fragment.tv_status_error_message.hide()

                if (data.token != null) {
                    fragment.l_token.show()
                    fragment.l_token.tv_token_symbol.text = data.token.tokenSymbol
                    fragment.l_token.tv_token_amount.text = data.token.amount
                    fragment.l_token.tv_token_fiat_amount.text = data.token.fiatAmount
                } else {
                    fragment.l_token.hide()
                }
            }
            BalanceStatus.Unreachable -> {
                fragment.l_balance.show()
                fragment.l_balance_error.hide()
                fragment.l_token.hide()
                fragment.tv_fiat_amount.hide()

                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = ""
                fragment.tv_status_error_message.text = data.errorMessage
                fragment.tv_status_error.text =
                        fragment.getString(R.string.wallet_blockchain_is_unreachable)

                showStatus(R.id.group_error)
                fragment.tv_status_error_message.show(!data.errorMessage.isNullOrBlank())
            }
            BalanceStatus.EmptyCard -> {
                fragment.l_balance.hide()
                fragment.l_balance_error.show()
                fragment.tv_error_title.text = fragment.getText(R.string.wallet_empty_card)
                fragment.tv_error_descriptions.text = fragment.getText(R.string.wallet_empty_card_description)
            }
            BalanceStatus.NoAccount -> {
                fragment.l_balance.hide()
                fragment.l_balance_error.show()
                fragment.tv_error_title.text = fragment.getText(R.string.wallet_no_account)
                fragment.tv_error_descriptions.text =
                        fragment.getString(
                                R.string.wallet_no_account_description,
                                data.amountToCreateAccount, data.currencySymbol
                        )
            }
            BalanceStatus.UnknownBlockchain -> {
                fragment.l_balance.hide()
                fragment.l_balance_error.show()
                fragment.tv_error_title.text = fragment.getText(R.string.wallet_unknown_blockchain_title)
                fragment.tv_error_descriptions.text =
                        fragment.getString(R.string.wallet_unknown_blockchain)
            }
        }
    }

    private fun showStatus(@IdRes viewRes: Int) {
        fragment.group_error.show(viewRes == R.id.group_error)
        fragment.tv_status_loading.show(viewRes == R.id.tv_status_loading)
        fragment.tv_status_verified.show(viewRes == R.id.tv_status_verified)
    }
}