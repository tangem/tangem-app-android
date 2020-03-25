package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.abstraction.BaseBlockWidget

class StubWidget(parent: ViewGroup) : BaseBlockWidget(parent) {
    override fun getLayoutId(): Int = R.layout.w_empty
    override var viewModel: List<ItemViewModel<*>> = listOf()
}