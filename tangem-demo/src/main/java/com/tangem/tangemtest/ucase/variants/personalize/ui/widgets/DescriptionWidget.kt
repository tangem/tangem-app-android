package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets

import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ViewState

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
        TransitionManager.beginDelayedTransition(view.parent as ViewGroup, AutoTransition())
        descriptionContainer.visibility = state
    }
}