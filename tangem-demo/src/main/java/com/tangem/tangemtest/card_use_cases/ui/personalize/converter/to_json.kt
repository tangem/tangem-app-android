package com.tangem.tangemtest.card_use_cases.ui.personalize.converter

import com.tangem.tangemtest._arch.structure.base.Block
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

class BlockToJsonConverter : Converter<List<Block>, TestJsonDto> {
    override fun convert(from: List<Block>): TestJsonDto = TestJsonDto()
}