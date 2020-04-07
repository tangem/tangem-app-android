package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest._arch.widget.ItemWidgetBuilder
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget

/**
* [REDACTED_AUTHOR]
 */
class PersonalizeItemBuilder : ItemWidgetBuilder {
    override fun build(item: BaseItem<*>, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is TextItem -> BlockHeadWidget(parent, item)
            is EditTextItem -> EditTextWidget(parent, item)
            is NumberItem -> NumberWidget(parent, item)
            is BoolItem -> SwitchWidget(parent, item)
            is ListItem -> SpinnerWidget(parent, item)
            else -> null
        }
    }
}