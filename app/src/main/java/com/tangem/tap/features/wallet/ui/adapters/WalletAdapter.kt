package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.tap.common.analytics.events.Portfolio
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.ui.images.load
import com.tangem.tap.features.wallet.ui.utils.getFormattedCryptoAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatAmount
import com.tangem.tap.features.wallet.ui.utils.getFormattedFiatRate
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemCurrencyWalletBinding

class WalletAdapter : ListAdapter<WalletDataModel, WalletAdapter.WalletsViewHolder>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return currentList[position].currency.currencySymbol.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletsViewHolder {
        val layout = ItemCurrencyWalletBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return WalletsViewHolder(layout)
    }

    override fun onBindViewHolder(holder: WalletsViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WalletDataModel>() {
        override fun areContentsTheSame(oldItem: WalletDataModel, newItem: WalletDataModel) = oldItem == newItem

        override fun areItemsTheSame(oldItem: WalletDataModel, newItem: WalletDataModel) = oldItem == newItem
    }

    class WalletsViewHolder(val binding: ItemCurrencyWalletBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(wallet: WalletDataModel) = with(binding) {
            val status = wallet.status
            val fiatCurrency = store.state.globalState.appCurrency

            val statusMessage = when (status) {
                is WalletDataModel.TransactionInProgress -> {
                    root.getString(R.string.wallet_balance_tx_in_progress)
                }
                is WalletDataModel.Unreachable -> {
                    root.getString(R.string.wallet_balance_blockchain_unreachable)
                }
                is WalletDataModel.MissedDerivation -> {
                    root.getString(R.string.wallet_balance_missing_derivation)
                }
                else -> null
            }

            if (status is WalletDataModel.Loading) {
                lContent.root.hide()
                lShimmer.root.veil()
            } else {
                lShimmer.root.unVeil()
                lContent.root.show()
            }

            ivCurrency.load(
                currency = wallet.currency,
                derivationStyle = store.state.globalState.scanResponse
                    ?.derivationStyleProvider?.getDerivationStyle(),
            )

            lContent.tvCurrency.text = wallet.currency.currencyName
            lContent.tvAmountFiat.text = wallet.getFormattedFiatAmount(fiatCurrency)
            lContent.tvAmount.text = wallet.getFormattedCryptoAmount()

            lContent.tvStatus.isVisible = statusMessage != null
            lContent.tvStatus.text = statusMessage

            lContent.tvExchangeRate.isVisible = statusMessage == null
            lContent.tvExchangeRate.text = wallet.getFormattedFiatRate(
                fiatCurrency = fiatCurrency,
                noRateValue = root.getString(id = R.string.token_item_no_rate),
            )

            if (wallet.walletAddresses != null) {
                cardWallet.setOnClickListener {
                    Analytics.send(Portfolio.TokenTapped())
                    store.dispatch(WalletAction.MultiWallet.SelectWallet(wallet.currency))
                }
            } else {
                cardWallet.setOnClickListener(null)
            }
        }
    }
}
