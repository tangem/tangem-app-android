package com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.impl.item

import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.abstraction.BaseWidget
import com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.abstraction.getResDescription
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
        getResDescription()?.let { tvDescription?.setText(it) }
        dataItem.viewModel.viewState.onDescriptionVisibilityChanged = { changeDescriptionVisibility(it) }
    }

    protected fun changeDescriptionVisibility(state: Int) {
        TransitionManager.beginDelayedTransition(view.parent as ViewGroup, AutoTransition())
        descriptionContainer.visibility = state
    }
}











