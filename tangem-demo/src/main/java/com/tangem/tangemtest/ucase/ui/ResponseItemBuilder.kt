package com.tangem.tangemtest.ucase.ui

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.impl.TextItem
import com.tangem.tangemtest._arch.widget.ItemWidgetBuilder
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget
import com.tangem.tangemtest.ucase.ui.widgets.ResponseTextWidget

/**
[REDACTED_AUTHOR]
 */
class ResponseItemBuilder : ItemWidgetBuilder {
    override fun build(item: BaseItem<*>, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is TextItem -> ResponseTextWidget(parent, item)
            else -> null
        }
    }
}