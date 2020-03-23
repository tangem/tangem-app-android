package com.tangem.tangemtest.card_use_cases.ui.personalize.personalize_converter.json_test

import com.tangem.tangemtest._arch.structure.base.Block

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
) : EncodeDecode<TestJsonDto, Block> {

    override fun encode(from: Block): TestJsonDto = blockToJson.convert(from)

    override fun decode(from: TestJsonDto): Block = jsonToBlock.convert(from)
}