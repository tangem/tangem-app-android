package com.tangem.tangemtest._main.entryPoint

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tangemtest.R
import com.tangem.tangemtest.cardUseCase.resources.ActionRes
import com.tangem.tangemtest.cardUseCase.resources.ActionType
import com.tangem.tangemtest.cardUseCase.resources.MainResourceHolder
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvBaseAdapter
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvBaseVH
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvCallback

/**
[REDACTED_AUTHOR]
 */
class RvActionsAdapter(
        private val wrapper: VhExDataWrapper,
        private val callback: RvCallback<Int>
) : RvBaseAdapter<RvActionsVH, ActionType>() {

    override fun onBindViewHolder(holder: RvActionsVH, position: Int) {
        holder.bindData(itemList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvActionsVH {
        val view = parent.inflate<View>(R.layout.vh_actions, false)
        return RvActionsVH(view, wrapper, callback)
    }

}

class VhExDataWrapper(
        val resHolder: MainResourceHolder,
        var descriptionIsVisible: Boolean
)

class RvActionsVH(
        itemView: View,
        private val wrapper: VhExDataWrapper,
        private val callback: RvCallback<Int>?
) : RvBaseVH<ActionType>(itemView) {

    private val tvAction = itemView.findViewById<TextView>(R.id.tv_title)
    private val containerDescription = itemView.findViewById<ViewGroup>(R.id.container_description)
    private val tvDescription = itemView.findViewById<TextView>(R.id.tv_description)

    override fun bindData(data: ActionType) {
        val res: ActionRes = wrapper.resHolder.safeGet(data)
        tvAction.text = getString(res.resName)
        tvDescription.text = getString(res.resDescription)

        itemView.setOnClickListener {
            res.resNavigation?.let { callback?.invoke(itemViewType, adapterPosition, it) }
        }

        containerDescription.visibility = if (wrapper.descriptionIsVisible) View.VISIBLE else View.GONE
    }

    override fun onDataBound(data: ActionType) {}
}

fun RecyclerView.ViewHolder.getString(@StringRes id: Int?, ifNull: String = ""): String {
    val reqId = id ?: return ifNull
    return itemView.context.getString(reqId)
}