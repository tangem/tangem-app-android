package com.tangem.tangemtest._arch.widget

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget

/**
[REDACTED_AUTHOR]
 */
interface ItemWidgetBuilder {
    fun build(item: BaseItem<*>, parent: ViewGroup): ViewWidget?
}