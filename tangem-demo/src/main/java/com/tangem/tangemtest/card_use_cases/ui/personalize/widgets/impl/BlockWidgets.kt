package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.impl.LinearBlock
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.BaseBlockWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base.UnitWidget

/**
[REDACTED_AUTHOR]
 */
class LinearBlockWidget(
        private val linearBlock: LinearBlock,
        parent: ViewGroup
) : BaseBlockWidget<List<UnitWidget<*>>>(parent) {
    override fun getLayoutId(): Int = R.layout.w_personilize_block
}