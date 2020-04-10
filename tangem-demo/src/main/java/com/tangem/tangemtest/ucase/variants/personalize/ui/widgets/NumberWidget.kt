package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.NumberItem
import com.tangem.tangemtest.ucase.variants.personalize.CardNumber
import ru.dev.gbixahue.eu4d.lib.android._android.views.addInputFilter
import ru.dev.gbixahue.eu4d.lib.android._android.views.moveCursorToEnd
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
* [REDACTED_AUTHOR]
 */
class NumberWidget(
        parent: ViewGroup,
        private val typedItem: NumberItem
) : DescriptionWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_personalize_item_number

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    private val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            typedItem.viewModel.updateDataByView(getValue(stringOf(s)))
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    init {
        tilItem.hint = getName()
// [REDACTED_TODO_COMMENT]
        if (item.id == CardNumber.Number) etItem.addInputFilter(InputFilter.LengthFilter(13))

        etItem.setText(stringOf(typedItem.getTypedData()))
        etItem.addTextChangedListener(watcher)
        item.viewModel.onDataUpdated = { silentUpdate(it as? Number) }
    }

    private fun silentUpdate(value: Number?) {
        etItem.removeTextChangedListener(watcher)
        etItem.setText(stringOf(value))
        etItem.moveCursorToEnd()
        etItem.addTextChangedListener(watcher)
    }

    private fun getValue(value: String): Long {
        return if (value.isEmpty()) 0L else value.toLong()
    }
}