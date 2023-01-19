package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.features.wallet.models.WalletWarningDescription
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutWarningCardBinding

/**
 * Created by Anton Zhilenkov on 18/05/2022.
 */
class WalletDetailWarningMessagesAdapter :
    ListAdapter<WalletWarningDescription, WalletDetailsWarningMessageVH>(DiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletDetailsWarningMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutWarningCardBinding.inflate(inflater, parent, false)

        return WalletDetailsWarningMessageVH(binding)
    }

    override fun onBindViewHolder(holder: WalletDetailsWarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    private class DiffUtilCallback : DiffUtil.ItemCallback<WalletWarningDescription>() {
        override fun areContentsTheSame(oldItem: WalletWarningDescription, newItem: WalletWarningDescription) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: WalletWarningDescription, newItem: WalletWarningDescription) =
            oldItem == newItem
    }
}

class WalletDetailsWarningMessageVH(
    val binding: LayoutWarningCardBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(warning: WalletWarningDescription) {
        binding.warningCard.setCardBackgroundColor(binding.root.getColor(R.color.darkGray2))
        setText(warning)
    }

    private fun setText(warning: WalletWarningDescription) = with(binding.warningContentContainer) {
        tvTitle.text = warning.title
        tvMessage.text = warning.message
    }
}