package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets

import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.*
import ru.dev.gbixahue.eu4d.lib.android._android.views.afterTextChanged
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */

class TextWidget(parent: ViewGroup, private val textUnit: TextUnit) : BaseParamWidget<String>(parent, textUnit) {

    private val tvName = view.findViewById<TextView>(R.id.tv_name)

    init {
        bindData()
    }

    private fun bindData() {
        tvName.setText(getResNameId())
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_text
}

class EditTextWidget(parent: ViewGroup, private val editTextUnit: EditTextUnit) : BaseParamWidget<String>(parent, editTextUnit) {

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    init {
        bindData()
    }

    private fun bindData() {
        tilItem.hint = tilItem.context.getString(getResNameId())
        etItem.setText(editTextUnit.viewModel?.data)
        etItem.afterTextChanged { editTextUnit.viewModel?.updateData(it) }
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_edit_text
}

class NumberWidget(parent: ViewGroup, private val numberUnit: NumberUnit) : BaseParamWidget<Number>(parent, numberUnit) {

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    init {
        bindData()
    }

    private fun bindData() {
        tilItem.hint = tilItem.context.getString(getResNameId())
        etItem.setText(stringOf(numberUnit.viewModel?.data))
        etItem.afterTextChanged { numberUnit.viewModel?.updateData(getIntValue(it)) }
    }

    private fun getIntValue(value: String): Number? {
        return if (value.isEmpty()) null else value.toInt()
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_number
}

class SwitchWidget(parent: ViewGroup, private val boolUnit: BoolUnit) : BaseParamWidget<Boolean>(parent, boolUnit) {

    private val switchItem = view.findViewById<Switch>(R.id.sw_item)

    init {
        bindData()
    }

    private fun bindData() {
        switchItem.setText(getResNameId())
        switchItem.isChecked = boolUnit.viewModel?.data ?: false
        switchItem.setOnCheckedChangeListener { view, isChecked -> boolUnit.viewModel?.updateData(isChecked) }
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_switch

}

class SpinnerWidget(parent: ViewGroup, private val listUnit: ListUnit) : BaseParamWidget<ModelHelper>(parent, listUnit) {

    private val spItem = view.findViewById<Spinner>(R.id.sp_item)
    private val spAdapter = SpItemAdapter(listUnit.viewModel?.data?.itemList)

    init {
        bindData()
    }

    private fun bindData() {
        val name = view.findViewById<TextView>(R.id.tv_name)
        name.setText(getResNameId())
        spItem.adapter = spAdapter
        spItem.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val data = listUnit.viewModel?.data ?: return

                data.selectedItem = data.itemList[position]
                listUnit.viewModel?.updateData(data)
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_spinner
}

class SpItemAdapter(list: List<KeyValue>?) : BaseAdapter() {

    private val itemList: List<KeyValue> = list ?: listOf()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.vh_sp_item)

        val tv = view.findViewById<TextView>(R.id.tv_sp_item)
        tv.text = extractData(itemList[position])

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: parent.inflate(R.layout.vh_sp_dropdown_item)

        val tv = view.findViewById<TextView>(R.id.tv_sp_item)
        tv.text = extractData(itemList[position])

        return view
    }

    private fun extractData(item: KeyValue): String? = item.key

    override fun getItem(position: Int): String = stringOf(itemList[position])

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = itemList.size
}