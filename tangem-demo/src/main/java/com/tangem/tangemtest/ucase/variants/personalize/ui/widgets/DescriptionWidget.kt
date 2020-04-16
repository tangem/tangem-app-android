package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.TextView
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ViewState
import com.tangem.tangemtest.extensions.view.beginDelayedTransition

/**
[REDACTED_AUTHOR]
 */
abstract class DescriptionWidget(
        parent: ViewGroup,
        item: Item
) : BaseAppWidget(parent, item) {

    private val descriptionContainer: ViewGroup by lazy { view.findViewById<ViewGroup>(R.id.container_description) }
    private val tvDescription: TextView? by lazy { descriptionContainer.findViewById<TextView>(R.id.tv_description) }

    override fun subscribeToViewStateChanges(viewState: ViewState) {
        super.subscribeToViewStateChanges(viewState)
        viewState.descriptionVisibility.onValueChanged = { changeDescriptionVisibility(it) }
    }

    protected open fun changeDescriptionVisibility(state: Int) {
        val tv = tvDescription ?: return
        val descriptionId = getResDescriptionId() ?: return
        val description = tv.context.getString(descriptionId)
        if (description.isEmpty()) return

        tv.text = description
        (view.parent as ViewGroup).beginDelayedTransition()
        descriptionContainer.visibility = state
    }
}