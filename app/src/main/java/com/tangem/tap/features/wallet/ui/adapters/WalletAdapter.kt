package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getIconRes
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.item_currency_wallet.view.*
import java.math.BigDecimal

class WalletAdapter
    : ListAdapter<WalletData, WalletAdapter.WalletsViewHolder>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return currentList[position].currencyData.currencySymbol?.hashCode()?.toLong() ?: 0
    }

    fun submitList(list: List<WalletData>, primaryBlockchain: Blockchain?, primaryToken: Token? = null) {
        val listModified = list.toMutableList()
        val primaryBlockchainWallet = when (
            val index = listModified.indexOfFirst { it.blockchain == primaryBlockchain }
        ) {
            -1 -> null
            else -> listModified.removeAt(index)
        }


        val primaryTokenWallet = if (primaryToken == null) null else when (
            val index = listModified.indexOfFirst { it.token == primaryToken }
        ) {
            -1 -> null
            else -> listModified.removeAt(index)
        }

        if (list.all { it.currencyData.fiatAmount == null }) {
            val sortedList = listOfNotNull(primaryBlockchainWallet, primaryTokenWallet) + listModified
            super.submitList(sortedList)
            return
        }

        val sorted = listModified.sortedWith(
                compareByDescending<WalletData> { it.currencyData.fiatAmountRaw ?: BigDecimal.ZERO }
                        .thenBy { it.currencyData.currencySymbol }
        )
        val sortedList = listOfNotNull(primaryBlockchainWallet, primaryTokenWallet) + sorted
        super.submitList(sortedList)
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
            view.tv_amount_fiat.text = wallet.currencyData.fiatAmount
            view.tv_exchange_rate.text = wallet.fiatRateString
            view.card_wallet.setOnClickListener {
                store.dispatch(WalletAction.MultiWallet.SelectWallet(wallet))
            }
            val blockchain = wallet.currencyData.currencySymbol?.let { Blockchain.fromCurrency(it) }
            if (blockchain != null && blockchain != Blockchain.Unknown) {
                view.tv_token_letter.text = null
                view.iv_currency.colorFilter = null
                view.iv_currency.setImageResource(blockchain.getIconRes())
            } else {
                view.tv_token_letter.text = wallet.currencyData.currencySymbol?.take(1)
                wallet.token?.getColor()?.let { view.iv_currency.setColorFilter(it) }
                view.iv_currency.setImageResource(R.drawable.shape_circle)
            }
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