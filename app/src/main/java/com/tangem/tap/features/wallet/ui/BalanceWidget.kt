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

enum class PayIdState {
    Loading,
    NotCreated,
    Loaded,
    Error
}

enum class BalanceStatus {
    VerifiedOnline,
    Unreachable,
    Loading,
    NoAccount,
    EmptyCard
}

data class BalanceWidgetData(
        val status: BalanceStatus? = null,
        val currency: String? = null,
        val currencySymbol: String? = null,
        val amount: String? = null,
        val fiatAmount: String? = null,
        val token: TokenData? = null,
        val amountToCreateAccount: Int? = null,
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
                fragment.tv_currency.text = data.currency
                fragment.tv_currency_symbol.text = "-"
                fragment.tv_amount.text = ""
                fragment.tv_fiat_amount.hide()
                showStatus(R.id.tv_status_loading)
                fragment.l_token.hide()
            }
            BalanceStatus.VerifiedOnline -> {
                fragment.l_balance.show()
                fragment.l_balance_error.hide()
                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = data.amount
                fragment.tv_currency_symbol.text = data.currencySymbol
                fragment.tv_fiat_amount.show()
                fragment.tv_fiat_amount.text = data.fiatAmount
                showStatus(R.id.tv_status_verified)

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
                showStatus(R.id.tv_status_error)
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
                                data.amountToCreateAccount?.toString(), data.currencySymbol
                        )
            }
        }
    }

    private fun showStatus(@IdRes viewRes: Int) {
        fragment.tv_status_error.show(viewRes == R.id.tv_status_error)
        fragment.tv_status_loading.show(viewRes == R.id.tv_status_loading)
        fragment.tv_status_verified.show(viewRes == R.id.tv_status_verified)
    }
}


enum class BalanceErrorWidgetState {
    NoAccount,
    NoWallet,
    BlockchainError
}

class BalanceErrorWidget(

)