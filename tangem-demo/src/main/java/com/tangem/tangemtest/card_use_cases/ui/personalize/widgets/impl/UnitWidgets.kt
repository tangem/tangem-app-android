package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.DataUnit
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.BaseViewWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.UnitWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.getResDescription
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.getResNameId
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
abstract class BaseUnitWidget<D>(
        parent: ViewGroup,
        override val unit: DataUnit<D>
) : BaseViewWidget(parent), UnitWidget<D> {

    private val descriptionContainer: ViewGroup by lazy { view.findViewById<ViewGroup>(R.id.container_description) }
    private val tvDescription: TextView? by lazy { descriptionContainer.findViewById<TextView>(R.id.tv_description) }

    init {
        Log.d(this, "init id: ${unit.id}")
        getResDescription()?.let { tvDescription?.setText(it) }
        unit.viewModel?.viewState?.onDescriptionVisibilityChanged = { changeDescriptionVisibility(it) }
    }

    private fun changeDescriptionVisibility(state: Int) {
        TransitionManager.beginDelayedTransition(view.parent as ViewGroup, AutoTransition())
        descriptionContainer.visibility = state
    }
}

class TextWidget(parent: ViewGroup, private val textUnit: TextUnit) : BaseUnitWidget<String>(parent, textUnit) {

    private val tvName = view.findViewById<TextView>(R.id.tv_name)

    init {
        tvName.setText(getResNameId())
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_text
}

class EditTextWidget(parent: ViewGroup, private val editTextUnit: EditTextUnit) : BaseUnitWidget<String>(parent, editTextUnit) {

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    private val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            editTextUnit.viewModel?.updateDataByView(stringOf(s))
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    init {
        tilItem.hint = tilItem.context.getString(getResNameId())
        etItem.setText(editTextUnit.viewModel?.data)
        etItem.addTextChangedListener(watcher)
        editTextUnit.viewModel?.onDataUpdated = {
            etItem.removeTextChangedListener(watcher)
            etItem.setText(it)
            etItem.addTextChangedListener(watcher)
        }
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_edit_text
}

class NumberWidget(parent: ViewGroup, private val numberUnit: NumberUnit) : BaseUnitWidget<Number>(parent, numberUnit) {

    private val tilItem = view.findViewById<TextInputLayout>(R.id.til_item)
    private val etItem = view.findViewById<TextInputEditText>(R.id.et_item)

    private val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            numberUnit.viewModel?.updateDataByView(getIntValue(stringOf(s)))
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    init {
        tilItem.hint = tilItem.context.getString(getResNameId())
        etItem.setText(stringOf(numberUnit.viewModel?.data))
        etItem.addTextChangedListener(watcher)
        numberUnit.viewModel?.onDataUpdated = {
            etItem.removeTextChangedListener(watcher)
            etItem.setText(stringOf(it))
            etItem.addTextChangedListener(watcher)
        }
    }

    private fun getIntValue(value: String): Number? {
        return if (value.isEmpty()) null else value.toInt()
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_number
}

class SwitchWidget(parent: ViewGroup, private val boolUnit: BoolUnit) : BaseUnitWidget<Boolean>(parent, boolUnit) {

    private val switchItem = view.findViewById<SwitchCompat>(R.id.sw_item)

    private val changeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        boolUnit.viewModel?.updateDataByView(isChecked)
    }

    init {
        switchItem.setText(getResNameId())
        switchItem.isChecked = boolUnit.viewModel?.data ?: false
        switchItem.setOnCheckedChangeListener(changeListener)
        boolUnit.viewModel?.onDataUpdated = {
            switchItem.setOnCheckedChangeListener(null)
            switchItem.isChecked = it ?: false
            switchItem.setOnCheckedChangeListener(changeListener)
        }
    }

    override fun getLayoutId(): Int = R.layout.w_personalize_item_switch
}

class SpinnerWidget(parent: ViewGroup, private val listUnit: ListUnit) : BaseUnitWidget<ListValueWrapper>(parent, listUnit) {

    private val spItem = view.findViewById<Spinner>(R.id.sp_item)
    private val spAdapter = SpItemAdapter(listUnit.viewModel?.data?.itemList)

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val data = listUnit.viewModel?.data ?: return

            data.selectedItem = data.itemList[position]
            listUnit.viewModel?.updateDataByView(data)
        }
    }

    init {
        val name = view.findViewById<TextView>(R.id.tv_name)
        name.setText(getResNameId())
        spItem.adapter = spAdapter
        spItem.onItemSelectedListener = onItemSelectedListener
        listUnit.viewModel?.onDataUpdated = {
            it?.apply {
                spItem.onItemSelectedListener = null
                this.itemList.firstOrNull { item -> item.value == selectedItem }?.let {
                    val position = itemList.indexOf(it)
                    spItem.setSelection(position)
                }
                spItem.onItemSelectedListener = onItemSelectedListener
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