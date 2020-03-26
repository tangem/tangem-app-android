package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

/**
[REDACTED_AUTHOR]
 */
class BlockToJsonConverter(
        private val valueMapper: ValueMapper,
        private val config: PersonalizeConfig
) : Converter<List<Block>, PersonalizeConfig> {

    override fun convert(from: List<Block>): PersonalizeConfig {
        return valueMapper.mapOnObject(from, config)
    }
}