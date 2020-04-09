package com.tangem.tangemtest._arch.widget

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ItemGroup
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget
import com.tangem.tangemtest._arch.widget.impl.LinearGroupWidget
import com.tangem.tangemtest._arch.widget.impl.StubWidget

/**
[REDACTED_AUTHOR]
 */
class WidgetBuilder(
        private val itemBuilder: ItemWidgetBuilder
) {

    fun build(item: Item, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is ItemGroup -> buildBlock(item, parent)
            is BaseItem -> itemBuilder.build(item, parent)
            else -> StubWidget(item.id, parent)
        }
    }

    private fun buildBlock(itemGroup: ItemGroup, parent: ViewGroup): ViewWidget {
        return when (itemGroup) {
            is ItemGroup -> {
                val linearBlock = LinearGroupWidget(parent, itemGroup)
                itemGroup.getItems().forEach { build(it, linearBlock.view as ViewGroup) }
                linearBlock
            }
            else -> StubWidget(itemGroup.id, parent)
        }
    }
}