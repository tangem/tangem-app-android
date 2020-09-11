package com.tangem.tap.features.wallet.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tangem.blockchain.common.Amount
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.dialog_wallet_send.*
import kotlinx.android.synthetic.main.item_wallet_amount_to_send.view.*


class AmountToSendDialog(context: Context) : BottomSheetDialog(context) {

    init {
        this.setContentView(R.layout.dialog_wallet_send)
    }

    fun show(amounts: List<Amount>?) {
        this.setOnDismissListener {
            store.dispatch(WalletAction.Send.Cancel)
        }

        rv_amounts_to_send.layoutManager = LinearLayoutManager(context)
        val dividerItemDecoration = DividerItemDecoration(
                ContextThemeWrapper(rv_amounts_to_send.context, R.style.AppTheme),
                DividerItemDecoration.VERTICAL
        )
        rv_amounts_to_send.addItemDecoration(dividerItemDecoration)

        val viewAdapter = ChooseAmountAdapter()
        rv_amounts_to_send.adapter = viewAdapter

        viewAdapter.submitList(amounts)

        show()
    }
}


class ChooseAmountAdapter
    : ListAdapter<Amount, ChooseAmountAdapter.AmountViewHolder>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmountViewHolder {
        val layout = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wallet_amount_to_send, parent, false)
        return AmountViewHolder(layout)
    }

    override fun onBindViewHolder(holder: AmountViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<Amount>() {
        override fun areContentsTheSame(
                oldItem: Amount, newItem: Amount
        ) = oldItem.currencySymbol == newItem.currencySymbol

        override fun areItemsTheSame(
                oldItem: Amount, newItem: Amount
        ) = oldItem == newItem
    }

    class AmountViewHolder(val view: View) :
            RecyclerView.ViewHolder(view) {

        fun bind(amount: Amount) {
            view.tv_currency_symbol.text = amount.currencySymbol
            view.tv_amount.text = amount.value?.toFormattedString(amount.decimals)
            view.setOnClickListener {
                store.dispatch(WalletAction.Send.Cancel)
                store.dispatch(WalletAction.Send(amount))
            }
        }
    }
}