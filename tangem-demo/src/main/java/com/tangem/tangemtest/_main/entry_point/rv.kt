package com.tangem.tangemtest._main.entry_point

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tangemtest.R
import com.tangem.tangemtest.card_use_cases.CardContext
import com.tangem.tangemtest.card_use_cases.actions.Action
import com.tangem.tangemtest.commons.Bindable
import com.tangem.tangemtest.commons.NavigateAction

/**
[REDACTED_AUTHOR]
 */
typealias RvCallback = (Int, Any?) -> Unit

class RvActionsAdapter(
        private val cardContext: CardContext,
        private val callback: RvCallback
) : RecyclerView.Adapter<RvActionsVH>() {

    private val actionList = mutableListOf<NavigateAction>()

    fun setItems(list: MutableList<NavigateAction>) {
        actionList.clear()
        actionList.addAll(list)
    }

    fun addToItems(list: MutableList<NavigateAction>) {
        actionList.addAll(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvActionsVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.vh_actions, parent, false)
        return RvActionsVH(view, cardContext.isVerified, callback)
    }

    override fun getItemCount(): Int = actionList.size

    override fun onBindViewHolder(holder: RvActionsVH, position: Int) {
        holder.bind(actionList[position])
    }
}

class RvActionsVH(
        view: View,
        private val cardIsVerified: Boolean,
        private val callback: RvCallback
) : RecyclerView.ViewHolder(view), Bindable<NavigateAction> {

    private val tvAction = itemView.findViewById<TextView>(R.id.tv_title)
    private val navigateRight = itemView.findViewById<TextView>(R.id.tv_navigate_right)

    override fun bind(data: NavigateAction) {
        tvAction.text = tvAction.context.getString(data.action.resId)
        itemView.setOnClickListener { callback(adapterPosition, data.destinationId) }
        switchItemBackground(data.action)
    }

    private fun switchItemBackground(action: Action) {
        when (action) {
            is Action.Scan -> {
            }
            else -> itemView.visibility = if (cardIsVerified) View.VISIBLE else View.GONE
        }
    }
}