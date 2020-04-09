package com.tangem.tangemtest.ucase.variants.responses.ui.widget

import android.view.ViewGroup
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.TextItem

/**
[REDACTED_AUTHOR]
 */
class ResponseTextWidget(
        parent: ViewGroup,
        private val typedItem: TextItem
) : ResponseWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_response_item

    private val tvName: TextView = view.findViewById(R.id.tv_name)
    private val tvValue: TextView = view.findViewById(R.id.tv_value)

    init {
        initWidgets()
    }

    private fun initWidgets() {
        val data = typedItem.getData() as? String
        tvName.text = getName()
        tvValue.text = data
    }
}