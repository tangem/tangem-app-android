package com.tangem.tangemtest.ucase.variants.responses.ui.widget

import android.view.ViewGroup
import com.google.android.material.checkbox.MaterialCheckBox
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.BoolItem
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.DescriptionWidget
import ru.dev.gbixahue.eu4d.lib.android._android.views.colorFrom

/**
[REDACTED_AUTHOR]
 */
class CheckBoxWidget(
        parent: ViewGroup,
        private val typedItem: BoolItem
) : DescriptionWidget(parent, typedItem) {

    override fun getLayoutId(): Int = R.layout.w_response_item_checkbox

    private val switchItem = view.findViewById<MaterialCheckBox>(R.id.sw_item)

    init {
        switchItem.text = getName()
        switchItem.isChecked = typedItem.getData() ?: false
        switchItem.isEnabled = false
        switchItem.setTextColor(switchItem.colorFrom(R.color.action_name))
    }
}