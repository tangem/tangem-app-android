package com.tangem.devkit.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.TextView
import com.tangem.devkit.R
import com.tangem.devkit._arch.structure.impl.TextItem

/**
[REDACTED_AUTHOR]
 */
class TextHeaderWidget(parent: ViewGroup, data: TextItem) : DescriptionWidget(parent, data) {
    override fun getLayoutId(): Int = R.layout.w_personalize_item_header

    private val tvName = view.findViewById<TextView>(R.id.tv_name)

    init {
        tvName.text = getName()
    }
}