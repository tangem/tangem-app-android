package com.tangem.tangemtest.ucase.variants.responses.ui.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.DescriptionWidget

/**
[REDACTED_AUTHOR]
 */
class ResponseTextWidget(
        parent: ViewGroup,
        private val typedItem: TextItem
) : DescriptionWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_response_item

    private val tvName: TextView = view.findViewById(R.id.tv_name)
    private val tvValue: TextView = view.findViewById(R.id.tv_value)

    init {
        initWidgets()
    }

    private fun initWidgets() {
        val data = typedItem.getData() as? String
        if (data == null || data.isEmpty()) {
            view.visibility = View.GONE
            return
        }

        tvName.text = getName()
        tvValue.text = data

        view.setOnClickListener {
            val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return@setOnClickListener

            val clip: ClipData = ClipData.newPlainText("FieldValue", "$data")
            clipboard.setPrimaryClip(clip)
        }
    }
}