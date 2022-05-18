package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getString
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutWarningCardBinding

/**
[REDACTED_AUTHOR]
 */
class WalletDetailWarningMessagesAdapter
    : ListAdapter<WalletDetailsWarning, WalletDetailsWarningMessageVH>(DiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletDetailsWarningMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutWarningCardBinding.inflate(inflater, parent, false)

        return WalletDetailsWarningMessageVH(binding)
    }

    override fun onBindViewHolder(holder: WalletDetailsWarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    private class DiffUtilCallback : DiffUtil.ItemCallback<WalletDetailsWarning>() {
        override fun areContentsTheSame(oldItem: WalletDetailsWarning, newItem: WalletDetailsWarning) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: WalletDetailsWarning, newItem: WalletDetailsWarning) =
            oldItem == newItem
    }
}

class WalletDetailsWarningMessageVH(
    val binding: LayoutWarningCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(warning: WalletDetailsWarning) {
        binding.warningCard.setCardBackgroundColor(binding.root.getColor(R.color.darkGray2))
        setText(warning)
    }

    private fun setText(warning: WalletDetailsWarning) = with(binding.warningContentContainer) {
        fun getString(
            stringResId: Int,
            formatArgs: List<String> = emptyList()
        ) = root.getString(stringResId, *formatArgs.toTypedArray())

        tvTitle.text = getString(warning.title)
        tvMessage.text = getString(warning.message, warning.messageArgs)
    }
}

data class WalletDetailsWarning(
    val title: Int,
    val message: Int,
    val titleArgs: List<String> = emptyList(),
    val messageArgs: List<String> = emptyList(),
)