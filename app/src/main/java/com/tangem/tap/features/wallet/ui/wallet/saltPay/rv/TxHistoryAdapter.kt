package com.tangem.tap.features.wallet.ui.wallet.saltPay.rv

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.setDrawable
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.models.PendingTransactionType
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemSaltPayTxHistoryBinding
import com.tangem.wallet.databinding.ItemSaltPayTxHistoryDateBinding

/**
[REDACTED_AUTHOR]
 */
internal class TxHistoryAdapter : ListAdapter<HistoryItemData, BaseTxHistoryVH>(DiffUtilCallback) {

    override fun getItemId(position: Int): Long {
        return if (currentList.isEmpty()) 0L
        else currentList[position].itemId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseTxHistoryVH {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> {
                val layout = ItemSaltPayTxHistoryDateBinding.inflate(inflater, parent, false)
                DateItemVH(layout)
            }
            1 -> {
                val layout = ItemSaltPayTxHistoryBinding.inflate(inflater, parent, false)
                TransactionItemVH(layout)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    override fun getItemViewType(position: Int): Int = currentList[position].viewType

    override fun onBindViewHolder(holder: BaseTxHistoryVH, position: Int) {
        holder.bind(currentList[position])
    }

    private object DiffUtilCallback : DiffUtil.ItemCallback<HistoryItemData>() {
        override fun areContentsTheSame(oldItem: HistoryItemData, newItem: HistoryItemData) = oldItem == newItem
        override fun areItemsTheSame(oldItem: HistoryItemData, newItem: HistoryItemData) = oldItem == newItem
    }
}

internal abstract class BaseTxHistoryVH(layout: View) : RecyclerView.ViewHolder(layout) {
    abstract fun bind(data: HistoryItemData)
}

private class DateItemVH(
    private val binding: ItemSaltPayTxHistoryDateBinding,
) : BaseTxHistoryVH(binding.root) {

    override fun bind(data: HistoryItemData) = with(binding) {
        val vhData = (data as? HistoryItemData.Date)?.date ?: return

        tvTitleDate.text = vhData
    }
}

private class TransactionItemVH(
    private val binding: ItemSaltPayTxHistoryBinding,
) : BaseTxHistoryVH(binding.root) {

    override fun bind(data: HistoryItemData) {
        val vhData = (data as? HistoryItemData.TransactionData)?.data ?: return
        setupImage(vhData)
        setupTitle(vhData)
        setupSubtitle(vhData)
        setupBalance(vhData)
    }

    private fun setupImage(data: HistoryTransactionData) = with(binding) {
        val drawable = when (data.transactionType) {
            PendingTransactionType.Incoming -> R.drawable.ic_tx_incoming
            PendingTransactionType.Outgoing -> R.drawable.ic_tx_outgoing
            PendingTransactionType.Unknown -> null
        }
        drawable?.let { imvTx.setDrawable(it) }

        if (data.isInProgress) {
            imvTx.setColorFilter(imvTx.getColor(R.color.icon_attention), PorterDuff.Mode.SRC_ATOP)
        } else {
            imvTx.clearColorFilter()
        }
    }

    private fun setupTitle(data: HistoryTransactionData) = with(binding) {
        tvTxHash.text = data.hash
    }

    private fun setupSubtitle(data: HistoryTransactionData) = with(binding) {
        tvTxStatus.show(data.isInProgress)
        tvTxTime.show(!data.isInProgress)
        tvTxTime.text = data.time
    }

    private fun setupBalance(data: HistoryTransactionData) = with(binding) {
        tvTxAmountSign.text = data.txSign
        tvTxAmountValue.text = data.amountValue
    }
}