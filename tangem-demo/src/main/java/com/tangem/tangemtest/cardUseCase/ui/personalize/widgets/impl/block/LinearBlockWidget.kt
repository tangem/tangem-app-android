package com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.impl.block

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest.cardUseCase.ui.personalize.widgets.abstraction.BaseBlockWidget

/**
[REDACTED_AUTHOR]
 */
class LinearBlockWidget(
        parent: ViewGroup,
        private val children: ListItemBlock
) : BaseBlockWidget(parent) {
    override fun getLayoutId(): Int = R.layout.w_personilize_block

    override var viewModel: List<ItemViewModel<*>> = listOf()
        get() = children.itemList.map { it as BaseItem<*> }.map { it.viewModel }
}