package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Spinner
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.KeyValue
import com.tangem.tangemtest._arch.structure.abstraction.ListViewModel
import com.tangem.tangemtest._arch.structure.impl.SpinnerItem
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
class SpinnerWidget(
        parent: ViewGroup,
        private val typedItem: SpinnerItem
) : DescriptionWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_personalize_item_spinner

    private val data: ListViewModel = typedItem.getTypedData()!!

    private val spItem = view.findViewById<Spinner>(R.id.sp_item)
    private val spAdapter = SpItemAdapter(data.itemList)

    private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            data.selectedItem = data.itemList[position].value
            typedItem.viewModel.updateDataByView(data)
        }
    }

    init {
        val name = view.findViewById<TextView>(R.id.tv_name)
        name.text = getName()
        spItem.adapter = spAdapter
        spAdapter.getItemPosition(stringOf(data.selectedItem))?.let {
            spItem.setSelection(it)
        }
        spItem.onItemSelectedListener = onItemSelectedListener
        typedItem.viewModel.onDataUpdated = {
            val selectedItem = it as? String
            spItem.onItemSelectedListener = null
            data.itemList.firstOrNull { item -> item.value == selectedItem }?.let { keyValue ->
                val position = data.itemList.indexOf(keyValue)
                spItem.setSelection(position)
            }
            spItem.onItemSelectedListener = onItemSelectedListener
        }
    }
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

    fun getItemPosition(item: String): Int? {
        val kv = itemList.firstOrNull { it.value == item } ?: return null
        return itemList.indexOf(kv)
    }
}