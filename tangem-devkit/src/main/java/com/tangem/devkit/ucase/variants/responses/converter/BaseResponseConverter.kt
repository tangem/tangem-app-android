package com.tangem.devkit.ucase.variants.responses.converter

import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit._arch.structure.abstraction.*
import com.tangem.devkit.ucase.domain.responses.ResponseFieldConverter
import com.tangem.devkit.ucase.variants.responses.item.TextHeaderItem
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
[REDACTED_AUTHOR]
 */
abstract class BaseResponseConverter<M> : ModelToItems<M> {
    protected val fieldConverter = ResponseFieldConverter()

    protected open fun createGroup(id: Id, colorId: Int? = null, addHeaderItem: Boolean = true): ItemGroup {
        val group = if (colorId == null) SimpleItemGroup(id)
        else SimpleItemGroup(id, BaseItemViewModel(viewState = ViewState(bgColor = colorId)))

        if (addHeaderItem) group.addItem(TextHeaderItem(id, ""))
        return group
    }

    protected open fun valueToString(value: Any?): String? {
        if (value == null) return null
        return stringOf(value)
    }
}