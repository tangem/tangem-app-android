package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.blockchain.common.Amount
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.DialogWalletSendBinding
import com.tangem.wallet.databinding.ItemWalletAmountToSendBinding

class AmountToSendBottomSheetDialog(
    context: Context,
    private val dialog: WalletDialog.SelectAmountToSendDialog,
) : BottomSheetDialog(context) {

    var binding: DialogWalletSendBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogWalletSendBinding.inflate(LayoutInflater.from(context))
        setContentView(binding!!.root)
    }

    override fun show() {
        super.show()

        setOnDismissListener {
            binding = null
            store.dispatch(WalletAction.DialogAction.Hide)
        }

        binding!!.rvAmountsToSend.layoutManager = LinearLayoutManager(context)
        val dividerItemDecoration = DividerItemDecoration(
            ContextThemeWrapper(binding!!.root.context, R.style.AppTheme),
            DividerItemDecoration.VERTICAL
        )
        binding!!.rvAmountsToSend.addItemDecoration(dividerItemDecoration)

        val viewAdapter = ChooseAmountAdapter()
        binding!!.rvAmountsToSend.adapter = viewAdapter

        viewAdapter.submitList(dialog.amounts)
    }
}

class ChooseAmountAdapter : ListAdapter<Amount, ChooseAmountAdapter.AmountViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmountViewHolder {
        val binding = ItemWalletAmountToSendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AmountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmountViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<Amount>() {
        override fun areContentsTheSame(
            oldItem: Amount,
            newItem: Amount
        ) = oldItem.currencySymbol == newItem.currencySymbol

        override fun areItemsTheSame(
            oldItem: Amount,
            newItem: Amount
        ) = oldItem == newItem
    }

    class AmountViewHolder(val binding: ItemWalletAmountToSendBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(amount: Amount) = with(binding) {
            tvCurrencySymbol.text = amount.currencySymbol
            tvAmount.text = amount.value?.toFormattedString(amount.decimals)
            root.setOnClickListener {
                store.dispatch(WalletAction.DialogAction.Hide)
                store.dispatch(WalletAction.Send(amount))
            }
        }
    }
}
