package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets

import android.view.ViewGroup
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.abstraction.ViewWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl.StubWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl.block.LinearBlockWidget
import com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.impl.item.*

class WidgetBuilder {

    fun build(item: Item, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is Block -> buildBlock(item, parent)
            is BaseItem<*> -> buildUnitItem(item, parent)
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

    private fun buildUnitItem(item: BaseItem<*>, parent: ViewGroup): ViewWidget? {
        return when (item) {
            is TextItem -> TextWidget(parent, item)
            is EditTextItem -> EditTextWidget(parent, item)
            is NumberItem -> NumberWidget(parent, item)
            is BoolItem -> SwitchWidget(parent, item)
            is ListItem -> SpinnerWidget(parent, item)
            else -> null
        }
    }
}