package com.tangem.tangemtest.ucase.ui.widgets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest._arch.widget.abstraction.getResNameId
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.DescriptionWidget

/**
[REDACTED_AUTHOR]
 */
class ResponseTextWidget(parent: ViewGroup, textItem: TextItem) : DescriptionWidget<String>(parent, textItem) {
    override fun getLayoutId(): Int = R.layout.w_response_item

    private val tvName: TextView by lazy { view.findViewById<TextView>(R.id.tv_name) }
    private val tvValue: TextView by lazy { view.findViewById<TextView>(R.id.tv_value) }

    init {
        tvName.setText(getResNameId())
        tvValue.text = dataItem.getData()

        view.setOnClickListener {
            val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return@setOnClickListener

            val clip: ClipData = ClipData.newPlainText("FieldValue", "${dataItem.getData()}")
            clipboard.setPrimaryClip(clip)
        }
    }
}