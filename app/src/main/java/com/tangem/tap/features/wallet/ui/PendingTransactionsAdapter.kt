package com.tangem.tap.features.wallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.features.wallet.models.PendingTransaction
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.item_pending_transaction.view.*

class PendingTransactionsAdapter
    : ListAdapter<PendingTransaction, PendingTransactionsAdapter.TransactionsViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionsViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pending_transaction, parent, false)
        return TransactionsViewHolder(layout)
    }

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<PendingTransaction>() {
        override fun areContentsTheSame(
                oldItem: PendingTransaction, newItem: PendingTransaction
        ) = oldItem == newItem

        override fun areItemsTheSame(
                oldItem: PendingTransaction, newItem: PendingTransaction
        ) = oldItem == newItem
    }


    class TransactionsViewHolder(val view: View) :
            RecyclerView.ViewHolder(view) {

        fun bind(transaction: PendingTransaction) {
            val transactionDescriptionRes = when (transaction.type) {
                PendingTransactionType.Incoming -> R.string.wallet_pending_transaction_incoming
                PendingTransactionType.Outcoming -> R.string.wallet_pending_transaction_outcoming
            }
            val transactionAddressRes = when (transaction.type) {
                PendingTransactionType.Incoming -> R.string.wallet_pending_transaction_incoming_address
                PendingTransactionType.Outcoming -> R.string.wallet_pending_transaction_outcoming_address
            }
            val image = when (transaction.type) {
                PendingTransactionType.Incoming -> R.drawable.ic_arrow_left
                PendingTransactionType.Outcoming -> R.drawable.ic_arrow_right_20
            }
            view.tv_pending_transaction.text = view.context.getString(transactionDescriptionRes)

            transaction.amount?.let { view.tv_pending_transaction_amount.text = "$it " }
            view.tv_pending_transaction_currency.text = "${transaction.currency}"

            if (transaction.address != null) {
                view.tv_pending_transaction_address.text =
                        view.context.getString(transactionAddressRes, transaction.address)
            }

            view.iv_pending_transaction.setImageDrawable(view.context.getDrawableCompat(image))
        }
    }
}