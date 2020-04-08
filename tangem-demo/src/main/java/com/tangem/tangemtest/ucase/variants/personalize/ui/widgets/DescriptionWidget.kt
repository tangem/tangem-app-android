package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.widget.abstraction.BaseWidget
import com.tangem.tangemtest._arch.widget.abstraction.getResDescription
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
abstract class DescriptionWidget<D>(
        parent: ViewGroup,
        data: BaseItem<D>
) : BaseWidget<D>(parent, data) {

    private val descriptionContainer: ViewGroup by lazy { view.findViewById<ViewGroup>(R.id.container_description) }
    private val tvDescription: TextView? by lazy { descriptionContainer.findViewById<TextView>(R.id.tv_description) }

    init {
        Log.d(this, "init id: ${dataItem.id}")
        initDescriptionWidget()
    }

    private fun initDescriptionWidget() {
        dataItem.viewModel.viewState.onDescriptionVisibilityChanged = { changeDescriptionVisibility(it) }
    }

    protected open fun changeDescriptionVisibility(state: Int) {
        val tv = tvDescription ?: return
        val descriptionId = getResDescription() ?: return
        val description = tv.context.getString(descriptionId)
        if (description.isEmpty()) return

        tv.text = description
        TransitionManager.beginDelayedTransition(view.parent as ViewGroup, AutoTransition())
        descriptionContainer.visibility = state
    }
}