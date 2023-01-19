package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemPendingTransactionBinding

class PendingTransactionsAdapter :
    ListAdapter<PendingTransaction, PendingTransactionsAdapter.TransactionsViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        val binding = ItemPendingTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<PendingTransaction>() {
        override fun areContentsTheSame(
            oldItem: PendingTransaction,
            newItem: PendingTransaction
        ) = oldItem == newItem

        override fun areItemsTheSame(
            oldItem: PendingTransaction,
            newItem: PendingTransaction
        ) = oldItem == newItem
    }

    class TransactionsViewHolder(val binding: ItemPendingTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: PendingTransaction) {
            if (transaction.type == PendingTransactionType.Unknown) {
                binding.root.hide()
            }

            val transactionDescriptionRes = when (transaction.type) {
                PendingTransactionType.Incoming -> R.string.wallet_pending_tx_receiving
                PendingTransactionType.Outgoing -> R.string.wallet_pending_tx_sending
                PendingTransactionType.Unknown -> return
            }
            val transactionAddressRes = when (transaction.type) {
                PendingTransactionType.Incoming -> R.string.wallet_pending_tx_receiving_address_format
                PendingTransactionType.Outgoing -> R.string.wallet_pending_tx_sending_address_format
                PendingTransactionType.Unknown -> return
            }
            val image = when (transaction.type) {
                PendingTransactionType.Incoming -> R.drawable.ic_arrow_left
                PendingTransactionType.Outgoing -> R.drawable.ic_arrow_right_20
                PendingTransactionType.Unknown -> return
            }
            binding.tvPendingTransaction.text =
                binding.root.getString(transactionDescriptionRes).let { "$it " }

            transaction.amountValueUi?.let { binding.tvPendingTransactionAmount.text = "$it " }
            binding.tvPendingTransactionCurrency.text = transaction.currency

            if (transaction.address != null) {
                binding.tvPendingTransactionAddress.text =
                    binding.root.getString(transactionAddressRes, transaction.address)
            }
            binding.ivPendingTransaction.setImageDrawable(binding.root.context.getDrawableCompat(image))
        }
    }
}
