package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets

import android.view.View
import android.view.ViewGroup
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.Block
import com.tangem.tangemtest._arch.structure.base.DataUnit
import com.tangem.tangemtest._arch.structure.base.Unit
import com.tangem.tangemtest._arch.structure.impl.*
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate

/**
[REDACTED_AUTHOR]
 */
interface BlockWidget : ViewWidget

class EmptyWidget(parent: ViewGroup) : BlockWidget {
    override val view: View = parent.inflate(getLayoutId(), parent, false)
    override fun getLayoutId(): Int = R.layout.w_empty
}

abstract class BaseBlockWidget<D>(parent: ViewGroup) : BaseViewWidget(parent), BlockWidget

class LinearBlockWidget(
        private val linearBlock: LinearBlock,
        parent: ViewGroup
) : BaseBlockWidget<List<UnitWidget<*>>>(parent) {
    override fun getLayoutId(): Int = R.layout.w_personilize_block
}

class WidgetBuilder {

    fun build(unit: Unit, parent: ViewGroup): ViewWidget? {
        return when (unit) {
            is Block -> buildBlock(unit, parent)
            is DataUnit<*> -> buildUnitItem(unit, parent)
            else -> EmptyWidget(parent)
        }
    }

    private fun buildBlock(block: Block, parent: ViewGroup): BlockWidget {
        return when (block) {
            is LinearBlock -> {
                val linearBlock = LinearBlockWidget(block, parent)
                block.getItems().forEach { build(it, linearBlock.view as ViewGroup) }
                linearBlock
            }
            else -> EmptyWidget(parent)
        }
    }

    private fun buildUnitItem(unit: DataUnit<*>, parent: ViewGroup): UnitWidget<*>? {
        return when (unit) {
            is TextUnit -> TextWidget(parent, unit)
            is EditTextUnit -> EditTextWidget(parent, unit)
            is NumberUnit -> NumberWidget(parent, unit)
            is BoolUnit -> SwitchWidget(parent, unit)
            is ListUnit -> SpinnerWidget(parent, unit)
            else -> null
        }
    }
}