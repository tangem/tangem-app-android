package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest._arch.widget.abstraction.getResNameId

/**
[REDACTED_AUTHOR]
 */
class BlockHeadWidget(parent: ViewGroup, data: TextItem) : DescriptionWidget<String>(parent, data) {
    override fun getLayoutId(): Int = R.layout.w_personalize_item_text

    private val tvName = view.findViewById<TextView>(R.id.tv_name)

    init {
        tvName?.setText(getResNameId())
    }
}