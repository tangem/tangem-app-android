package com.tangem.tap.features.wallet.ui

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_balance.*
import kotlinx.android.synthetic.main.layout_token.view.*

enum class PayIdState {
    Loading,
    NotCreated,
    Loaded,
    Error
}

data class PayIdData(
    val address: String? = null,
    val state: PayIdState? = null
)

enum class BalanceStatus {
    VerifiedOnline,
    Unreachable,
    Loading
}

data class BalanceWidgetData(
        val status: BalanceStatus? = null,
        val currency: String? = null,
        val amount: String? = null,
        val fiatAmount: String? = null,
        val token: TokenData? = null
)

data class TokenData(
        val amount: String,
        val tokenSymbol: String
)


class BalanceWidget(
        val fragment: Fragment,
        val data: BalanceWidgetData,
) {

    fun setup() {

        when (data.status) {
            BalanceStatus.Loading -> {
                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = "-"
                fragment.tv_fiat_amount.hide()
                showStatus(R.id.tv_status_loading)
                fragment.l_token.hide()
            }
            BalanceStatus.VerifiedOnline -> {
                fragment.tv_currency.text = data.currency
                fragment.tv_amount.text = data.amount
                fragment.tv_fiat_amount.text = data.fiatAmount
                showStatus(R.id.tv_status_verified)

                if (data.token != null) {
                    fragment.l_token.show()
                    fragment.l_token.tv_token_symbol.text = data.token.tokenSymbol
                    fragment.l_token.tv_token_amount.text = data.token.amount
                } else {
                    fragment.l_token.hide()

                }

            }
            BalanceStatus.Unreachable -> {
                fragment.l_token.hide()
                showStatus(R.id.tv_status_error)


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