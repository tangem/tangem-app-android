package com.tangem.tangemtest._arch.widget.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.abstraction.ItemGroup
import com.tangem.tangemtest._arch.widget.abstraction.BaseViewWidget

/**
[REDACTED_AUTHOR]
 */
class LinearGroupWidget(
        parent: ViewGroup,
        itemGroup: ItemGroup
) : BaseViewWidget(parent, itemGroup) {

    override fun getLayoutId(): Int = R.layout.w_personilize_block

}