package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.BaseItemViewModel
import com.tangem.tangemtest._arch.structure.abstraction.KeyValue
import com.tangem.tangemtest._arch.structure.abstraction.ListViewModel

/**
[REDACTED_AUTHOR]
 */

open class TypedItem<D>(id: Id, value: D? = null) : BaseItem(id, BaseItemViewModel(value)) {
    open fun getTypedData(): D? = viewModel.data as? D
}

class TextItem(id: Id, value: String? = null) : TypedItem<String>(id, value)
class NumberItem(id: Id, value: Number? = null) : TypedItem<Number>(id, value)
class BoolItem(id: Id, value: Boolean? = null) : TypedItem<Boolean>(id, value)

class EditTextItem(id: Id, value: String? = null) : TypedItem<String>(id, value)

class SpinnerItem(id: Id, value: List<KeyValue>, selectedValue: Any?)
    : TypedItem<ListViewModel>(id, ListViewModel(selectedValue, value)
)