package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.ui.BalanceStatus
import com.tangem.tap.features.wallet.ui.images.load
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemCurrencyWalletBinding

class WalletAdapter
    : ListAdapter<WalletData, WalletAdapter.WalletsViewHolder>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return currentList[position].currencyData.currencySymbol?.hashCode()?.toLong() ?: 0
    }

    fun submitList(
        list: List<WalletData>,
        primaryBlockchain: Blockchain?,
        primaryToken: Token? = null
    ) {
        // We used this method to sort the list of currencies. Sorting is disabled for now.
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val layout = ItemCurrencyWalletBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
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

    class WalletsViewHolder(val binding: ItemCurrencyWalletBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(wallet: WalletData) = with(binding) {
            val status = wallet.currencyData.status
            // Skip changes when on refreshing status
            if (status == BalanceStatus.Refreshing) return@with

            val statusMessage = when (status) {
                BalanceStatus.TransactionInProgress -> {
                    root.getString(R.string.wallet_balance_tx_in_progress)
                }
                BalanceStatus.Unreachable -> {
                    root.getString(R.string.wallet_balance_blockchain_unreachable)
                }
                BalanceStatus.NoAccount,
                BalanceStatus.VerifiedOnline,
                BalanceStatus.SameCurrencyTransactionInProgress,
                BalanceStatus.EmptyCard,
                BalanceStatus.UnknownBlockchain,
                BalanceStatus.Loading,
                BalanceStatus.Refreshing,
                null,
                -> null
            }

            if (status == null || status == BalanceStatus.Loading) {
                lContent.root.hide()
                lShimmer.root.veil()
            } else {
                lShimmer.root.unVeil()
                lContent.root.show()
            }

            ivCurrency.load(
                currency = wallet.currency,
                derivationStyle = store.state.globalState
                    .scanResponse
                    ?.card
                    ?.derivationStyle,
            )

            lContent.tvCurrency.text = wallet.currencyData.currency
            lContent.tvAmountFiat.text = wallet.currencyData.fiatAmountFormatted
            lContent.tvAmount.text = wallet.currencyData.amountFormatted

            lContent.tvStatus.isVisible = statusMessage != null
            lContent.tvStatus.text = statusMessage

            lContent.tvExchangeRate.isVisible = statusMessage == null
            lContent.tvExchangeRate.text = wallet.fiatRateString
                ?: root.getString(id = R.string.token_item_no_rate)

            if (wallet.walletAddresses != null) {
                cardWallet.setOnClickListener {
                    store.dispatch(WalletAction.MultiWallet.SelectWallet(wallet))
                }
            } else {
                cardWallet.setOnClickListener(null)
            }
        }
    }
}
