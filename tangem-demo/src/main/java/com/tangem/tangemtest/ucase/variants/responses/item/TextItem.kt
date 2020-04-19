package com.tangem.tangemtest.ucase.variants.responses.item

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItemViewModel
import com.tangem.tangemtest._arch.structure.abstraction.ItemViewModel
import com.tangem.tangemtest._arch.structure.abstraction.ViewState
import com.tangem.tangemtest._arch.structure.impl.TypedItem

open class TextHeaderItem(id: Id, viewModel: ItemViewModel) : TypedItem<String>(id, viewModel) {
    constructor(id: Id, value: String? = null, viewState: ViewState = ViewState())
            : this(id, BaseItemViewModel(value, viewState))
}