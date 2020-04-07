package com.tangem.tangemtest._arch.widget

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.widget.abstraction.ViewWidget
import com.tangem.tangemtest._arch.widget.impl.LinearBlockWidget
import com.tangem.tangemtest._arch.widget.impl.StubWidget

/**
[REDACTED_AUTHOR]
 */
class WidgetBuilder(
        private val itemBuilder: ItemWidgetBuilder
) {

    fun build(item: Item, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is Block -> buildBlock(item, parent)
            is BaseItem<*> -> itemBuilder.build(item, parent)
            else -> StubWidget(parent)
        }
    }

    private fun buildBlock(block: Block, parent: ViewGroup): ViewWidget {
        return when (block) {
            is ListItemBlock -> {
                val linearBlock = LinearBlockWidget(parent, block)
                block.getItems().forEach { build(it, linearBlock.view as ViewGroup) }
                linearBlock
            }
            else -> StubWidget(parent)
        }
    }
}