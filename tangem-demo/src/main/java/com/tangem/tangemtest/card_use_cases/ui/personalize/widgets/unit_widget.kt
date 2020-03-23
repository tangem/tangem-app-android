package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.DataUnit
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

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

interface DescriptionWidget {
    fun toggleDescriptionVisibility()
}

interface ParamWidget<D> : UnitWidget<D>, DescriptionWidget

abstract class BaseParamWidget<D>(
        parent: ViewGroup,
        override val unit: DataUnit<D>
) : BaseViewWidget(parent), ParamWidget<D> {

    override fun toggleDescriptionVisibility() {
        val tvDescription = view.findViewById<TextView>(R.id.tv_description) ?: return

        tvDescription.text = getDescription()
        TransitionManager.beginDelayedTransition(tvDescription.parent as ViewGroup, AutoTransition())
        tvDescription.visibility = unit.viewModel?.viewState?.descriptionVisibility ?: View.GONE
    }
}

fun UnitWidget<*>.getResNameId(): Int {
    val id = unit.payload["resName"] ?: R.string.unknown
    return id as Int
}

fun UnitWidget<*>.getDescription(): String {
    return stringOf(unit.payload["description"], isNull = "not found")
}