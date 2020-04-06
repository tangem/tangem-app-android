package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.impl.item

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.abstraction.getResNameId
import ru.dev.gbixahue.eu4d.lib.android._android.views.moveCursorToEnd
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class EditTextWidget(parent: ViewGroup, data: EditTextItem) : DescriptionWidget<String>(parent, data) {
    override fun getLayoutId(): Int = R.layout.w_personalize_item_edit_text

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    private val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            dataItem.viewModel.updateDataByView(stringOf(s))
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    init {
        tilItem.hint = tilItem.context.getString(getResNameId())
        etItem.setText(dataItem.viewModel.data)
        etItem.addTextChangedListener(watcher)
        dataItem.viewModel.onDataUpdated = { silentUpdate(it) }
    }

    private fun silentUpdate(value: String?) {
        etItem.removeTextChangedListener(watcher)
        etItem.setText(value)
        etItem.moveCursorToEnd()
        etItem.addTextChangedListener(watcher)
    }
}