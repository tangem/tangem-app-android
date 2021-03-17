package com.tangem.tap.features.wallet.ui.adapters

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_warning.view.*

class WarningMessagesAdapter : ListAdapter<WarningMessage, WarningMessageVH>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        val layout = inflater.inflate(R.layout.layout_warning, parent, false)
        return WarningMessageVH(layout)
    }

    override fun onBindViewHolder(holder: WarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WarningMessage>() {
        override fun areContentsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem

        override fun areItemsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem
    }
}

class WarningMessageVH(val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupOkButton(warning)
    }

    private fun setText(warning: WarningMessage) {
        fun getString(resId: Int?, default: String) = if (resId == null) default else view.getString(resId)

        view.tv_title.text = getString(warning.titleResId, warning.title)
        view.tv_message.text = getString(warning.messageResId, warning.message)
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        view.card_view.setCardBackgroundColor(view.context.resources.getColor(color))
    }

    private fun setupOkButton(warning: WarningMessage) {
        view.btn_got_it.show(warning.type == WarningMessage.Type.Temporary)
        view.btn_got_it.setOnClickListener {
            store.dispatch(GlobalAction.HideWarningMessage(warning))
        }
    }
}

class SpacesItemDecoration(private val spacePx: Int) : ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = spacePx
        outRect.right = spacePx

        outRect.top = spacePx / 2
        outRect.top = spacePx / 2
    }
}