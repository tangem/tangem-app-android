package com.tangem.tap.features.wallet.ui.wallet.saltPay.rv

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.setDrawable
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.tap.features.wallet.ui.wallet.saltPay.HistoryTransactionData
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemSaltPayTxHistoryBinding

/**
[REDACTED_AUTHOR]
 */
internal class TransactionHistoryAdapter : ListAdapter<HistoryTransactionData, TransactionItemVH>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return if (currentList.isEmpty()) 0
        else currentList[position].transactionData.hash.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionItemVH {
        val inflater = LayoutInflater.from(parent.context)
        val layout = ItemSaltPayTxHistoryBinding.inflate(inflater, parent, false)
        return TransactionItemVH(layout)
    }

    override fun onBindViewHolder(holder: TransactionItemVH, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<HistoryTransactionData>() {
        override fun areContentsTheSame(
            oldItem: HistoryTransactionData,
            newItem: HistoryTransactionData,
        ) = oldItem == newItem

        override fun areItemsTheSame(
            oldItem: HistoryTransactionData,
            newItem: HistoryTransactionData,
        ) = oldItem == newItem
    }
}

internal class TransactionItemVH(
    private val binding: ItemSaltPayTxHistoryBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(data: HistoryTransactionData) = with(binding) {
        setupImage(data)
        setupTitle(data)
        setupSubtitle(data)
        setupBalance(data)
    }

    private fun setupImage(data: HistoryTransactionData) = with(binding) {
        val drawable = when (data.getTransactionType()) {
            PendingTransactionType.Incoming -> R.drawable.ic_tx_incoming
            PendingTransactionType.Outgoing -> R.drawable.ic_tx_outgoing
            PendingTransactionType.Unknown -> null
        }
        drawable?.let { imvTx.setDrawable(it) }

        if (data.isInProgress()) {
            imvTx.setColorFilter(imvTx.getColor(R.color.icon_attention), PorterDuff.Mode.MULTIPLY)
        } else {
            imvTx.clearColorFilter()
        }
    }

    private fun setupTitle(data: HistoryTransactionData) = with(binding) {
        val hash = data.transactionData.hash ?: return@with

        val formattedHash = "${hash.substring(0..5)}...${hash.substring(hash.length - 4)}"
        tvTxHash.text = formattedHash
    }

    private fun setupSubtitle(data: HistoryTransactionData) = with(binding) {
        val inProgress = data.isInProgress()
        tvTxStatus.show(inProgress)
        tvTxTime.show(!inProgress)

        val time = data.transactionData.date?.timeInMillis.toString() ?: "none"
        tvTxTime.text = time
    }

    private fun setupBalance(data: HistoryTransactionData) = with(binding) {
        val sign = when (data.getTransactionType()) {
            PendingTransactionType.Incoming -> "+"
            PendingTransactionType.Outgoing -> "-"
            PendingTransactionType.Unknown -> ""
        }
        val amount = data.transactionData.amount
        tvTxAmountSign.text = sign
        tvTxAmountValue.text = amount.value?.toFormattedCurrencyString(8, amount.currencySymbol)
    }
}