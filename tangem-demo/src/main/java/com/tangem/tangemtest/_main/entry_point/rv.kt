package com.tangem.tangemtest._main.entry_point

import android.view.View
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest.commons.NavigateOptions
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvAdapter
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvCallback
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvVH

/**
[REDACTED_AUTHOR]
 */
class RvActionsAdapter(callback: RvCallback<NavigateOptions>) : RvAdapter<RvActionsVH, NavigateOptions>(callback) {

    override fun createViewHolder(view: View, listener: RvCallback<NavigateOptions>?): RvActionsVH {
        return RvActionsVH(view, listener)
    }

    override fun getLayoutId(): Int = R.layout.vh_actions
}

class RvActionsVH(itemView: View, callback: RvCallback<NavigateOptions>?) : RvVH<NavigateOptions>(itemView, callback) {
    private val tvAction = itemView.findViewById<TextView>(R.id.tv_title)

    override fun onDataBound(data: NavigateOptions) {
        tvAction.text = tvAction.context.getString(data.name.resName)
        itemView.setOnClickListener { invokeCallback() }
    }
}