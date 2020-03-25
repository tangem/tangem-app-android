package com.tangem.tangemtest.card_use_cases.ui.personalize.converter

import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest.card_use_cases.ui.personalize.dto.TestJsonDto

/**
[REDACTED_AUTHOR]
 */
interface EncodeDecode<A, B> {
    fun decode(from: A): B
    fun encode(from: B): A
}

class JsonBlockEnDe(
        private val jsonToBlock: JsonToBlockConverter,
        private val blockToJson: BlockToJsonConverter
) : EncodeDecode<TestJsonDto, List<Block>> {

    override fun encode(from: List<Block>): TestJsonDto = blockToJson.convert(from)

    override fun decode(from: TestJsonDto): List<Block> = jsonToBlock.convert(from)
}