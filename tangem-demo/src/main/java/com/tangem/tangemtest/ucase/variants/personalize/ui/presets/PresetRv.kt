package com.tangem.tangemtest.ucase.variants.personalize.ui.presets

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.SafeValueChanged
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvBaseAdapter
import ru.dev.gbixahue.eu4d.lib.android._android.views.recycler_view.RvVH

/**
[REDACTED_AUTHOR]
 */
class RvPresetNamesAdapter(
        private val onItemClicked: SafeValueChanged<String>,
        private val onDeleteClicked: SafeValueChanged<String>
) : RvBaseAdapter<PresetNameVH, String>() {
    override fun onBindViewHolder(holder: PresetNameVH, position: Int) {
        holder.bindData(itemList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetNameVH {
        val view = parent.inflate<View>(R.layout.vh_personalization_preset_name, false)
        return PresetNameVH(view, onItemClicked, { position, value ->
            onDeleteClicked(value)
            itemList.removeAt(position)
            this.notifyItemRemoved(position)
        })
    }
}

class PresetNameVH(
        itemView: View,
        private val onItemClicked: SafeValueChanged<String>,
        private val onDeleteClicked: (Int, String) -> Unit
) : RvVH<String>(itemView) {
    private val tvName = itemView.findViewById<TextView>(R.id.tv_name)
    private val btnDelete = itemView.findViewById<View>(R.id.btn_delete)
    override fun onDataBound(data: String) {
        tvName.text = data
        tvName.isClickable = false
        itemView.setOnClickListener { onItemClicked(data) }
        btnDelete.setOnClickListener { onDeleteClicked(absoluteAdapterPosition, data) }
    }
}