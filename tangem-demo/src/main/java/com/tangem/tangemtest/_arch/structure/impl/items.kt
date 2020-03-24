package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.base.DataUnit
import com.tangem.tangemtest._arch.structure.base.Id


class TextUnit(override val id: Id, value: String? = null) : DataUnit<String>(StringViewModel(value))
class EditTextUnit(override val id: Id, value: String? = null) : DataUnit<String>(StringViewModel(value))
class NumberUnit(override val id: Id, value: Number? = null) : DataUnit<Number>(NumberViewModel(value))
class BoolUnit(override val id: Id, value: Boolean? = null) : DataUnit<Boolean>(BoolViewModel(value))

class ListUnit(override val id: Id, value: List<KeyValue>, selectedValue: Any)
    : DataUnit<ModelHelper>(ListViewModel(ModelHelper(selectedValue, value))
)

fun DataUnit<*>.resName(resId: Int): DataUnit<*> {
    payload["resName"] = resId
    return this
}