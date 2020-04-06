package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem

/**
[REDACTED_AUTHOR]
 */
class AnyItem(override val id: Id, value: Any? = null) : BaseItem<Any>(AnyViewModel(value))
class TextItem(override val id: Id, value: String? = null) : BaseItem<String>(StringViewModel(value))
class EditTextItem(override val id: Id, value: String? = null) : BaseItem<String>(StringViewModel(value))
class NumberItem(override val id: Id, value: Number? = null) : BaseItem<Number>(NumberViewModel(value))
class BoolItem(override val id: Id, value: Boolean? = null) : BaseItem<Boolean>(BoolViewModel(value))

class ListItem(override val id: Id, value: List<KeyValue>, selectedValue: Any?)
    : BaseItem<ListValueWrapper>(ListViewModel(ListValueWrapper(selectedValue, value))
)

inline fun<reified T> BaseItem<T>.getData(): T? = viewModel.data