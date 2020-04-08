package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ItemsToModel
import com.tangem.tangemtest._arch.structure.abstraction.ModelConverter
import com.tangem.tangemtest._arch.structure.abstraction.ModelToItems
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.ItemsToPersonalizationConfig
import com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo.PersonalizationConfigToItems
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig

/**
[REDACTED_AUTHOR]
 */
class PersonalizationConfigConverter : ModelConverter<PersonalizationConfig> {

    private val toModel: ItemsToModel<PersonalizationConfig> = ItemsToPersonalizationConfig()
    private val toItems: ModelToItems<PersonalizationConfig> = PersonalizationConfigToItems()

    override fun convert(from: List<Item>, default: PersonalizationConfig): PersonalizationConfig {
        return toModel.convert(from, default)
    }

    override fun convert(from: PersonalizationConfig): List<Item> {
        return toItems.convert(from)
    }
}