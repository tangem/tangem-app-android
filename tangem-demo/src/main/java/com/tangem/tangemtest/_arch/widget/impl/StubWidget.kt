package com.tangem.tangemtest._arch.widget.impl

import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.BaseItemViewModel
import com.tangem.tangemtest._arch.widget.abstraction.BaseViewWidget

/**
[REDACTED_AUTHOR]
 */
class StubWidget(id: Id, parent: ViewGroup) : BaseViewWidget(parent, BaseItem(id, BaseItemViewModel())) {
    override fun getLayoutId(): Int = R.layout.w_empty
}