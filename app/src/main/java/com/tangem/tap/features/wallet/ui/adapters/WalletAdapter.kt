package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.extensions.loadCurrenciesIcon
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.item_currency_wallet.view.*

class WalletAdapter
    : ListAdapter<WalletData, WalletAdapter.WalletsViewHolder>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return currentList[position].currencyData.currencySymbol?.hashCode()?.toLong() ?: 0
    }

    fun submitList(list: List<WalletData>, primaryBlockchain: Blockchain?, primaryToken: Token? = null) {
        // We used this method to sort the list of currencies. Sorting is disabled for now.
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_currency_wallet, parent, false)
        return WalletsViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WalletsViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WalletData>() {
        override fun areContentsTheSame(
                oldItem: WalletData, newItem: WalletData
        ) = oldItem == newItem

        override fun areItemsTheSame(
                oldItem: WalletData, newItem: WalletData
        ) = oldItem == newItem
    }

    class WalletsViewHolder(val view: View) :
            RecyclerView.ViewHolder(view) {

        fun bind(wallet: WalletData) {
            view.tv_currency.text = wallet.currencyData.currency
            view.tv_amount.text = wallet.currencyData.amount?.takeWhile { !it.isWhitespace() }
            view.tv_currency_symbol.text = wallet.currencyData.amount?.takeLastWhile { !it.isWhitespace() }
            view.tv_amount_fiat.text = wallet.currencyData.fiatAmountFormatted
            view.tv_exchange_rate.text = wallet.fiatRateString
            view.card_wallet.setOnClickListener {
                store.dispatch(WalletAction.MultiWallet.SelectWallet(wallet))
            }
            val blockchain = wallet.currency.blockchain
            val token = (wallet.currency as? Currency.Token)?.token

            Picasso.get().loadCurrenciesIcon(
                imageView = view.iv_currency,
                textView = view.tv_token_letter,
                token = token, blockchain = blockchain,
            )

            when (wallet.currencyData.status) {
                BalanceStatus.VerifiedOnline, BalanceStatus.SameCurrencyTransactionInProgress -> hideWarning()
                BalanceStatus.Loading -> {
                    hideWarning()
                    if (wallet.currencyData.amount == null) {
                        view.tv_exchange_rate.text = view.context.getText(R.string.wallet_balance_loading)
                    }
                }
                BalanceStatus.TransactionInProgress ->
                    showWarning(view.context.getString(R.string.wallet_balance_tx_in_progress))
                BalanceStatus.Unreachable ->
                    showWarning(view.context.getString(R.string.wallet_balance_blockchain_unreachable))

                BalanceStatus.NoAccount ->
                    showWarning(view.context.getString(R.string.wallet_error_no_account))
                else -> {
                }
            }
        }

        private fun showWarning(message: String) {
            toggleWarning(true)
            view.tv_status_error_message.text = message
        }

        private fun hideWarning() {
            toggleWarning(false)
        }

        private fun toggleWarning(show: Boolean) {
            view.tv_exchange_rate.show(!show)
            view.tv_status_error_message.show(show)
        }
    }
}