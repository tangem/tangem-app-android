package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.abstraction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.DataHolder
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
interface ViewWidget : LayoutHolder {
    val view: View
}

interface DataWidget<D> : ViewWidget {
    var dataItem: BaseItem<D>
}

interface BlockViewWidget : ViewWidget, DataHolder<List<ItemViewModel<*>>>

abstract class BaseWidget<D>(
        parent: ViewGroup,
        override var dataItem: BaseItem<D>
) : ViewWidget, DataWidget<D> {

    override val view: View = inflate(getLayoutId(), parent)

    init {
        if (dataItem.viewModel.viewState.isHiddenField) {
            view.visibility = View.GONE
        }
    }
}

abstract class BaseBlockWidget(parent: ViewGroup) : BlockViewWidget {
    override val view: View = inflate(getLayoutId(), parent)
}

fun DataWidget<*>.getResNameId(): Int = MainResourceHolder.safeGet<Resources>(dataItem.id).resName

fun DataWidget<*>.getResDescription(): Int? = MainResourceHolder.safeGet<Resources>(dataItem.id).resDescription

internal fun inflate(id: Int, parent: ViewGroup): View {
    val layoutId = if (id <= 0) R.layout.w_empty else id
    val inflatedView = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    parent.addView(inflatedView)
    return inflatedView
}