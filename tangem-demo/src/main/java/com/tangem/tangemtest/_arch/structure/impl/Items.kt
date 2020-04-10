package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.*

/**
[REDACTED_AUTHOR]
 */

open class TypedItem<D>(id: Id, viewModel: ItemViewModel) : BaseItem(id, viewModel) {
    open fun getTypedData(): D? = viewModel.data as? D
}

class TextItem(id: Id, viewModel: ItemViewModel) : TypedItem<String>(id, viewModel) {
    constructor(id: Id, value: String? = null, viewState: ViewState = ViewState())
            : this(id, BaseItemViewModel(value, viewState))
}

class NumberItem(id: Id, viewModel: ItemViewModel) : TypedItem<Number>(id, viewModel) {
    constructor(id: Id, value: Number? = null, viewState: ViewState = ViewState())
            : this(id, BaseItemViewModel(value, viewState))
}

class BoolItem(id: Id, viewModel: ItemViewModel) : TypedItem<Boolean>(id, viewModel) {
    constructor(id: Id, value: Boolean? = null, viewState: ViewState = ViewState())
            : this(id, BaseItemViewModel(value, viewState))
}


class EditTextItem(id: Id, viewModel: ItemViewModel) : TypedItem<String>(id, viewModel) {
    constructor(id: Id, value: String? = null, viewState: ViewState = ViewState())
            : this(id, BaseItemViewModel(value, viewState))
}


class SpinnerItem(id: Id, viewModel: ListViewModel) : TypedItem<ListViewModel>(id, viewModel) {
    constructor(id: Id, list: List<KeyValue>, selectedValue: Any?, viewState: ViewState = ViewState())
            : this(id, ListViewModel(list, selectedValue, viewState))
}