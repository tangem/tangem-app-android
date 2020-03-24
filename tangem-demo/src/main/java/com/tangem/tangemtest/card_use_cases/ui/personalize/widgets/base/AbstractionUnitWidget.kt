package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.DataUnit
import com.tangem.tangemtest.card_use_cases.ui.personalize.PersonalizeResources
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
interface ViewWidget : LayoutHolder {
    val view: View
}

interface UnitWidget<D> : ViewWidget {
    val unit: DataUnit<D>
}

abstract class BaseViewWidget(parent: ViewGroup) : ViewWidget {
    override val view: View = inflateView(parent)

    private fun inflateView(parent: ViewGroup): View {
        var id = getLayoutId()
        if (id == -1) id = R.layout.w_empty
        val inflatedView = LayoutInflater.from(parent.context).inflate(id, parent, false)
        parent.addView(inflatedView)
        return inflatedView
    }
}

fun UnitWidget<*>.getResNameId(): Int = PersonalizeResources.get(unit.id).resName

fun UnitWidget<*>.getResDescription(): Int? = PersonalizeResources.get(unit.id).resDescription