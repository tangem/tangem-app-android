package com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest.ucase.variants.personalize.ui.widgets.abstraction.BaseBlockWidget

class StubWidget(parent: ViewGroup) : BaseBlockWidget(parent) {
    override fun getLayoutId(): Int = R.layout.w_empty
    override var viewModel: List<ItemViewModel<*>> = listOf()
}