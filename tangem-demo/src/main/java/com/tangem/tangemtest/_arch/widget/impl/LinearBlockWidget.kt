package com.tangem.tangemtest._arch.widget.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.abstraction.iterate
import com.tangem.tangemtest._arch.widget.abstraction.BaseBlockWidget

/**
[REDACTED_AUTHOR]
 */
class LinearBlockWidget(
        parent: ViewGroup,
        private val children: ListItemBlock
) : BaseBlockWidget(parent) {
    override fun getLayoutId(): Int = R.layout.w_personilize_block

    override var viewModel: List<ItemViewModel<*>> = listOf()
        get() {
            val vmList = mutableListOf<ItemViewModel<*>>()
            children.itemList.iterate { item ->
                (item as? ItemViewModel<*>)?.let { vmList.add(it) }
            }
            return vmList.toList()
        }
}