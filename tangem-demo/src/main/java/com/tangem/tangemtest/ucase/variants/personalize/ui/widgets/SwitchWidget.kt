package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.SwitchCompat
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.BoolItem

/**
[REDACTED_AUTHOR]
 */
class SwitchWidget(
        parent: ViewGroup,
        private val typedItem: BoolItem
) : DescriptionWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_personalize_item_switch

    private val switchItem = view.findViewById<SwitchCompat>(R.id.sw_item)

    private val changeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        typedItem.viewModel.updateDataByView(isChecked)
    }

    init {
        switchItem.text = getName()
        switchItem.isChecked = typedItem.getTypedData() ?: false
        switchItem.setOnCheckedChangeListener(changeListener)
        typedItem.viewModel.onDataUpdated = {
            switchItem.setOnCheckedChangeListener(null)
            switchItem.isChecked = it as? Boolean ?: false
            switchItem.setOnCheckedChangeListener(changeListener)
        }
    }
}